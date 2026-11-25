package br.pucpr.biblioteca.biblioteca.livro;

/**
 * Papel GoF: <b>ConcreteState</b> (State, R1) — LIVRE.
 *
 * <p>Transições que saem daqui: {@code reservar()} → RESERVADO; {@code enviarParaReparo()} → REPARANDO;
 * {@code excluir()} → estado final. Qualquer outro evento é recusado pela implementação padrão de
 * {@link EstadoLivro}.
 */
final class Livre extends EstadoLivro {

    static final Livre INSTANCIA = new Livre();

    private Livre() {
    }

    @Override
    public SituacaoLivro situacao() {
        return SituacaoLivro.LIVRE;
    }

    @Override
    void reservar(Livro livro) {
        livro.mudarPara(Reservado.INSTANCIA);
    }

    @Override
    void enviarParaReparo(Livro livro) {
        livro.mudarPara(Reparando.INSTANCIA);
    }

    @Override
    void excluir(Livro livro) {
        // Aceito: chega ao estado final. O livro deixa de existir no acervo (o Acervo o remove do repositório).
    }
}
