package br.pucpr.biblioteca.interfaceusuario;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Opções aceitas por {@code java -jar target/biblioteca.jar} (D-09, D-10, E-07).
 *
 * @param perfil         {@code --perfil=memoria|producao}; vazio = o do {@code application.properties} (producao)
 * @param reiniciarDados {@code --reiniciar-dados}: apaga os dados do MySQL e recria os de exemplo
 * @param roteiro        {@code --roteiro=arquivo}: respostas digitadas automaticamente antes do teclado
 * @param ajuda          {@code --ajuda}
 */
public record OpcoesDaLinhaDeComando(Optional<String> perfil, boolean reiniciarDados, Path roteiro, boolean ajuda) {

    public static final String AJUDA = """
            Uso: java -jar target/biblioteca.jar [opções]
              --perfil=producao   MySQL (padrão, D-09). Precisa de BIBLIOTECA_DB_SENHA definida.
              --perfil=memoria    Sem banco: dados só nesta execução (plano B da demonstração).
              --reiniciar-dados   Apaga os dados do MySQL e recria os dados de exemplo.
              --roteiro=ARQUIVO   Responde as perguntas com as linhas do arquivo e depois volta ao teclado
                                  (ex.: docs/demo/roteiro-demo.txt).
              --ajuda             Mostra esta ajuda.""";

    public static OpcoesDaLinhaDeComando de(String... args) {
        Optional<String> perfil = Optional.empty();
        boolean reiniciar = false;
        Path roteiro = null;
        boolean ajuda = false;
        for (String arg : args) {
            if (arg.startsWith("--perfil=")) {
                perfil = Optional.of(arg.substring("--perfil=".length()));
            } else if (arg.equals("--reiniciar-dados")) {
                reiniciar = true;
            } else if (arg.startsWith("--roteiro=")) {
                roteiro = Path.of(arg.substring("--roteiro=".length()));
            } else if (arg.equals("--ajuda") || arg.equals("-h") || arg.equals("--help")) {
                ajuda = true;
            } else {
                throw new IllegalArgumentException("Opção desconhecida: " + arg);
            }
        }
        return new OpcoesDaLinhaDeComando(perfil, reiniciar, roteiro, ajuda);
    }
}
