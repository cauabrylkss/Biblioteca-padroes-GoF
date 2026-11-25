package br.pucpr.biblioteca.biblioteca.movimentacao;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Transação de circulação registrada em {@code Movimentacao}: base comum de {@link Reserva}, {@link Emprestimo} e
 * {@link Devolucao} (modelo do Trabalho 02). A classe é {@code sealed}: só esses três tipos existem, o que permite aos
 * repositórios tratar cada um sem um {@code default} esquecido.
 *
 * <p>O identificador é atribuído uma única vez, pelo repositório, quando a transação é gravada.
 */
public abstract sealed class Transacao permits Reserva, Emprestimo, Devolucao {

    private Long id;
    private final long idLivro;
    private final long idEstudante;
    private final LocalDate dataTransacao;

    protected Transacao(long idLivro, long idEstudante, LocalDate dataTransacao) {
        this.idLivro = idLivro;
        this.idEstudante = idEstudante;
        this.dataTransacao = Objects.requireNonNull(dataTransacao, "dataTransacao");
    }

    public Long getId() {
        return id;
    }

    /** Chamado pelo repositório ao gravar; um id nunca é trocado. */
    public void atribuirId(long novoId) {
        if (id != null) {
            throw new IllegalStateException("Transação já possui id " + id);
        }
        this.id = novoId;
    }

    public long getIdLivro() {
        return idLivro;
    }

    public long getIdEstudante() {
        return idEstudante;
    }

    public LocalDate getDataTransacao() {
        return dataTransacao;
    }
}
