package br.pucpr.biblioteca.biblioteca.circulacao;

import br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro;
import java.time.LocalDate;

/** Comprovante de empréstimo efetivado (R4). */
public record ComprovanteEmprestimo(long idEmprestimo, long idLivro, long idEstudante, long idAtendente,
                                    LocalDate dataEmprestimo, LocalDate vencimento, int numRenovacoes,
                                    SituacaoLivro situacaoLivro) implements Comprovante {
}
