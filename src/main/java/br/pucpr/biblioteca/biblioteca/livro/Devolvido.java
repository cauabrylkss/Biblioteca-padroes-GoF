package br.pucpr.biblioteca.biblioteca.livro;

/**
 * Papel GoF: <b>ConcreteState</b> (State, R1) — DEVOLVIDO (aguardando inspeção).
 *
 * <p>Transições que saem daqui: {@code inspecionar(emBomEstado) [emBomEstado]} → LIVRE e
 * {@code inspecionar(emBomEstado) [not emBomEstado]} → REPARANDO. Não aceita {@code reservar}: um livro devolvido
 * ainda não inspecionado não pode ser reservado.
 */
final class Devolvido extends EstadoLivro {

    static final Devolvido INSTANCIA = new Devolvido();

    private Devolvido() {
    }

    @Override
    public SituacaoLivro situacao() {
        return SituacaoLivro.DEVOLVIDO;
    }

    @Override
    void inspecionar(Livro livro, boolean emBomEstado) {
        if (emBomEstado) {
            livro.mudarPara(Livre.INSTANCIA);
        } else {
            livro.mudarPara(Reparando.INSTANCIA);
        }
    }
}
