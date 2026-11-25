package br.pucpr.biblioteca.biblioteca.circulacao;

import br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro;
import java.time.LocalDate;

/** Comprovante de renovação (R4/R6): vencimento anterior, novo vencimento (+30 dias) e renovações já feitas. */
public record ComprovanteRenovacao(long idEmprestimo, long idLivro, long idEstudante, LocalDate vencimentoAnterior,
                                   LocalDate novoVencimento, int numRenovacoes, SituacaoLivro situacaoLivro)
        implements Comprovante {
}
