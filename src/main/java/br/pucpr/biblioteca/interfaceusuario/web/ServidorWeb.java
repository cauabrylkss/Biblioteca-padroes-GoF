package br.pucpr.biblioteca.interfaceusuario.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Servidor HTTP da interface web, com o {@link HttpServer} que já vem no JDK (sem Spring, sem Tomcat, sem
 * dependência nova). Faz só o "encanamento":
 * <ul>
 *   <li>{@code /api/...} → {@link InterfaceWeb#atender}, que chama os tratadores e devolve JSON;</li>
 *   <li>{@code /} e {@code /index.html}, {@code /app.js}, {@code /estilo.css} → arquivos da página, lidos de
 *       {@code src/main/resources/web/}.</li>
 * </ul>
 * Escuta só em {@code localhost} (a demonstração roda na mesma máquina) e atende <b>uma requisição por vez</b>
 * (executor padrão do {@code HttpServer}), como um balcão de atendimento.
 */
public final class ServidorWeb {

    private static final Pattern ARQUIVO_DA_PAGINA = Pattern.compile("[a-z0-9-]+\\.(html|js|css)");
    private static final Map<String, String> TIPOS = Map.of(
            "html", "text/html; charset=utf-8",
            "js", "text/javascript; charset=utf-8",
            "css", "text/css; charset=utf-8");

    private final HttpServer servidor;
    private final InterfaceWeb interfaceWeb;

    /**
     * Cria o servidor na porta informada (0 = qualquer porta livre, usado nos testes); só começa a atender em
     * {@link #iniciar()}.
     *
     * @throws UncheckedIOException se a porta estiver em uso
     */
    public ServidorWeb(InterfaceWeb interfaceWeb, int porta) {
        this.interfaceWeb = interfaceWeb;
        try {
            this.servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), porta), 0);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível abrir a porta " + porta
                    + " (já está em uso?). Use --porta=NNNN para escolher outra.", e);
        }
        servidor.createContext("/api/", this::atenderApi);
        servidor.createContext("/", this::servirPagina);
    }

    public void iniciar() {
        servidor.start();
    }

    public void parar() {
        servidor.stop(0);
    }

    public int porta() {
        return servidor.getAddress().getPort();
    }

    /** Endereço para abrir no navegador, ex.: {@code http://localhost:8080/}. */
    public URI endereco() {
        return URI.create("http://localhost:" + porta() + "/");
    }

    private void atenderApi(HttpExchange troca) throws IOException {
        try (troca) {
            String corpo = new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            InterfaceWeb.RespostaHttp resposta = interfaceWeb.atender(troca.getRequestMethod(),
                    troca.getRequestURI().getPath(), troca.getRequestURI().getRawQuery(), corpo);
            enviar(troca, resposta.codigo(), "application/json; charset=utf-8",
                    resposta.json().getBytes(StandardCharsets.UTF_8));
        }
    }

    private void servirPagina(HttpExchange troca) throws IOException {
        try (troca) {
            String caminho = troca.getRequestURI().getPath();
            String nome = caminho.equals("/") ? "index.html" : caminho.substring(1);
            InputStream arquivo = ARQUIVO_DA_PAGINA.matcher(nome).matches()
                    ? ServidorWeb.class.getResourceAsStream("/web/" + nome)
                    : null;                                   // nada fora de /web/ (ex.: "../application.properties")
            if (arquivo == null || !troca.getRequestMethod().equals("GET")) {
                enviar(troca, 404, TIPOS.get("html"), "Página não encontrada.".getBytes(StandardCharsets.UTF_8));
                return;
            }
            try (arquivo) {
                String extensao = nome.substring(nome.lastIndexOf('.') + 1);
                enviar(troca, 200, TIPOS.get(extensao), arquivo.readAllBytes());
            }
        }
    }

    private static void enviar(HttpExchange troca, int codigo, String tipo, byte[] corpo) throws IOException {
        troca.getResponseHeaders().set("Content-Type", tipo);
        troca.getResponseHeaders().set("Cache-Control", "no-store");      // a página sempre mostra o estado atual
        troca.sendResponseHeaders(codigo, corpo.length);
        try (OutputStream saida = troca.getResponseBody()) {
            saida.write(corpo);
        }
    }
}
