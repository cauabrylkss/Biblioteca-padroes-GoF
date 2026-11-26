package br.pucpr.biblioteca.interfaceusuario.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucpr.biblioteca.academico.iEstudante;
import br.pucpr.biblioteca.biblioteca.tratadores.iDevolucao;
import br.pucpr.biblioteca.biblioteca.tratadores.iEmprestimo;
import br.pucpr.biblioteca.biblioteca.tratadores.iLivro;
import br.pucpr.biblioteca.compartilhado.RelogioAjustavel;
import br.pucpr.biblioteca.config.Aplicacao;
import br.pucpr.biblioteca.config.iAdministracao;
import br.pucpr.biblioteca.interfaceusuario.OpcoesDaLinhaDeComando;
import br.pucpr.biblioteca.interfaceusuario.Tratadores;
import br.pucpr.biblioteca.rh.iAtendente;
import br.pucpr.biblioteca.rh.iPessoa;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Interface web: o fluxo da demonstração pela API HTTP (perfil de memória com os dados de exemplo, servidor real numa
 * porta livre), as respostas de recusa/erro, a página servida e a regra de arquitetura "a interface só conhece
 * tratadores".
 */
class InterfaceWebTest {

    private static final Path PAGINA = Path.of("src", "main", "resources", "web", "index.html");

    private final HttpClient cliente = HttpClient.newHttpClient();
    private InterfaceWeb interfaceWeb;
    private ServidorWeb servidor;

    @BeforeEach
    void subirServidor() {
        Aplicacao app = aplicacaoEmMemoria(RelogioAjustavel.em(LocalDate.of(2026, 11, 26)));
        interfaceWeb = new InterfaceWeb(tratadores(app), app.relogio());
        servidor = new ServidorWeb(interfaceWeb, 0);
        servidor.iniciar();
    }

    @AfterEach
    void pararServidor() {
        servidor.parar();
    }

    @Test
    void fluxoDaDemonstracaoPelaInterfaceWeb() {
        String saida = String.join("\n",
                get("iLivro/consultarLivro", "idLivro", "1").body(),
                post("iEmprestimo/solicitarReserva", "idLivro", "1", "idEstudante", "1").body(),
                get("iLivro/consultarLivro", "idLivro", "1").body(),
                post("iEmprestimo/realizarEmprestimo", "idLivro", "1", "idEstudante", "1", "idAtendente", "1").body(),
                post("iEmprestimo/renovarEmprestimo", "idLivro", "1", "idEstudante", "1").body(),
                post("iAdministracao/avancarDias", "dias", "65").body(),
                post("iDevolucao/registrarDevolucao", "idLivro", "1", "idEstudante", "1", "idAtendente", "1").body(),
                post("iDevolucao/inspecionarLivro", "idLivro", "1", "emBomEstado", "true").body(),
                post("iAdministracao/simularPagamento", "boleto", "1001").body(),
                post("iAdministracao/simularPagamento", "boleto", "1001").body(),
                get("iAdministracao/emailsEnviados").body(),
                get("iAdministracao/medicoesDaConsulta").body(),
                get("iAdministracao/indicadores").body());

        assertThat(saida)
                .contains("Situação atual: LIVRE")
                .contains("agora está RESERVADO")
                .contains("Situação atual: RESERVADO")                              // título do cache, estado atual
                .contains("Livro EMPRESTADO")
                .contains("renovado", "Livro RENOVADO")
                .contains("atraso de 5 dia(s). Livro DEVOLVIDO")
                .contains("Multa emitida: Multa 1: R$ 5,00 por 5 dia(s) de atraso (política original")
                .contains("boleto 1001 no banco A — EMITIDO")
                .contains("inspecionar: livro 1 DEVOLVIDO → LIVRE")
                .contains("pagamento confirmado: boleto PAGO, multa QUITADA")
                .contains("notificação repetida ou multa já quitada — nada alterado")
                .contains("Boleto da multa", "Devolução registrada", "Multa quitada")    // caixa de e-mails
                .contains("consultarMetadados: 2 chamada(s)")
                .contains("Devoluções: 1 | atrasadas: 1 | dias de atraso acumulados: 5")
                .doesNotContain("Recusado:", "Erro:", "\"ok\":false");
    }

