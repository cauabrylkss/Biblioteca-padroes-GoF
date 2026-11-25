package br.pucpr.biblioteca.biblioteca.acervo;

import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Organização hierárquica do acervo (R10): guarda a raiz da árvore e oferece as operações de alto nível usadas pela
 * aplicação — criar grupos, catalogar, mover e retirar livros.
 *
 * <p>Não conhece {@code Livro} nem {@code Movimentacao}: trabalha só com ids de livro. Mover um livro de seção
 * apenas reposiciona o {@link ItemDoAcervo}; id, estado e empréstimo do livro não são tocados.
 * Mantida em memória (decisão C-08).
 */
public class OrganizacaoDoAcervo {

    public static final long ID_RAIZ = 0;

    private final GrupoDoAcervo raiz = new GrupoDoAcervo(ID_RAIZ, "Acervo", TipoGrupo.ACERVO);
    private final Map<Long, GrupoDoAcervo> grupos = new LinkedHashMap<>();
    private long proximoId = 1;

    public OrganizacaoDoAcervo() {
        grupos.put(ID_RAIZ, raiz);
    }

    public synchronized GrupoDoAcervo criarGrupo(String nome, TipoGrupo tipo, long idGrupoPai) {
        if (tipo == TipoGrupo.ACERVO) {
            throw new RegraDeNegocioException("O tipo ACERVO é reservado para a raiz.");
        }
        GrupoDoAcervo pai = grupo(idGrupoPai);
        GrupoDoAcervo novo = new GrupoDoAcervo(proximoId, nome, tipo);
        pai.adicionar(novo);
        grupos.put(proximoId++, novo);
        return novo;
    }

    /** Coloca um livro num grupo. Recusado se o livro já estiver em algum grupo. */
    public synchronized void catalogar(long idLivro, long idGrupo) {
        grupo(idGrupo).adicionar(new ItemDoAcervo(idLivro));
    }

    /** Move o livro para outro grupo. Se o destino recusar, o livro volta para onde estava. */
    public synchronized void mover(long idLivro, long idGrupoDestino) {
        ItemDoAcervo item = itemDoLivro(idLivro);
        GrupoDoAcervo destino = grupo(idGrupoDestino);
        GrupoDoAcervo origem = item.getPai().orElseThrow();
        if (origem == destino) {
            return;
        }
        origem.remover(item);
        try {
            destino.adicionar(item);
        } catch (RuntimeException recusa) {
            origem.adicionar(item);
            throw recusa;
        }
    }

    /** Tira o livro da organização (ex.: livro excluído ou incinerado). */
    public synchronized void retirar(long idLivro) {
        ItemDoAcervo item = itemDoLivro(idLivro);
        item.getPai().orElseThrow().remover(item);
    }

    public synchronized Optional<GrupoDoAcervo> grupoDoLivro(long idLivro) {
        return raiz.localizarItem(idLivro).flatMap(NoDoAcervo::getPai);
    }

    public synchronized GrupoDoAcervo grupo(long idGrupo) {
        GrupoDoAcervo g = grupos.get(idGrupo);
        if (g == null) {
            throw new RegraDeNegocioException("Grupo do acervo inexistente: " + idGrupo);
        }
        return g;
    }

    public synchronized List<GrupoDoAcervo> grupos() {
        return List.copyOf(grupos.values());
    }

    public GrupoDoAcervo raiz() {
        return raiz;
    }

    public synchronized int contarLivros() {
        return raiz.contarLivros();
    }

    private ItemDoAcervo itemDoLivro(long idLivro) {
        return raiz.localizarItem(idLivro).orElseThrow(
                () -> new RegraDeNegocioException("O livro " + idLivro + " não está catalogado em nenhum grupo."));
    }
}
