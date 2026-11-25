package br.pucpr.biblioteca.interfaceusuario;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Leitura do teclado e escrita no console da CLI. Separada da {@link InterfaceCli} para que os testes possam
 * "digitar" por ela e para a demonstração poder seguir um <b>roteiro</b> (arquivo com as respostas, uma por linha):
 * as linhas do roteiro são respondidas e mostradas como se tivessem sido digitadas; quando ele acaba, a leitura volta
 * para o teclado. No roteiro, linhas iniciadas por {@code #} são ignoradas e {@code " #"} começa um comentário.
 */
public final class EntradaSaida {

    /** A entrada acabou (fim do arquivo, Ctrl+Z/Ctrl+D): a CLI encerra. */
    public static final class FimDaEntrada extends RuntimeException {
        FimDaEntrada() {
            super("Fim da entrada");
        }
    }

    private final Deque<String> roteiro;
    private final BufferedReader teclado;
    private final PrintStream saida;

    public EntradaSaida(List<String> respostasDoRoteiro, BufferedReader teclado, PrintStream saida) {
        this.roteiro = new ArrayDeque<>(respostasDoRoteiro);
        this.teclado = Objects.requireNonNull(teclado, "teclado");
        this.saida = Objects.requireNonNull(saida, "saida");
    }

    /**
     * Console do sistema, opcionalmente começando por um roteiro.
     *
     * @param charsetDoTeclado como decodificar o que é digitado (ver {@link ConsoleDoWindows#usarUtf8()})
     */
    public static EntradaSaida doConsole(Path arquivoDeRoteiro, Charset charsetDoTeclado) {
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in, charsetDoTeclado));
        List<String> respostas = arquivoDeRoteiro == null ? List.of() : lerRoteiro(arquivoDeRoteiro);
        return new EntradaSaida(respostas, teclado, System.out);
    }

    /** Respostas de um arquivo de roteiro, sem comentários. */
    public static List<String> lerRoteiro(Path arquivo) {
        try {
            return Files.readAllLines(arquivo, StandardCharsets.UTF_8).stream()
                    .filter(linha -> !linha.strip().startsWith("#"))
                    .map(linha -> linha.contains(" #") ? linha.substring(0, linha.indexOf(" #")) : linha)
                    .map(String::strip)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler o roteiro " + arquivo, e);
        }
    }

    public void mostrar(String texto) {
        saida.println(texto);
    }

    /** Pergunta e devolve a resposta (sem espaços nas pontas; pode ser vazia). */
    public String ler(String rotulo) {
        saida.print(rotulo + ": ");
        saida.flush();
        String linha;
        if (!roteiro.isEmpty()) {
            linha = roteiro.poll();
            saida.println(linha);                     // mostra a resposta do roteiro como se fosse digitada
        } else {
            linha = lerDoTeclado();
        }
        return linha.strip();
    }

    /** Pergunta até receber um número inteiro. */
    public long lerNumero(String rotulo) {
        while (true) {
            String resposta = ler(rotulo);
            try {
                return Long.parseLong(resposta);
            } catch (NumberFormatException e) {
                mostrar("  Digite um número inteiro.");
            }
        }
    }

    /** Como {@link #lerNumero}, mas resposta vazia devolve {@code null}. */
    public Long lerNumeroOpcional(String rotulo) {
        while (true) {
            String resposta = ler(rotulo);
            if (resposta.isEmpty()) {
                return null;
            }
            try {
                return Long.parseLong(resposta);
            } catch (NumberFormatException e) {
                mostrar("  Digite um número inteiro ou deixe em branco.");
            }
        }
    }

    /** Pergunta até receber s ou n. */
    public boolean lerSimNao(String rotulo) {
        while (true) {
            String resposta = ler(rotulo + " (s/n)").toLowerCase();
            if (resposta.equals("s") || resposta.equals("sim")) {
                return true;
            }
            if (resposta.equals("n") || resposta.equals("nao") || resposta.equals("não")) {
                return false;
            }
            mostrar("  Responda s ou n.");
        }
    }

    private String lerDoTeclado() {
        try {
            String linha = teclado.readLine();
            if (linha == null) {
                throw new FimDaEntrada();
            }
            return linha;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
