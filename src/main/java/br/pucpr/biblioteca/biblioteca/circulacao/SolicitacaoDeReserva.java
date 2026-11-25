package br.pucpr.biblioteca.biblioteca.circulacao;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/** Papel GoF: <b>ConcreteCommand</b> (Command, R6) — reservar um livro para um estudante. */
public final class SolicitacaoDeReserva extends SolicitacaoDeCirculacao {

    private final long idLivro;
    private final long idEstudante;

    public SolicitacaoDeReserva(UUID id, String autor, Instant criadaEm, long idLivro, long idEstudante) {
        super(id, autor, criadaEm);
        this.idLivro = idLivro;
        this.idEstudante = idEstudante;
    }

    /** Cria uma solicitação nova, com identificador gerado e o instante atual do relógio. */
    public static SolicitacaoDeReserva nova(String autor, Clock relogio, long idLivro, long idEstudante) {
        return new SolicitacaoDeReserva(UUID.randomUUID(), autor, relogio.instant(), idLivro, idEstudante);
    }

    @Override
    protected ComprovanteReserva executar(FachadaDeCirculacao fachada) {
        return fachada.reservar(idLivro, idEstudante);
    }

    @Override
    public String descricao() {
        return "Reserva do livro " + idLivro + " para o estudante " + idEstudante;
    }

    public long getIdLivro() {
        return idLivro;
    }

    public long getIdEstudante() {
        return idEstudante;
    }
}
