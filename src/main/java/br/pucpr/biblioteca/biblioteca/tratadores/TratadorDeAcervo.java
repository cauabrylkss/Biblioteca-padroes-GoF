package br.pucpr.biblioteca.biblioteca.tratadores;

import br.pucpr.biblioteca.biblioteca.acervo.Acervo;
import br.pucpr.biblioteca.biblioteca.acervo.ConsultaAcervo;
import br.pucpr.biblioteca.biblioteca.acervo.GrupoDoAcervo;
import br.pucpr.biblioteca.biblioteca.acervo.MetadadosLivro;
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
 *
 * <p>Consultas de metadados passam pela {@link ConsultaAcervo} montada no composition root (Decorator, R9) — o tratador
 * não sabe se há cache ou medição. Ao alterar o título ou excluir um livro, avisa a consulta para o cache descartar o
 * livro (C-10). A ficha completa ({@link #consultarLivro}) continua lendo o estado atual do repositório.
 */
public class TratadorDeAcervo implements iLivro {

    private final CadastroDeLivro cadastro;
    private final Acervo acervo;
    private final UnidadeDeTrabalho unidadeDeTrabalho;
    private final ConsultaAcervo consulta;

    public TratadorDeAcervo(CadastroDeLivro cadastro, Acervo acervo, UnidadeDeTrabalho unidadeDeTrabalho,
                            ConsultaAcervo consulta) {
        this.cadastro = Objects.requireNonNull(cadastro, "cadastro");
        this.acervo = Objects.requireNonNull(acervo, "acervo");
        this.unidadeDeTrabalho = Objects.requireNonNull(unidadeDeTrabalho, "unidadeDeTrabalho");
        this.consulta = Objects.requireNonNull(consulta, "consulta");
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
        consulta.livroAlterado(idLivro);
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
    public Optional<MetadadosLivro> consultarMetadados(long idLivro) {
        return consulta.consultarMetadados(idLivro);
    }

    @Override
    public List<MetadadosLivro> buscarPorTitulo(String trecho) {
        return consulta.buscarPorTitulo(trecho);
    }

    @Override
    public void excluirLivro(long idLivro) {
        unidadeDeTrabalho.executar(() -> acervo.excluirLivro(idLivro));
        consulta.livroAlterado(idLivro);
    }

    private FichaDoLivro ficha(Livro livro) {
        String grupo = acervo.organizacao().grupoDoLivro(livro.getId()).map(GrupoDoAcervo::descricao).orElse(null);
        return FichaDoLivro.de(livro, grupo);
    }
}
