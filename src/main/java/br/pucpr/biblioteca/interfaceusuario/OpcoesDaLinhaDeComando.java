package br.pucpr.biblioteca.interfaceusuario;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Opções aceitas por {@code java -jar target/biblioteca.jar} (D-01 revista, D-09, D-10, E-07, E-14).
 *
 * @param perfil         {@code --perfil=memoria|producao}; vazio = o do {@code application.properties} (producao)
 * @param reiniciarDados {@code --reiniciar-dados}: apaga os dados do MySQL e recria os de exemplo
 * @param web            {@code true} = interface web (padrão); {@code false} = CLI ({@code --interface=cli})
 * @param porta          {@code --porta=N}: porta da interface web (padrão 8080)
 * @param abrirNavegador {@code false} com {@code --sem-navegador}
 * @param roteiro        {@code --roteiro=arquivo}: respostas digitadas automaticamente na CLI (implica a CLI)
 * @param ajuda          {@code --ajuda}
 */
public record OpcoesDaLinhaDeComando(Optional<String> perfil, boolean reiniciarDados, boolean web, int porta,
                                     boolean abrirNavegador, Path roteiro, boolean ajuda) {

    public static final int PORTA_PADRAO = 8080;

    public static final String AJUDA = """
            Uso: java -jar target/biblioteca.jar [opções]
              --perfil=producao   MySQL (padrão, D-09). Precisa de BIBLIOTECA_DB_SENHA definida.
              --perfil=memoria    Sem banco: dados só nesta execução (plano B da demonstração).
              --reiniciar-dados   Apaga os dados do MySQL e recria os dados de exemplo.
              --interface=web     Interface web (padrão): abre http://localhost:8080 no navegador.
              --interface=cli     Interface de linha de comando (reserva).
              --porta=NNNN        Porta da interface web (padrão 8080).
              --sem-navegador     Não abre o navegador sozinho (só mostra o endereço).
              --roteiro=ARQUIVO   CLI: responde as perguntas com as linhas do arquivo e depois volta ao teclado
                                  (ex.: docs/demo/roteiro-demo.txt). Sem --interface, já escolhe a CLI.
              --ajuda             Mostra esta ajuda.""";

    public static OpcoesDaLinhaDeComando de(String... args) {
        Optional<String> perfil = Optional.empty();
        boolean reiniciar = false;
        Optional<String> interfaceEscolhida = Optional.empty();
        int porta = PORTA_PADRAO;
        boolean abrirNavegador = true;
        Path roteiro = null;
        boolean ajuda = false;
        for (String arg : args) {
            if (arg.startsWith("--perfil=")) {
                perfil = Optional.of(arg.substring("--perfil=".length()));
            } else if (arg.equals("--reiniciar-dados")) {
                reiniciar = true;
            } else if (arg.startsWith("--interface=")) {
                interfaceEscolhida = Optional.of(arg.substring("--interface=".length()));
            } else if (arg.startsWith("--porta=")) {
                porta = porta(arg.substring("--porta=".length()));
            } else if (arg.equals("--sem-navegador")) {
                abrirNavegador = false;
            } else if (arg.startsWith("--roteiro=")) {
                roteiro = Path.of(arg.substring("--roteiro=".length()));
            } else if (arg.equals("--ajuda") || arg.equals("-h") || arg.equals("--help")) {
                ajuda = true;
            } else {
                throw new IllegalArgumentException("Opção desconhecida: " + arg);
            }
        }
        boolean web = switch (interfaceEscolhida.orElse(roteiro == null ? "web" : "cli")) {
            case "web" -> true;
            case "cli" -> false;
            default -> throw new IllegalArgumentException("--interface deve ser web ou cli");
        };
        if (web && roteiro != null) {
            throw new IllegalArgumentException("--roteiro só vale para a CLI (--interface=cli)");
        }
        return new OpcoesDaLinhaDeComando(perfil, reiniciar, web, porta, abrirNavegador, roteiro, ajuda);
    }

    private static int porta(String texto) {
        try {
            int porta = Integer.parseInt(texto);
            if (porta >= 1 && porta <= 65535) {
                return porta;
            }
        } catch (NumberFormatException e) {
            // cai na mensagem abaixo
        }
        throw new IllegalArgumentException("--porta deve ser um número entre 1 e 65535: " + texto);
    }
}
