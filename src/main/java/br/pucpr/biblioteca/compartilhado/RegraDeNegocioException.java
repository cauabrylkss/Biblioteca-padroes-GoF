package br.pucpr.biblioteca.compartilhado;

/**
 * Violação de uma regra do domínio (guarda não atendida, pré-requisito ausente, operação recusada).
 *
 * <p>É a raiz das exceções de negócio do sistema (decisão C-13). A interface mostra a mensagem ao usuário; nenhuma
 * exceção desta família deve deixar o domínio em estado parcial.
 */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
