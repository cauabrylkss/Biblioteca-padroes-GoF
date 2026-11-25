package br.pucpr.biblioteca.biblioteca.circulacao;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Papel GoF: <b>ConcreteCommand</b> (Command, R6) — renovar o empréstimo de um livro.
 * O limite de 2 renovações continua no domínio (estados do livro); a solicitação só encaminha.
 */
public final class SolicitacaoDeRenovacao extends SolicitacaoDeCirculacao {

    private final long idLivro;
    private final long idEstudante;

    public SolicitacaoDeRenovacao(UUID id, String autor, Instant criadaEm, long idLivro, long idEstudante) {
        super(id, autor, criadaEm);
        this.idLivro = idLivro;
        this.idEstudante = idEstudante;
    }

    /** Cria uma solicitação nova, com identificador gerado e o instante atual do relógio. */
    public static SolicitacaoDeRenovacao nova(String autor, Clock relogio, long idLivro, long idEstudante) {
        return new SolicitacaoDeRenovacao(UUID.randomUUID(), autor, relogio.instant(), idLivro, idEstudante);
    }

    @Override
    protected ComprovanteRenovacao executar(FachadaDeCirculacao fachada) {
        return fachada.renovar(idLivro, idEstudante);
    }

    @Override
    public String descricao() {
        return "Renovação do empréstimo do livro " + idLivro + " (estudante " + idEstudante + ")";
    }

    public long getIdLivro() {
        return idLivro;
    }

    public long getIdEstudante() {
        return idEstudante;
    }
}
