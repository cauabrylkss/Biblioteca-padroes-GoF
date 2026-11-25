package br.pucpr.biblioteca.persistencia.jdbc;

import java.util.Objects;

/**
 * Onde e como conectar no MySQL (perfil {@code producao}, D-07).
 *
 * <p>A senha nunca fica no código nem no {@code application.properties}: vem da variável de ambiente
 * {@code BIBLIOTECA_DB_SENHA}. A URL e o usuário também podem vir do ambiente ({@code BIBLIOTECA_DB_URL},
 * {@code BIBLIOTECA_DB_USUARIO}); sem eles valem a URL padrão do {@code application.properties} e o usuário
 * {@code biblioteca} criado por {@code docs/mysql/criar-banco.sql}.
 *
 * @param url     URL JDBC do banco {@code biblioteca}
 * @param usuario usuário do MySQL
 * @param senha   senha (pode ser {@code null}: o erro só aparece ao tentar conectar, com uma mensagem clara)
 */
public record ConfiguracaoDoBanco(String url, String usuario, String senha) {

    public static final String VARIAVEL_URL = "BIBLIOTECA_DB_URL";
    public static final String VARIAVEL_USUARIO = "BIBLIOTECA_DB_USUARIO";
    public static final String VARIAVEL_SENHA = "BIBLIOTECA_DB_SENHA";
    public static final String USUARIO_PADRAO = "biblioteca";

    public ConfiguracaoDoBanco {
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(usuario, "usuario");
    }

    /** Lê as variáveis de ambiente; {@code urlPadrao} vale quando {@code BIBLIOTECA_DB_URL} não está definida. */
    public static ConfiguracaoDoBanco doAmbiente(String urlPadrao) {
        return new ConfiguracaoDoBanco(
                variavel(VARIAVEL_URL, urlPadrao),
                variavel(VARIAVEL_USUARIO, USUARIO_PADRAO),
                variavel(VARIAVEL_SENHA, null));
    }

    boolean temSenha() {
        return senha != null && !senha.isBlank();
    }

    private static String variavel(String nome, String padrao) {
        String valor = System.getenv(nome);
        return valor == null || valor.isBlank() ? padrao : valor.strip();
    }

    /** Nunca mostra a senha (o {@code toString} padrão de um record mostraria). */
    @Override
    public String toString() {
        return "ConfiguracaoDoBanco[url=" + url + ", usuario=" + usuario + ", senha="
                + (temSenha() ? "***" : "(não definida)") + "]";
    }
}
