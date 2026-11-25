package br.pucpr.biblioteca.biblioteca.acervo;

import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Papel GoF: <b>Composite</b> (Composite, R10) — unidade, seção ou subseção do acervo.
 *
 * <p>Contém outros nós (grupos ou itens) e responde às operações de {@link NoDoAcervo} repassando-as aos filhos:
 * {@link #contarLivros()} soma a contagem de cada filho, {@link #listarLivros()} junta as listas. A recursão para
 * sozinha nos itens, que respondem por si.
 *
 * <p>Regras garantidas pelo próprio grupo em {@link #adicionar(NoDoAcervo)}:
 * <ul>
 *   <li><b>sem ciclos</b>: um grupo não pode entrar dentro de si mesmo nem de um descendente seu;</li>
 *   <li><b>livro em um só grupo</b>: não entra livro que já esteja em qualquer grupo da mesma árvore;</li>
 *   <li>um nó que já tem pai não é adicionado em outro lugar (para trocar de lugar, use mover).</li>
 * </ul>
 */
public final class GrupoDoAcervo extends NoDoAcervo {

    private final long id;
    private final String nome;
    private final TipoGrupo tipo;
    private final List<NoDoAcervo> filhos = new ArrayList<>();

    public GrupoDoAcervo(long id, String nome, TipoGrupo tipo) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do grupo é obrigatório");
        }
        this.id = id;
        this.nome = nome.strip();
        this.tipo = Objects.requireNonNull(tipo, "tipo");
    }

    public void adicionar(NoDoAcervo filho) {
        Objects.requireNonNull(filho, "filho");
        if (filho.getPai().isPresent()) {
            throw new RegraDeNegocioException(
                    filho.descricao() + " já pertence a " + filho.getPai().get().descricao() + ".");
        }
        if (filho.ehIgualOuAncestralDe(this)) {
            throw new RegraDeNegocioException(
                    "Ciclo recusado: " + filho.descricao() + " não pode ficar dentro de " + descricao() + ".");
        }
        Set<Long> livrosJaNaArvore = new HashSet<>(raiz().listarLivros());
        for (Long idLivro : filho.listarLivros()) {
            if (livrosJaNaArvore.contains(idLivro)) {
                String ondeEsta = raiz().localizarItem(idLivro)
                        .flatMap(NoDoAcervo::getPai).map(NoDoAcervo::descricao).orElse("outro grupo");
                throw new RegraDeNegocioException("O livro " + idLivro + " já pertence a " + ondeEsta + ".");
            }
        }
        filhos.add(filho);
        filho.definirPai(this);
    }

    public void remover(NoDoAcervo filho) {
        if (!filhos.remove(filho)) {
            throw new RegraDeNegocioException(filho.descricao() + " não está em " + descricao() + ".");
        }
        filho.definirPai(null);
    }

    @Override
    public int contarLivros() {
        int total = 0;
        for (NoDoAcervo filho : filhos) {
            total += filho.contarLivros();          // grupo filho conta os seus; item conta 1
        }
        return total;
    }

    @Override
    public List<Long> listarLivros() {
        List<Long> livros = new ArrayList<>();
        for (NoDoAcervo filho : filhos) {
            livros.addAll(filho.listarLivros());
        }
        return livros;
    }

    @Override
    public Optional<ItemDoAcervo> localizarItem(long idLivro) {
        for (NoDoAcervo filho : filhos) {
            Optional<ItemDoAcervo> achado = filho.localizarItem(idLivro);
            if (achado.isPresent()) {
                return achado;
            }
        }
        return Optional.empty();
    }

    @Override
    public String descricao() {
        return tipo + " " + nome;
    }

    public long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public TipoGrupo getTipo() {
        return tipo;
    }

    public List<NoDoAcervo> getFilhos() {
        return List.copyOf(filhos);
    }
}
