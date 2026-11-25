package br.pucpr.biblioteca.persistencia.jdbc;

/**
 * Erro de acesso ao MySQL (conexão, SQL). Embrulha a {@link java.sql.SQLException}, que é verificada, para que as
 * interfaces de repositório (iguais nas duas famílias, R8) não precisem declarar exceções de JDBC.
 */
public class FalhaNoBancoDeDados extends RuntimeException {

    public FalhaNoBancoDeDados(String mensagem) {
        super(mensagem);
    }

    public FalhaNoBancoDeDados(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
