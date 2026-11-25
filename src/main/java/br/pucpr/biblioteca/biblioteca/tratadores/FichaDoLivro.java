package br.pucpr.biblioteca.biblioteca.tratadores;

import br.pucpr.biblioteca.biblioteca.livro.Livro;
import br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro;

/**
 * Dados de um livro para exibição na interface. É uma cópia imutável: a interface não recebe o objeto {@code Livro}
 * e, portanto, não consegue disparar eventos de estado sem passar pelos tratadores.
 *
 * @param grupo descrição do grupo do acervo em que o livro está catalogado, ou {@code null}
 */
public record FichaDoLivro(long id, String titulo, SituacaoLivro situacao, String grupo) {

    public static FichaDoLivro de(Livro livro, String grupo) {
        return new FichaDoLivro(livro.getId(), livro.getTitulo(), livro.getSituacao(), grupo);
    }
}
