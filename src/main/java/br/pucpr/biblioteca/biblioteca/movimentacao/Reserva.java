package br.pucpr.biblioteca.biblioteca.movimentacao;

import java.time.LocalDate;

/** Reserva ativa de um livro por um estudante. É consumida (removida) quando o empréstimo é efetivado (R4). */
public class Reserva extends Transacao {

    public Reserva(long idLivro, long idEstudante, LocalDate dataReserva) {
        super(idLivro, idEstudante, dataReserva);
    }

    /** Usado pela guarda de {@code Reservado.emprestar}: só o estudante da reserva pode efetivar o empréstimo. */
    public boolean pertenceA(long idEstudante) {
        return getIdEstudante() == idEstudante;
    }
}
