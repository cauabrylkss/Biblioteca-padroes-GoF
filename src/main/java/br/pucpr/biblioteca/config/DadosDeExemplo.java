package br.pucpr.biblioteca.config;

import br.pucpr.biblioteca.biblioteca.acervo.GrupoDoAcervo;
import br.pucpr.biblioteca.biblioteca.acervo.OrganizacaoDoAcervo;
import br.pucpr.biblioteca.biblioteca.acervo.TipoGrupo;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dados de exemplo para a demonstração (D-10). Os cadastros passam pelos tratadores, como se fossem digitados.
 *
 * <ul>
 *   <li><b>Organização do acervo (R10)</b>: montada a cada execução, porque a árvore fica só em memória (C-08). Os
 *       livros de exemplo que já existem (no MySQL, de uma execução anterior) são catalogados de novo.</li>
 *   <li><b>Pessoas, papéis e livros</b>: cadastrados só se o banco estiver vazio — no MySQL eles continuam lá entre
 *       execuções. Para voltar ao estado inicial: {@code --reiniciar-dados}.</li>
 * </ul>
 *
 * Com o banco vazio, os ids ficam: estudantes Ana = 1 e Carla = 2, atendente Bruno = 1, livros 1 a 5.
 */
final class DadosDeExemplo {

    static final String CPF_ANA = "123.456.789-09";
    static final String CPF_BRUNO = "111.444.777-35";
    static final String CPF_CARLA = "529.982.247-25";

    private DadosDeExemplo() {
    }

    /** Monta a organização do acervo e, se não houver dados, cadastra os exemplos. */
    static void carregar(Aplicacao app) {
        Map<Long, Long> grupoDeCadaLivro = montarOrganizacao(app.organizacaoDoAcervo());
        boolean vazio = app.tratadorDeAcervo().consultarAcervo().isEmpty()
                && app.tratadorDePessoa().consultarPessoas().isEmpty();
        if (vazio) {
            cadastrarPessoas(app);
            cadastrarLivros(app, grupoDeCadaLivro);
        } else {
            catalogarLivrosExistentes(app, grupoDeCadaLivro);
        }
    }

    /** Acervo → Unidade Central → seções e subseção. Devolve o grupo de cada livro de exemplo (id do livro → grupo). */
    private static Map<Long, Long> montarOrganizacao(OrganizacaoDoAcervo organizacao) {
        GrupoDoAcervo central = organizacao.criarGrupo("Central", TipoGrupo.UNIDADE, OrganizacaoDoAcervo.ID_RAIZ);
        GrupoDoAcervo computacao = organizacao.criarGrupo("Computação", TipoGrupo.SECAO, central.getId());
        GrupoDoAcervo engenharia = organizacao.criarGrupo("Engenharia de Software", TipoGrupo.SUBSECAO,
                computacao.getId());
        GrupoDoAcervo matematica = organizacao.criarGrupo("Matemática", TipoGrupo.SECAO, central.getId());

        Map<Long, Long> grupos = new LinkedHashMap<>();
        grupos.put(1L, engenharia.getId());
        grupos.put(2L, engenharia.getId());
        grupos.put(3L, computacao.getId());
        grupos.put(4L, matematica.getId());
        grupos.put(5L, matematica.getId());
        return grupos;
    }

    private static void cadastrarPessoas(Aplicacao app) {
        app.tratadorDePessoa().inserirPessoa("Ana", "Souza", CPF_ANA);
        app.tratadorDePessoa().inserirPessoa("Bruno", "Lima", CPF_BRUNO);
        app.tratadorDePessoa().inserirPessoa("Carla", "Dias", CPF_CARLA);
        app.tratadorDeEstudante().inserirEstudante(CPF_ANA);
        app.tratadorDeEstudante().inserirEstudante(CPF_CARLA);
        app.tratadorDeAtendente().inserirAtendente(CPF_BRUNO);
    }

    private static void cadastrarLivros(Aplicacao app, Map<Long, Long> grupos) {
        String[] titulos = {"Padrões de Projeto", "Refatoração", "Código Limpo", "Cálculo Volume 1", "Álgebra Linear"};
        for (int i = 0; i < titulos.length; i++) {
            long id = i + 1;
            app.tratadorDeAcervo().inserirLivro(id, titulos[i], grupos.get(id));
        }
    }

    private static void catalogarLivrosExistentes(Aplicacao app, Map<Long, Long> grupos) {
        OrganizacaoDoAcervo organizacao = app.organizacaoDoAcervo();
        grupos.forEach((idLivro, idGrupo) -> {
            boolean existe = app.tratadorDeAcervo().consultarLivro(idLivro).isPresent();
            if (existe && organizacao.grupoDoLivro(idLivro).isEmpty()) {
                organizacao.catalogar(idLivro, idGrupo);
            }
        });
    }
}
