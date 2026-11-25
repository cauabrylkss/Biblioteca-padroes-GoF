package br.pucpr.biblioteca.biblioteca.movimentacao;

import br.pucpr.biblioteca.compartilhado.DiasDeAtraso;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Devolução de um empréstimo. Guarda o vencimento que valia no momento da devolução, para que o atraso possa ser
 * explicado depois mesmo que o empréstimo não seja mais consultado. Um empréstimo é considerado <b>ativo</b> enquanto
 * não existe devolução que o referencie.
 */
public final class Devolucao extends Transacao {

    private final long idEmprestimo;
    private final long idAtendente;
    private final LocalDate vencimento;

    /** Registra a devolução do empréstimo informado na data indicada. */
    public Devolucao(Emprestimo emprestimo, long idAtendente, LocalDate dataDevolucao) {
        this(emprestimo.getId(), emprestimo.getIdLivro(), emprestimo.getIdEstudante(), idAtendente,
                emprestimo.getVencimento(), dataDevolucao);
    }

    private Devolucao(Long idEmprestimo, long idLivro, long idEstudante, long idAtendente, LocalDate vencimento,
                      LocalDate dataDevolucao) {
        super(idLivro, idEstudante, dataDevolucao);
        this.idEmprestimo = Objects.requireNonNull(idEmprestimo, "o empréstimo precisa estar gravado (com id)");
        this.idAtendente = idAtendente;
        this.vencimento = Objects.requireNonNull(vencimento, "vencimento");
    }

    /** Reconstitui uma devolução gravada (usado pelos repositórios). */
    public static Devolucao reconstituir(long id, long idEmprestimo, long idLivro, long idEstudante, long idAtendente,
                                         LocalDate vencimento, LocalDate dataDevolucao) {
        Devolucao d = new Devolucao(idEmprestimo, idLivro, idEstudante, idAtendente, vencimento, dataDevolucao);
        d.atribuirId(id);
        return d;
    }

    public long getIdEmprestimo() {
        return idEmprestimo;
    }

    public long getIdAtendente() {
        return idAtendente;
    }

    public LocalDate getVencimento() {
        return vencimento;
    }

    public LocalDate getDataDevolucao() {
        return getDataTransacao();
    }

    /** Dias corridos de atraso (B-07). */
    public long diasDeAtraso() {
        return DiasDeAtraso.entre(vencimento, getDataDevolucao());
    }
}
