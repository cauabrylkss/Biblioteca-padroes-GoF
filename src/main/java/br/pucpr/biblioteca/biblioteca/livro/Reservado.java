package br.pucpr.biblioteca.biblioteca.livro;

import br.pucpr.biblioteca.biblioteca.movimentacao.Reserva;
import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;

/**
 * Papel GoF: <b>ConcreteState</b> (State, R1) — RESERVADO.
 *
 * <p>Transição que sai daqui: {@code emprestar(reserva, idEstudante) [reserva.idEstudante = idEstudante]} →
 * EMPRESTADO. Empréstimo sem reserva não existe (B-04): LIVRE não aceita {@code emprestar}.
 */
final class Reservado extends EstadoLivro {

    static final Reservado INSTANCIA = new Reservado();

    private Reservado() {
    }

    @Override
    public SituacaoLivro situacao() {
        return SituacaoLivro.RESERVADO;
    }

    @Override
    void emprestar(Livro livro, Reserva reserva, long idEstudante) {
        // Guarda: a reserva tem de ser do livro e do mesmo estudante que está pegando o livro.
        if (reserva.getIdLivro() != livro.getId() || !reserva.pertenceA(idEstudante)) {
            throw new RegraDeNegocioException(
                    "O livro " + livro.getId() + " está reservado para outro estudante.");
        }
        livro.mudarPara(Emprestado.INSTANCIA);
    }
}
