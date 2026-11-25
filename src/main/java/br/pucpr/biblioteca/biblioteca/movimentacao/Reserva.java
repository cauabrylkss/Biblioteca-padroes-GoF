package br.pucpr.biblioteca.biblioteca.movimentacao;

import java.time.LocalDate;

/** Reserva ativa de um livro por um estudante. É consumida (removida) quando o empréstimo é efetivado (R4). */
public final class Reserva extends Transacao {

    public Reserva(long idLivro, long idEstudante, LocalDate dataReserva) {
        super(idLivro, idEstudante, dataReserva);
    }

    /** Reconstitui uma reserva gravada (usado pelos repositórios). */
    public static Reserva reconstituir(long id, long idLivro, long idEstudante, LocalDate dataReserva) {
        Reserva r = new Reserva(idLivro, idEstudante, dataReserva);
        r.atribuirId(id);
        return r;
    }

    /** Usado pela guarda de {@code Reservado.emprestar}: só o estudante da reserva pode efetivar o empréstimo. */
    public boolean pertenceA(long idEstudante) {
        return getIdEstudante() == idEstudante;
    }
}
