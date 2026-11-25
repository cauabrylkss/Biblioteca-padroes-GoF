package br.pucpr.biblioteca.biblioteca.acervo;

import br.pucpr.biblioteca.biblioteca.livro.Livro;
import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;
import br.pucpr.biblioteca.persistencia.RepositorioDeLivros;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Ponto interno de acesso <b>Acervo</b> (Trabalho 02): livros do acervo e sua organização hierárquica (R10).
 *
 * <p>Os livros ficam no repositório criado pela fábrica de persistência do perfil (R8). Cada consulta devolve o
 * livro com o estado <b>atual</b> gravado — é daqui que a fachada lê o livro antes de reservar (R9: o cache de
 * metadados nunca participa dessa decisão).
 */
public class Acervo {

    private final RepositorioDeLivros repositorio;
    private final OrganizacaoDoAcervo organizacao;

    public Acervo(RepositorioDeLivros repositorio, OrganizacaoDoAcervo organizacao) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.organizacao = Objects.requireNonNull(organizacao, "organizacao");
    }

    public Livro inserirLivro(Livro livro) {
        repositorio.salvar(livro);
        return livro;
    }

    /** Grava as alterações do livro (título ou estado, depois de um evento). */
    public void atualizarLivro(Livro livro) {
        exigirLivro(livro.getId());
        repositorio.salvar(livro);
    }

    public Optional<Livro> consultarLivro(long idLivro) {
        return repositorio.buscarPorId(idLivro);
    }

    public Livro exigirLivro(long idLivro) {
        return consultarLivro(idLivro)
                .orElseThrow(() -> new RegraDeNegocioException("Livro não cadastrado: " + idLivro + "."));
    }

    public List<Livro> listarLivros() {
        return repositorio.listarTodos();
    }

    /**
     * Exclui o livro. Quem decide se pode é o estado do livro ({@code excluir()} só é aceito em LIVRE, B-18); aceito,
     * o livro sai do repositório e da organização do acervo.
     */
    public void excluirLivro(long idLivro) {
        Livro livro = exigirLivro(idLivro);
        livro.excluir();
        repositorio.remover(idLivro);
        retirarDaOrganizacao(idLivro);
    }

    /** Coloca o livro num grupo da árvore do acervo (R10). */
    public void catalogar(long idLivro, long idGrupo) {
        exigirLivro(idLivro);
        organizacao.catalogar(idLivro, idGrupo);
    }

    /** Tira o livro da árvore, se ele estiver catalogado (livro excluído ou incinerado, B-19). */
    public void retirarDaOrganizacao(long idLivro) {
        if (organizacao.grupoDoLivro(idLivro).isPresent()) {
            organizacao.retirar(idLivro);
        }
    }

    public OrganizacaoDoAcervo organizacao() {
        return organizacao;
    }
}
