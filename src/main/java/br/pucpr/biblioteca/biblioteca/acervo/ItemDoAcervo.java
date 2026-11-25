package br.pucpr.biblioteca.biblioteca.acervo;

import java.util.List;
import java.util.Optional;

/**
 * Papel GoF: <b>Leaf</b> (Composite, R10) — a posição de um livro na organização do acervo.
 *
 * <p>Guarda <b>apenas o identificador</b> do livro, nunca o objeto {@code Livro}. Por isso organizar ou mover itens
 * não tem como alterar o estado do livro nem a movimentação (empréstimos, reservas).
 */
public final class ItemDoAcervo extends NoDoAcervo {

    private final long idLivro;

    public ItemDoAcervo(long idLivro) {
        if (idLivro <= 0) {
            throw new IllegalArgumentException("O id do livro deve ser positivo");
        }
        this.idLivro = idLivro;
    }

    public long getIdLivro() {
        return idLivro;
    }

    @Override
    public int contarLivros() {
        return 1;
    }

    @Override
    public List<Long> listarLivros() {
        return List.of(idLivro);
    }

    @Override
    public Optional<ItemDoAcervo> localizarItem(long id) {
        return id == idLivro ? Optional.of(this) : Optional.empty();
    }

    @Override
    public String descricao() {
        return "Livro " + idLivro;
    }
}
