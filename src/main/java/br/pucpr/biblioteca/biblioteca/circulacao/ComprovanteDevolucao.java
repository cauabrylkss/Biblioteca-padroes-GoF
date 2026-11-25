package br.pucpr.biblioteca.biblioteca.circulacao;

import br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro;
import java.time.LocalDate;

/** Comprovante de devolução (R4). O livro fica DEVOLVIDO até a inspeção. */
public record ComprovanteDevolucao(long idDevolucao, long idEmprestimo, long idLivro, long idEstudante,
                                   long idAtendente, LocalDate vencimento, LocalDate dataDevolucao, long diasDeAtraso,
                                   SituacaoLivro situacaoLivro) implements Comprovante {
}
