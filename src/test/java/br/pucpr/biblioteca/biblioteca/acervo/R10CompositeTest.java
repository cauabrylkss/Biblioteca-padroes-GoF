package br.pucpr.biblioteca.biblioteca.acervo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucpr.biblioteca.biblioteca.livro.Livro;
import br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro;
import br.pucpr.biblioteca.biblioteca.movimentacao.Emprestimo;
import br.pucpr.biblioteca.biblioteca.movimentacao.Reserva;
import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** R10 — Composite: organização hierárquica do acervo. */
class R10CompositeTest {

    private final OrganizacaoDoAcervo organizacao = new OrganizacaoDoAcervo();

    // ---------------------------------------------------------------------------------------------
    // Cenários de verificação do enunciado
    // ---------------------------------------------------------------------------------------------

    @Test
    void R10_unidadeComDuasSecoesTotalCinco() {
        GrupoDoAcervo unidade = organizacao.criarGrupo("Biblioteca Central", TipoGrupo.UNIDADE, OrganizacaoDoAcervo.ID_RAIZ);
        GrupoDoAcervo exatas = organizacao.criarGrupo("Exatas", TipoGrupo.SECAO, unidade.getId());
        GrupoDoAcervo humanas = organizacao.criarGrupo("Humanas", TipoGrupo.SECAO, unidade.getId());
        catalogar(exatas, 1, 2, 3);
        catalogar(humanas, 4, 5);

        assertThat(unidade.contarLivros()).isEqualTo(5);
        assertThat(exatas.contarLivros()).isEqualTo(3);
        assertThat(humanas.contarLivros()).isEqualTo(2);
        assertThat(unidade.listarLivros()).containsExactly(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    void R10_moverLivroEntreSecoesNaoAlteraIdEstadoNemEmprestimo() {
        GrupoDoAcervo unidade = organizacao.criarGrupo("Biblioteca Central", TipoGrupo.UNIDADE, OrganizacaoDoAcervo.ID_RAIZ);
        GrupoDoAcervo exatas = organizacao.criarGrupo("Exatas", TipoGrupo.SECAO, unidade.getId());
        GrupoDoAcervo humanas = organizacao.criarGrupo("Humanas", TipoGrupo.SECAO, unidade.getId());

        // livro emprestado de verdade (R1)
        LocalDate hoje = LocalDate.of(2026, 11, 25);
        Livro livro = new Livro(42, "Design Patterns");
        livro.reservar();
        livro.emprestar(new Reserva(42, 1, hoje), 1);
        Emprestimo emprestimo = new Emprestimo(42, 1, 7, hoje, 30);
        organizacao.catalogar(livro.getId(), exatas.getId());
        ItemDoAcervo itemAntes = exatas.localizarItem(42).orElseThrow();

        organizacao.mover(42, humanas.getId());

        assertThat(organizacao.grupoDoLivro(42)).contains(humanas);
        assertThat(humanas.localizarItem(42)).as("o mesmo item foi reposicionado").containsSame(itemAntes);
        assertThat(exatas.contarLivros()).isZero();
        assertThat(unidade.contarLivros()).isEqualTo(1);
        // nada no livro nem no empréstimo mudou
        assertThat(livro.getId()).isEqualTo(42);
        assertThat(livro.getSituacao()).isEqualTo(SituacaoLivro.EMPRESTADO);
        assertThat(emprestimo.getVencimento()).isEqualTo(hoje.plusDays(30));
        assertThat(emprestimo.getNumRenovacoes()).isZero();
    }

    // ---------------------------------------------------------------------------------------------
    // Regras do grupo e apoio à arguição
    // ---------------------------------------------------------------------------------------------

    @Test
    void R10_grupoRecusaCiclos() {
        GrupoDoAcervo unidade = organizacao.criarGrupo("Central", TipoGrupo.UNIDADE, OrganizacaoDoAcervo.ID_RAIZ);
        GrupoDoAcervo secao = organizacao.criarGrupo("Exatas", TipoGrupo.SECAO, unidade.getId());
        GrupoDoAcervo subsecao = organizacao.criarGrupo("Computação", TipoGrupo.SUBSECAO, secao.getId());

        // grupo dentro de si mesmo
        GrupoDoAcervo solto = new GrupoDoAcervo(99, "Solto", TipoGrupo.SECAO);
        assertThatThrownBy(() -> solto.adicionar(solto))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("Ciclo");

        // ancestral dentro de descendente: a unidade sai da raiz e tenta entrar na própria subseção
        organizacao.raiz().remover(unidade);
        assertThatThrownBy(() -> subsecao.adicionar(unidade))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("Ciclo");
        assertThat(subsecao.getFilhos()).isEmpty();
    }

    @Test
    void R10_grupoRecusaLivroQueJaPertenceAOutroGrupo() {
        GrupoDoAcervo unidade = organizacao.criarGrupo("Central", TipoGrupo.UNIDADE, OrganizacaoDoAcervo.ID_RAIZ);
        GrupoDoAcervo exatas = organizacao.criarGrupo("Exatas", TipoGrupo.SECAO, unidade.getId());
        GrupoDoAcervo humanas = organizacao.criarGrupo("Humanas", TipoGrupo.SECAO, unidade.getId());
        organizacao.catalogar(7, exatas.getId());

        assertThatThrownBy(() -> organizacao.catalogar(7, humanas.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("já pertence a SECAO Exatas");
        assertThatThrownBy(() -> humanas.adicionar(new ItemDoAcervo(7)))
                .as("a regra está no próprio grupo, não só na organização")
                .isInstanceOf(RegraDeNegocioException.class);
        assertThat(organizacao.contarLivros()).isEqualTo(1);
    }

    @Test
    void R10_grupoEItemRespondemAsMesmasOperacoes() {
        GrupoDoAcervo unidade = organizacao.criarGrupo("Central", TipoGrupo.UNIDADE, OrganizacaoDoAcervo.ID_RAIZ);
        GrupoDoAcervo secao = organizacao.criarGrupo("Exatas", TipoGrupo.SECAO, unidade.getId());
        catalogar(secao, 1, 2);
        organizacao.catalogar(3, unidade.getId());                   // livro direto na unidade
        ItemDoAcervo item = unidade.localizarItem(3).orElseThrow();

        List<NoDoAcervo> nos = List.of(secao, item);                // tratados igualmente
        int total = 0;
        for (NoDoAcervo no : nos) {
            total += no.contarLivros();
        }

        assertThat(total).isEqualTo(3);
        assertThat(item.listarLivros()).containsExactly(3L);
        assertThat(secao.listarLivros()).containsExactly(1L, 2L);
    }

    @Test
    void R10_arvoreDeProfundidadeVariavel() {
        GrupoDoAcervo unidade = organizacao.criarGrupo("Central", TipoGrupo.UNIDADE, OrganizacaoDoAcervo.ID_RAIZ);
        GrupoDoAcervo secao = organizacao.criarGrupo("Exatas", TipoGrupo.SECAO, unidade.getId());
        GrupoDoAcervo computacao = organizacao.criarGrupo("Computação", TipoGrupo.SUBSECAO, secao.getId());
        GrupoDoAcervo engenharia = organizacao.criarGrupo("Engenharia de Software", TipoGrupo.SUBSECAO, computacao.getId());
        catalogar(engenharia, 10, 11);
        catalogar(computacao, 12);

        assertThat(unidade.contarLivros()).isEqualTo(3);
        assertThat(organizacao.grupoDoLivro(10)).contains(engenharia);
    }

    @Test
    void R10_moverParaDestinoQueRecusaMantemOLivroNoLugar() {
        GrupoDoAcervo unidade = organizacao.criarGrupo("Central", TipoGrupo.UNIDADE, OrganizacaoDoAcervo.ID_RAIZ);
        GrupoDoAcervo exatas = organizacao.criarGrupo("Exatas", TipoGrupo.SECAO, unidade.getId());
        organizacao.catalogar(1, exatas.getId());

        assertThatThrownBy(() -> organizacao.mover(1, 999)).isInstanceOf(RegraDeNegocioException.class);

        assertThat(organizacao.grupoDoLivro(1)).contains(exatas);
    }

    private void catalogar(GrupoDoAcervo grupo, long... idsLivros) {
        for (long id : idsLivros) {
            organizacao.catalogar(id, grupo.getId());
        }
    }
}
