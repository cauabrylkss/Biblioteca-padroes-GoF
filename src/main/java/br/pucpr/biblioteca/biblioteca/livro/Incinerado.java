package br.pucpr.biblioteca.biblioteca.livro;

/**
 * Papel GoF: <b>ConcreteState</b> (State, R1) — INCINERADO.
 *
 * <p>Estado final (B-19): não sobrescreve nenhum evento, portanto todos são recusados. O registro do livro
 * permanece para histórico.
 */
final class Incinerado extends EstadoLivro {

    static final Incinerado INSTANCIA = new Incinerado();

    private Incinerado() {
    }

    @Override
    public SituacaoLivro situacao() {
        return SituacaoLivro.INCINERADO;
    }
}
