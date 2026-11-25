package br.pucpr.biblioteca.interfaceusuario;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Acentos no console do Windows. O console abre na página de código do sistema (ex.: 850), mas a CLI escreve em UTF-8
 * ({@code Main}): sem ajuste, "Padrões" aparece como "PadrÃµes" e o texto digitado chega com os acentos trocados.
 *
 * <p>{@link #usarUtf8()} muda o console para UTF-8 (página 65001, o mesmo que {@code chcp 65001}) enquanto a CLI roda
 * e devolve a página original ao sair. Fora do Windows, ou sem console, não faz nada. Não tem nada de domínio.
 */
public final class ConsoleDoWindows {

    private static final String UTF8 = "65001";
    private static final Pattern NUMERO = Pattern.compile("(\\d+)\\D*$");      // último número da resposta

    private ConsoleDoWindows() {
    }

    /**
     * Coloca o console em UTF-8 e devolve o charset com que a entrada (teclado) deve ser lida:
     * <ul>
     *   <li>console do Windows em UTF-8 (trocado agora ou já estava): UTF-8;</li>
     *   <li>Windows sem console (entrada redirecionada, Git Bash/mintty): UTF-8, que é o que eles enviam;</li>
     *   <li>console do Windows que não aceitou a troca: a página do console, detectada pelo Java;</li>
     *   <li>fora do Windows: a codificação do terminal (normalmente UTF-8).</li>
     * </ul>
     */
    public static Charset usarUtf8() {
        Charset doTerminal = System.console() != null ? System.console().charset() : StandardCharsets.UTF_8;
        if (!System.getProperty("os.name", "").startsWith("Windows")) {
            return doTerminal;
        }
        Optional<String> original = paginaAtual();
        if (original.isEmpty()) {
            return StandardCharsets.UTF_8;                      // sem console do Windows
        }
        if (!original.get().equals(UTF8)) {
            chcp(UTF8);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> chcp(original.get())));
        }
        return paginaAtual().filter(UTF8::equals).isPresent() ? StandardCharsets.UTF_8 : doTerminal;
    }

    /** Página de código do console, lida da resposta do {@code chcp} (ex.: "Página de código ativa: 850"). */
    private static Optional<String> paginaAtual() {
        String resposta = chcp(null);
        Matcher m = NUMERO.matcher(resposta.strip());
        return m.find() ? Optional.of(m.group(1)) : Optional.empty();
    }

    /**
     * Executa {@code chcp [pagina]} <b>no mesmo console</b> e devolve o que ele escreveu ("" se não deu).
     *
     * <p>A entrada é herdada de propósito: quando nenhum fluxo é herdado, o Java cria o processo filho sem console
     * (CREATE_NO_WINDOW) e o {@code chcp} mudaria um console que ninguém vê.
     */
    private static String chcp(String pagina) {
        ProcessBuilder pb = pagina == null
                ? new ProcessBuilder("cmd", "/c", "chcp")
                : new ProcessBuilder("cmd", "/c", "chcp", pagina);
        pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectErrorStream(true);
        try {
            Process processo = pb.start();
            String resposta = new String(processo.getInputStream().readAllBytes(), StandardCharsets.ISO_8859_1);
            if (!processo.waitFor(5, TimeUnit.SECONDS)) {
                processo.destroy();
                return "";
            }
            return processo.exitValue() == 0 ? resposta : "";
        } catch (IOException e) {
            return "";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        }
    }
}
