package br.pucpr.biblioteca.biblioteca.tratadores;

import br.pucpr.biblioteca.biblioteca.acervo.Acervo;
import br.pucpr.biblioteca.biblioteca.acervo.GrupoDoAcervo;
import br.pucpr.biblioteca.biblioteca.livro.Livro;
import br.pucpr.biblioteca.cadastro.CadastroDeLivro;
import br.pucpr.biblioteca.cadastro.Resultado;
import br.pucpr.biblioteca.persistencia.UnidadeDeTrabalho;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Tratador do acervo (Trabalho 02), realiza {@link iLivro}. Delega a inclusão ao {@link CadastroDeLivro}
 * (Template Method, R7) e as demais operações ao {@link Acervo}. Não testa o estado do livro: a exclusão dispara o
 * evento {@code excluir()} e o estado decide.
 */
public class TratadorDeAcervo implements iLivro {

    private final CadastroDeLivro cadastro;
    private final Acervo acervo;
    private final UnidadeDeTrabalho unidadeDeTrabalho;

    public TratadorDeAcervo(CadastroDeLivro cadastro, Acervo acervo, UnidadeDeTrabalho unidadeDeTrabalho) {
        this.cadastro = Objects.requireNonNull(cadastro, "cadastro");
        this.acervo = Objects.requireNonNull(acervo, "acervo");
        this.unidadeDeTrabalho = Objects.requireNonNull(unidadeDeTrabalho, "unidadeDeTrabalho");
    }

    @Override
    public Resultado<FichaDoLivro> inserirLivro(long idLivro, String titulo, Long idGrupo) {
        return cadastro.incluir(new CadastroDeLivro.Dados(idLivro, titulo, idGrupo)).mapear(this::ficha);
    }

    @Override
    public FichaDoLivro atualizarLivro(long idLivro, String novoTitulo) {
        Livro livro = acervo.exigirLivro(idLivro);
        livro.alterarTitulo(novoTitulo);
        acervo.atualizarLivro(livro);
        return ficha(livro);
    }

    @Override
    public Optional<FichaDoLivro> consultarLivro(long idLivro) {
        return acervo.consultarLivro(idLivro).map(this::ficha);
    }

    @Override
    public List<FichaDoLivro> consultarAcervo() {
        return acervo.listarLivros().stream().map(this::ficha).toList();
    }

    @Override
    public void excluirLivro(long idLivro) {
        unidadeDeTrabalho.executar(() -> acervo.excluirLivro(idLivro));
    }

    private FichaDoLivro ficha(Livro livro) {
        String grupo = acervo.organizacao().grupoDoLivro(livro.getId()).map(GrupoDoAcervo::descricao).orElse(null);
        return FichaDoLivro.de(livro, grupo);
    }
}
