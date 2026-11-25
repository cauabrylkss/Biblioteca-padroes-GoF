package br.pucpr.biblioteca.persistencia.memoria;

import br.pucpr.biblioteca.biblioteca.livro.Livro;
import br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro;
import br.pucpr.biblioteca.persistencia.RepositorioDeLivros;
import java.util.List;
import java.util.Optional;

/**
 * Papel GoF: <b>ConcreteProduct</b> (Abstract Factory, R8) — livros em memória.
 *
 * <p>Grava o livro como no banco: id, título e o <b>nome</b> do estado ({@link SituacaoLivro}); na leitura,
 * reconstitui o objeto de estado com {@link Livro#reconstituir} (R1). O construtor recebe a unidade de trabalho
 * concreta da família em memória e só é visível dentro do pacote: quem cria este repositório é a
 * {@link FabricaEmMemoria}.
 */
public final class RepositorioDeLivrosEmMemoria implements RepositorioDeLivros {

    private record RegistroLivro(long id, String titulo, SituacaoLivro situacao) {
    }

    private final UnidadeDeTrabalhoEmMemoria unidade;
    private final TabelaEmMemoria<RegistroLivro> tabela;

    RepositorioDeLivrosEmMemoria(UnidadeDeTrabalhoEmMemoria unidade) {
        this.unidade = unidade;
        this.tabela = new TabelaEmMemoria<>(unidade);
    }

    UnidadeDeTrabalhoEmMemoria unidadeDeTrabalho() {
        return unidade;
    }

    @Override
    public void salvar(Livro livro) {
        RegistroLivro registro = new RegistroLivro(livro.getId(), livro.getTitulo(), livro.getSituacao());
        unidade.comTrava(() -> tabela.gravar(registro.id(), registro));
    }

    @Override
    public Optional<Livro> buscarPorId(long idLivro) {
        return unidade.comTrava(() -> tabela.buscar(idLivro).map(RepositorioDeLivrosEmMemoria::paraLivro));
    }

    @Override
    public List<Livro> listarTodos() {
        return unidade.comTrava(() -> tabela.todos().stream().map(RepositorioDeLivrosEmMemoria::paraLivro).toList());
    }

    @Override
    public void remover(long idLivro) {
        unidade.comTrava(() -> tabela.remover(idLivro));
    }

    private static Livro paraLivro(RegistroLivro r) {
        return Livro.reconstituir(r.id(), r.titulo(), r.situacao());
    }
}
