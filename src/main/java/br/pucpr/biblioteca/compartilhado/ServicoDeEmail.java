package br.pucpr.biblioteca.compartilhado;

/**
 * Envio de e-mail ao estudante (nó SMTP do diagrama de implantação). Nesta versão o envio é simulado por
 * {@link ServicoDeEmailSimulado} (decisão B-20).
 */
public interface ServicoDeEmail {

    /**
     * @throws RuntimeException se o servidor de e-mail falhar
     */
    void enviar(long idEstudante, String assunto, String texto);
}