    @Test
    void dadosDoComprovanteVoltamEmJsonComDinheiroExato() {
        post("iEmprestimo/solicitarReserva", "idLivro", "1", "idEstudante", "1");
        post("iEmprestimo/realizarEmprestimo", "idLivro", "1", "idEstudante", "1", "idAtendente", "1");
        post("iAdministracao/avancarDias", "dias", "35");

        HttpResponse<String> devolucao =
                post("iDevolucao/registrarDevolucao", "idLivro", "1", "idEstudante", "1", "idAtendente", "1");

        assertThat(devolucao.statusCode()).isEqualTo(200);
        assertThat(devolucao.headers().firstValue("Content-Type")).hasValueSatisfying(
                tipo -> assertThat(tipo).startsWith("application/json").contains("utf-8"));
        assertThat(devolucao.body())
                .startsWith("{\"ok\":true,")
                .contains("\"situacaoLivro\":\"DEVOLVIDO\"", "\"diasDeAtraso\":5", "\"valor\":5.00",
                        "\"politica\":\"original\"", "\"dataDevolucao\":\"31/12/2026\"");
    }

    @Test
    void recusaDoDominioDevolve409ESemAlterarOLivro() {
        HttpResponse<String> recusa =
                post("iEmprestimo/realizarEmprestimo", "idLivro", "2", "idEstudante", "1", "idAtendente", "1");

        assertThat(recusa.statusCode()).isEqualTo(409);
        assertThat(recusa.body()).startsWith("{\"ok\":false,").contains("Recusado:");
        assertThat(get("iLivro/consultarLivro", "idLivro", "2").body()).contains("Situação atual: LIVRE");
    }

    @Test
    void dadoInvalidoDevolve400EInexistenteDevolve404() {
        assertThat(get("iLivro/consultarLivro", "idLivro", "abc"))
                .satisfies(r -> assertThat(r.statusCode()).isEqualTo(400))
                .satisfies(r -> assertThat(r.body()).contains("Dados inválidos: idLivro deve ser um número inteiro"));
        assertThat(get("iEstudante/consultarEstudante", "idEstudante", "99"))
                .satisfies(r -> assertThat(r.statusCode()).isEqualTo(404))
                .satisfies(r -> assertThat(r.body()).contains("Estudante não cadastrado: 99"));
        assertThat(get("iLivro/naoExiste").statusCode()).isEqualTo(404);
        assertThat(get("iLivro/consultarAcervo").statusCode()).isEqualTo(200);
        assertThat(post("iLivro/consultarAcervo").statusCode()).isEqualTo(404);     // consulta só por GET
    }

    @Test
    void paginaEArquivosEstaticosSaoServidosSoDaPastaWeb() {
        assertThat(pagina("/")).satisfies(r -> {
            assertThat(r.statusCode()).isEqualTo(200);
            assertThat(r.headers().firstValue("Content-Type")).contains("text/html; charset=utf-8");
            assertThat(r.body()).contains("<title>Sistema de Biblioteca");
        });
        assertThat(pagina("/app.js").statusCode()).isEqualTo(200);
        assertThat(pagina("/estilo.css").statusCode()).isEqualTo(200);
        assertThat(pagina("/application.properties").statusCode()).isEqualTo(404);
        assertThat(pagina("/web/../application.properties").statusCode()).isEqualTo(404);
    }

    /** Cada formulário da página aponta para uma operação que a API conhece, com o mesmo método (GET/POST). */
    @Test
    void todaOperacaoDaPaginaExisteNaApi() throws IOException {
        String html = Files.readString(PAGINA, StandardCharsets.UTF_8);
        Matcher formulario = Pattern.compile("<form\\b([^>]*)>(.*?)</form>", Pattern.DOTALL).matcher(html);
        Pattern metodo = Pattern.compile("data-metodo=\"(GET|POST)\"");
        Pattern rota = Pattern.compile("data-rota=\"([^\"]+)\"");
        List<String> chamadasDaPagina = new ArrayList<>();
        while (formulario.find()) {
            Matcher m = metodo.matcher(formulario.group(1));
            if (!m.find()) {
                continue;                                      // formulário "Entrar" (operar como)
            }
            Matcher r = rota.matcher(formulario.group(0));
            while (r.find()) {
                chamadasDaPagina.add(m.group(1) + " /api/" + r.group(1));
            }
        }

        assertThat(chamadasDaPagina).hasSizeGreaterThan(30);
        assertThat(interfaceWeb.operacoes()).containsAll(chamadasDaPagina);
    }

