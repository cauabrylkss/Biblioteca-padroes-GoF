package br.pucpr.biblioteca.biblioteca.livro;

import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;

/** Um evento chegou a um estado que não o aceita. O livro não é alterado. */
public class TransicaoInvalidaException extends RegraDeNegocioException {

    private final String evento;
    private final SituacaoLivro situacao;

    public TransicaoInvalidaException(String evento, SituacaoLivro situacao) {
        super("Operação '" + evento + "' não é permitida para um livro " + situacao + ".");
        this.evento = evento;
        this.situacao = situacao;
    }

    public String getEvento() {
        return evento;
    }

    public SituacaoLivro getSituacao() {
        return situacao;
    }
}
