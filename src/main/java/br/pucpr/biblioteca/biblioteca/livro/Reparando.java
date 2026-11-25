package br.pucpr.biblioteca.biblioteca.livro;

/**
 * Papel GoF: <b>ConcreteState</b> (State, R1) — REPARANDO.
 *
 * <p>Transições que saem daqui: {@code retornarDoReparo()} → LIVRE; {@code registrarLaudoIrrecuperavel()} →
 * INCINERADO (evento de laudo irrecuperável exigido pelo enunciado).
 */
final class Reparando extends EstadoLivro {

    static final Reparando INSTANCIA = new Reparando();

    private Reparando() {
    }

    @Override
    public SituacaoLivro situacao() {
        return SituacaoLivro.REPARANDO;
    }

    @Override
    void retornarDoReparo(Livro livro) {
        livro.mudarPara(Livre.INSTANCIA);
    }

    @Override
    void registrarLaudoIrrecuperavel(Livro livro) {
        livro.mudarPara(Incinerado.INSTANCIA);
    }
}
