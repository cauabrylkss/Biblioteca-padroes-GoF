package br.pucpr.biblioteca.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucpr.biblioteca.biblioteca.livro.Livro;
import br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro;
import br.pucpr.biblioteca.biblioteca.movimentacao.Reserva;
import br.pucpr.biblioteca.config.Aplicacao;
import br.pucpr.biblioteca.config.PerfilDeExecucao;
import br.pucpr.biblioteca.persistencia.jdbc.FabricaRelacional;
import br.pucpr.biblioteca.persistencia.memoria.FabricaEmMemoria;
import br.pucpr.biblioteca.rh.Pessoa;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/**
 * R8 — Abstract Factory: famílias compatíveis de acesso aos dados. Nada aqui precisa de MySQL: a família relacional é
 * conferida pelos tipos e pela unidade compartilhada (conexão preguiçosa). O uso real do MySQL está em
 * {@code R08MySqlIntegracaoTest} ({@code mvnw test -Pmysql}).
 */
class R08AbstractFactoryTest {

    private static final String PACOTE_MEMORIA = "br.pucpr.biblioteca.persistencia.memoria";
    private static final String PACOTE_JDBC = "br.pucpr.biblioteca.persistencia.jdbc";
    private static final String CPF_ANA = "123.456.789-09";

    // ---------------------------------------------------------------------------------------------
    // Cenário de verificação do enunciado
    // ---------------------------------------------------------------------------------------------

    @Test
    void R08_perfilTesteSoObjetosEmMemoria() {
        Properties configuracao = Aplicacao.carregarPropriedades();
        configuracao.setProperty("perfil", "teste");
        Aplicacao app = Aplicacao.montar(configuracao, Clock.systemDefaultZone());
        FabricaDePersistencia fabrica = app.fabrica();

        assertThat(app.perfil()).isEqualTo(PerfilDeExecucao.TESTE);
        assertThat(fabrica).isInstanceOf(FabricaEmMemoria.class);
        List<Object> produtos = List.of(fabrica.unidadeDeTrabalho(), fabrica.livros(), fabrica.movimentacao(),
                fabrica.relacionamento(), fabrica.multas());
        assertThat(produtos).allSatisfy(produto ->
                assertThat(produto.getClass().getPackageName()).isEqualTo(PACOTE_MEMORIA));

        // todos os repositórios da família usam a mesma unidade de trabalho, criada pela fábrica
        assertThat(((FabricaEmMemoria) fabrica).repositoriosCompartilhamAUnidade()).isTrue();
        assertThat(fabrica.unidadeDeTrabalho()).isSameAs(fabrica.unidadeDeTrabalho());
    }

    @Test
    void R08_perfilProducaoRepositoriosMysqlMesmaUnidade() {
        Properties configuracao = Aplicacao.carregarPropriedades();
        configuracao.setProperty("perfil", "producao");
        Aplicacao app = Aplicacao.montar(configuracao, Clock.systemDefaultZone());
        FabricaDePersistencia fabrica = app.fabrica();

        assertThat(app.perfil()).isEqualTo(PerfilDeExecucao.PRODUCAO);
        assertThat(fabrica).isInstanceOf(FabricaRelacional.class);
        List<Object> produtos = List.of(fabrica.unidadeDeTrabalho(), fabrica.livros(), fabrica.movimentacao(),
                fabrica.relacionamento(), fabrica.multas());
        assertThat(produtos).allSatisfy(produto ->
                assertThat(produto.getClass().getPackageName()).isEqualTo(PACOTE_JDBC));

        // a mesma unidade de trabalho (= a mesma conexão/transação) em todos os repositórios da família
        FabricaRelacional relacional = (FabricaRelacional) fabrica;
        assertThat(relacional.repositoriosCompartilhamAUnidade()).isTrue();
        // montar a aplicação inteira não conectou no banco: a conexão é aberta só no primeiro uso
        assertThat(relacional.conexaoAberta()).isFalse();
    }

    // ---------------------------------------------------------------------------------------------
    // Unidade de trabalho compartilhada
    // ---------------------------------------------------------------------------------------------