    @Test
    void interfaceWebSoConheceTratadores() {
        List<Class<?>> tiposDosCampos = Arrays.stream(InterfaceWeb.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .<Class<?>>map(Field::getType)
                .toList();

        assertThat(tiposDosCampos).containsExactlyInAnyOrder(iLivro.class, iPessoa.class, iEstudante.class,
                iAtendente.class, iEmprestimo.class, iDevolucao.class, iAdministracao.class, Clock.class, Map.class);
    }

    @Test
    void jsonDeRecordsListasOptionalDatasEDinheiro() {
        record Item(long id, String nome, BigDecimal valor, LocalDate data, Optional<String> obs) {
        }
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("itens", List.of(new Item(1, "Diz \"oi\"\n", new BigDecimal("5.00"), LocalDate.of(2026, 11, 27),
                Optional.empty())));
        mapa.put("ok", true);

        assertThat(Json.escrever(mapa)).isEqualTo("{\"itens\":[{\"id\":1,\"nome\":\"Diz \\\"oi\\\"\\n\","
                + "\"valor\":5.00,\"data\":\"27/11/2026\",\"obs\":null}],\"ok\":true}");
    }

    @Test
    void opcoesDaLinhaDeComandoDaInterfaceWeb() {
        OpcoesDaLinhaDeComando padrao = OpcoesDaLinhaDeComando.de();
        assertThat(padrao.web()).isTrue();
        assertThat(padrao.porta()).isEqualTo(8080);
        assertThat(padrao.abrirNavegador()).isTrue();

        OpcoesDaLinhaDeComando outra = OpcoesDaLinhaDeComando.de("--porta=9090", "--sem-navegador");
        assertThat(outra.porta()).isEqualTo(9090);
        assertThat(outra.abrirNavegador()).isFalse();

        assertThat(OpcoesDaLinhaDeComando.de("--interface=cli").web()).isFalse();
        assertThat(OpcoesDaLinhaDeComando.de("--roteiro=docs/demo/roteiro-demo.txt").web()).isFalse();  // plano B
        assertThatThrownBy(() -> OpcoesDaLinhaDeComando.de("--interface=web", "--roteiro=x.txt"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OpcoesDaLinhaDeComando.de("--interface=tv"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OpcoesDaLinhaDeComando.de("--porta=abc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------------------------------------------------------------------------------------------

    private HttpResponse<String> get(String rota, String... campos) {
        String query = campos.length == 0 ? "" : "?" + formulario(campos);
        return enviar(HttpRequest.newBuilder(URI.create(servidor.endereco() + "api/" + rota + query)).GET());
    }

    private HttpResponse<String> post(String rota, String... campos) {
        return enviar(HttpRequest.newBuilder(URI.create(servidor.endereco() + "api/" + rota))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formulario(campos))));
    }

    private HttpResponse<String> pagina(String caminho) {
        return enviar(HttpRequest.newBuilder(URI.create("http://localhost:" + servidor.porta() + caminho)).GET());
    }

    private HttpResponse<String> enviar(HttpRequest.Builder requisicao) {
        try {
            return cliente.send(requisicao.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    /** "idLivro", "1", "idEstudante", "1" → "idLivro=1&idEstudante=1". */
    private static String formulario(String... campos) {
        return IntStream.range(0, campos.length / 2)
                .mapToObj(i -> URLEncoder.encode(campos[2 * i], StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(campos[2 * i + 1], StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private static Tratadores tratadores(Aplicacao app) {
        return new Tratadores(app.tratadorDeAcervo(), app.tratadorDePessoa(), app.tratadorDeEstudante(),
                app.tratadorDeAtendente(), app.tratadorDeEmprestimo(), app.tratadorDeDevolucao(),
                app.tratadorDeAdministracao());
    }

    private static Aplicacao aplicacaoEmMemoria(Clock relogio) {
        Properties props = Aplicacao.carregarPropriedades();
        props.setProperty("perfil", "memoria");
        Aplicacao app = Aplicacao.montar(props, relogio);
        app.prepararParaUso(false);
        return app;
    }
}
