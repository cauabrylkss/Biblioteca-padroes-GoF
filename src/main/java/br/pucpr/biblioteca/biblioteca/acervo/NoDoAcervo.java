package br.pucpr.biblioteca.biblioteca.acervo;

import java.util.List;
import java.util.Optional;

/**
 * Papel GoF: <b>Component</b> (Composite, R10) — qualquer nó da árvore de organização do acervo.
 *
 * <p>Grupos ({@link GrupoDoAcervo}: unidade, seção, subseção) e itens ({@link ItemDoAcervo}: referência a um livro)
 * respondem às <b>mesmas operações</b>, então quem usa a árvore não precisa saber se está diante de uma seção
 * inteira ou de um único livro.
 */
public abstract class NoDoAcervo {

    private GrupoDoAcervo pai;

    /** Quantidade de livros neste nó (1 para um item; soma dos filhos para um grupo). */
    public abstract int contarLivros();

    /** Ids dos livros neste nó, na ordem da árvore. */
    public abstract List<Long> listarLivros();

    /** Procura, a partir deste nó, o item que referencia o livro. */
    public abstract Optional<ItemDoAcervo> localizarItem(long idLivro);

    /** Texto para exibição (ex.: "SECAO Exatas"). */
    public abstract String descricao();

    public Optional<GrupoDoAcervo> getPai() {
        return Optional.ofNullable(pai);
    }

    /** Topo da árvore a que este nó pertence. */
    public NoDoAcervo raiz() {
        NoDoAcervo atual = this;
        while (atual.pai != null) {
            atual = atual.pai;
        }
        return atual;
    }

    /** {@code true} se este nó é o próprio {@code outro} ou um dos seus ancestrais (usado para impedir ciclos). */
    boolean ehIgualOuAncestralDe(NoDoAcervo outro) {
        for (NoDoAcervo n = outro; n != null; n = n.pai) {
            if (n == this) {
                return true;
            }
        }
        return false;
    }

    /** Só {@link GrupoDoAcervo} muda o pai de um nó, ao adicionar ou remover filhos. */
    void definirPai(GrupoDoAcervo novoPai) {
        this.pai = novoPai;
    }
}
