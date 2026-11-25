package br.pucpr.biblioteca.biblioteca.circulacao;

import br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro;
import java.time.LocalDate;
import java.util.List;

/**
 * Comprovante de devolução (R4). O livro fica DEVOLVIDO até a inspeção.
 *
 * @param avisos falhas de ouvintes da devolução (R5) — a devolução continua registrada mesmo se algum falhar
 */
public record ComprovanteDevolucao(long idDevolucao, long idEmprestimo, long idLivro, long idEstudante,
                                   long idAtendente, LocalDate vencimento, LocalDate dataDevolucao, long diasDeAtraso,
                                   SituacaoLivro situacaoLivro, List<String> avisos) implements Comprovante {

    public ComprovanteDevolucao {
        avisos = List.copyOf(avisos);
    }

    ComprovanteDevolucao comAvisos(List<String> novosAvisos) {
        return new ComprovanteDevolucao(idDevolucao, idEmprestimo, idLivro, idEstudante, idAtendente, vencimento,
                dataDevolucao, diasDeAtraso, situacaoLivro, novosAvisos);
    }
}