    @Test
    void R08_desfazerDescartaTudoQueOsRepositoriosGravaramNaTransacao() {
        FabricaEmMemoria fabrica = new FabricaEmMemoria();
        UnidadeDeTrabalho uow = fabrica.unidadeDeTrabalho();

        uow.iniciar();
        fabrica.livros().salvar(new Livro(1, "Padrões de Projeto"));
        fabrica.relacionamento().salvarPessoa(
                new Pessoa("Ana", "Souza", CPF_ANA, LocalDateTime.of(2026, 11, 25, 9, 0)));
        fabrica.movimentacao().inserir(new Reserva(1, 1, LocalDate.of(2026, 11, 25)));
        uow.desfazer();

        assertThat(fabrica.livros().listarTodos()).isEmpty();
        assertThat(fabrica.relacionamento().listarPessoas()).isEmpty();
        assertThat(fabrica.movimentacao().listarTodas()).isEmpty();
        assertThat(uow.emAndamento()).isFalse();
    }

    @Test
    void R08_confirmarTornaAsGravacoesDefinitivas() {
        FabricaEmMemoria fabrica = new FabricaEmMemoria();
        fabrica.unidadeDeTrabalho().executar(() -> fabrica.livros().salvar(new Livro(1, "Refatoração")));

        assertThat(fabrica.livros().buscarPorId(1)).isPresent();
    }

    @Test
    void R08_falhaDentroDeExecutarDesfazEPropagaAExcecao() {
        FabricaEmMemoria fabrica = new FabricaEmMemoria();

        assertThatThrownBy(() -> fabrica.unidadeDeTrabalho().executar(() -> {
            fabrica.livros().salvar(new Livro(1, "Refatoração"));
            throw new IllegalStateException("falha no meio");
        })).hasMessage("falha no meio");

        assertThat(fabrica.livros().buscarPorId(1)).isEmpty();
    }

    @Test
    void R08_transacaoDentroDeTransacaoRecusada() {
        UnidadeDeTrabalho uow = new FabricaEmMemoria().unidadeDeTrabalho();
        uow.iniciar();
        assertThatThrownBy(uow::iniciar).isInstanceOf(IllegalStateException.class);
        uow.desfazer();
        assertThatThrownBy(uow::confirmar).isInstanceOf(IllegalStateException.class);
    }

    // ---------------------------------------------------------------------------------------------
    // Os repositórios guardam cópias: estado gravado pelo nome e reconstituído (R1)
    // ---------------------------------------------------------------------------------------------

    @Test
    void R08_objetoLidoAlteradoSemSalvarNaoMudaOsDados() {
        FabricaEmMemoria fabrica = new FabricaEmMemoria();
        fabrica.livros().salvar(new Livro(1, "Refatoração"));

        Livro lido = fabrica.livros().buscarPorId(1).orElseThrow();
        lido.reservar();                                       // alterado, mas não salvo

        assertThat(fabrica.livros().buscarPorId(1).orElseThrow().getSituacao()).isEqualTo(SituacaoLivro.LIVRE);
    }

    @Test
    void R08_estadoDoLivroGravadoPeloNomeEReconstituidoNaLeitura() {
        FabricaEmMemoria fabrica = new FabricaEmMemoria();
        Livro livro = new Livro(1, "Refatoração");
        livro.reservar();
        fabrica.livros().salvar(livro);

        Livro relido = fabrica.livros().buscarPorId(1).orElseThrow();
        assertThat(relido).isNotSameAs(livro);
        assertThat(relido.getSituacao()).isEqualTo(SituacaoLivro.RESERVADO);
        assertThatThrownBy(relido::reservar)                    // o comportamento do estado também voltou
                .hasMessageContaining("RESERVADO");
    }

    @Test
    void R08_perfilEscolhidoPelaConfiguracao() {
        assertThat(PerfilDeExecucao.de("teste")).isEqualTo(PerfilDeExecucao.TESTE);
        assertThat(PerfilDeExecucao.de("memoria")).isEqualTo(PerfilDeExecucao.TESTE);
        assertThat(PerfilDeExecucao.de("producao")).isEqualTo(PerfilDeExecucao.PRODUCAO);
        assertThatThrownBy(() -> PerfilDeExecucao.de("oracle")).isInstanceOf(IllegalArgumentException.class);
    }
}
