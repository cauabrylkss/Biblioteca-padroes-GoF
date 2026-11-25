package br.pucpr.biblioteca.biblioteca.livro;

import br.pucpr.biblioteca.biblioteca.movimentacao.Emprestimo;
import br.pucpr.biblioteca.biblioteca.movimentacao.Reserva;

/**
 * Papel GoF: <b>State</b> (R1) — estado do ciclo de vida de um {@link Livro}.
 *
 * <p>Há um método para cada evento da máquina de estados. A implementação padrão de <b>todos</b> eles é recusar o
 * evento com {@link TransicaoInvalidaException} <b>sem tocar no livro</b>. Cada estado concreto sobrescreve apenas
 * os eventos que aceita; o corpo do método sobrescrito contém a guarda e a ação da transição, e termina trocando o
 * estado do livro. Assim, cada transição do diagrama corresponde a um método da classe do estado de origem.
 *
 * <p>Os estados não guardam dados (o livro e o empréstimo chegam por parâmetro), então cada classe tem uma única
 * instância compartilhada ({@code INSTANCIA}).
 */
public abstract class EstadoLivro {

    /** Nome gravado na persistência e exibido na interface. */
    public abstract SituacaoLivro situacao();

    // ---- Eventos (padrão: recusar) ----

    /** LIVRE → RESERVADO. */
    void reservar(Livro livro) {
        throw recusar("reservar");
    }

    /** RESERVADO → EMPRESTADO, guarda {@code [reserva.idEstudante = idEstudante]}. */
    void emprestar(Livro livro, Reserva reserva, long idEstudante) {
        throw recusar("emprestar");
    }

    /** EMPRESTADO/RENOVADO → RENOVADO, guarda {@code [numRenovacoes < 2]}, ação {@code emprestimo.prorrogar()}. */
    void renovar(Livro livro, Emprestimo emprestimo) {
        throw recusar("renovar");
    }

    /** EMPRESTADO/RENOVADO → DEVOLVIDO. */
    void registrarDevolucao(Livro livro) {
        throw recusar("registrar devolução");
    }

    /** DEVOLVIDO → LIVRE {@code [emBomEstado]} ou → REPARANDO {@code [not emBomEstado]}. */
    void inspecionar(Livro livro, boolean emBomEstado) {
        throw recusar("inspecionar");
    }

    /** LIVRE → REPARANDO. */
    void enviarParaReparo(Livro livro) {
        throw recusar("enviar para reparo");
    }

    /** REPARANDO → LIVRE. */
    void retornarDoReparo(Livro livro) {
        throw recusar("retornar do reparo");
    }

    /** REPARANDO → INCINERADO (evento de laudo irrecuperável). */
    void registrarLaudoIrrecuperavel(Livro livro) {
        throw recusar("registrar laudo irrecuperável");
    }

    /** LIVRE → estado final: o livro pode ser removido do acervo. */
    void excluir(Livro livro) {
        throw recusar("excluir");
    }

    protected final TransicaoInvalidaException recusar(String evento) {
        return new TransicaoInvalidaException(evento, situacao());
    }

    /**
     * Reconstitui o objeto de estado a partir do nome gravado na persistência. É o único lugar com um {@code switch}
     * sobre a situação — e ele só converte dado em objeto; nenhuma regra é decidida aqui.
     */
    public static EstadoLivro de(SituacaoLivro situacao) {
        return switch (situacao) {
            case LIVRE -> Livre.INSTANCIA;
            case RESERVADO -> Reservado.INSTANCIA;
            case EMPRESTADO -> Emprestado.INSTANCIA;
            case RENOVADO -> Renovado.INSTANCIA;
            case DEVOLVIDO -> Devolvido.INSTANCIA;
            case REPARANDO -> Reparando.INSTANCIA;
            case INCINERADO -> Incinerado.INSTANCIA;
        };
    }

    @Override
    public String toString() {
        return situacao().name();
    }
}
