package br.pucpr.biblioteca.biblioteca.livro;

import br.pucpr.biblioteca.biblioteca.movimentacao.Emprestimo;
import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;

/**
 * Papel GoF: <b>ConcreteState</b> (State, R1) — RENOVADO.
 *
 * <p>Transições que saem daqui: {@code renovar(emprestimo) [numRenovacoes < 2] / emprestimo.prorrogar()} →
 * RENOVADO (auto-transição); {@code registrarDevolucao()} → DEVOLVIDO. É aqui que a 3ª renovação é recusada.
 */
final class Renovado extends EstadoLivro {

    static final Renovado INSTANCIA = new Renovado();

    private Renovado() {
    }

    @Override
    public SituacaoLivro situacao() {
        return SituacaoLivro.RENOVADO;
    }

    @Override
    void renovar(Livro livro, Emprestimo emprestimo) {
        // Guarda avaliada ANTES da ação: se falhar, o vencimento não é tocado.
        if (emprestimo.getNumRenovacoes() >= Emprestimo.LIMITE_RENOVACOES) {
            throw new RegraDeNegocioException("Limite de " + Emprestimo.LIMITE_RENOVACOES + " renovações atingido.");
        }
        emprestimo.prorrogar();                 // ação: vencimento + 30 dias, numRenovacoes + 1
        livro.mudarPara(Renovado.INSTANCIA);    // auto-transição
    }

    @Override
    void registrarDevolucao(Livro livro) {
        livro.mudarPara(Devolvido.INSTANCIA);
    }
}
