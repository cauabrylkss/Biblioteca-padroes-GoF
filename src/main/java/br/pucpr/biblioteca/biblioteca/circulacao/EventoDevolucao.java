package br.pucpr.biblioteca.biblioteca.circulacao;

import br.pucpr.biblioteca.compartilhado.DiasDeAtraso;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Evento publicado depois que uma devolução é confirmada (R5).
 *
 * <p>Leva <b>todos os dados</b> de que os ouvintes precisam — estudante, vencimento e data de devolução —, tirados no
 * momento da devolução. Assim nenhum ouvinte precisa consultar o banco depois (quando o empréstimo poderia já ter
 * mudado) e cada um pode rodar isolado dos demais.
 */
public record EventoDevolucao(long idLivro, long idEstudante, long idEmprestimo, LocalDate vencimento,
                              LocalDate dataDevolucao) {

    public EventoDevolucao {
        Objects.requireNonNull(vencimento, "vencimento");
        Objects.requireNonNull(dataDevolucao, "dataDevolucao");
    }

    public long diasDeAtraso() {
        return DiasDeAtraso.entre(vencimento, dataDevolucao);
    }

    public boolean atrasada() {
        return diasDeAtraso() > 0;
    }
}
