package br.pucpr.biblioteca.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteDevolucao;
import br.pucpr.biblioteca.biblioteca.circulacao.ComprovanteEmprestimo;
import br.pucpr.biblioteca.biblioteca.circulacao.SolicitacaoDeRenovacao;
import br.pucpr.biblioteca.biblioteca.circulacao.SolicitacaoDeReserva;
import br.pucpr.biblioteca.biblioteca.livro.Livro;
import br.pucpr.biblioteca.biblioteca.livro.SituacaoLivro;
import br.pucpr.biblioteca.biblioteca.movimentacao.Emprestimo;
import br.pucpr.biblioteca.biblioteca.movimentacao.Reserva;
import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;
import br.pucpr.biblioteca.compartilhado.RelogioAjustavel;
import br.pucpr.biblioteca.config.Aplicacao;
import br.pucpr.biblioteca.financeiro.EstadoMulta;
import br.pucpr.biblioteca.financeiro.Multa;
import br.pucpr.biblioteca.financeiro.PoliticaOriginal;
import br.pucpr.biblioteca.financeiro.ResultadoDaNotificacao;
import br.pucpr.biblioteca.financeiro.SituacaoBoleto;
import br.pucpr.biblioteca.persistencia.jdbc.FabricaRelacional;
import br.pucpr.biblioteca.rh.Pessoa;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * R8 com o <b>MySQL real</b> (D-08). Fora do {@code mvnw test} padrão; rodar com {@code mvnw test -Pmysql}.
 *
 * <p>Precisa do banco criado por {@code docs/mysql/criar-banco.sql} e das variáveis {@code BIBLIOTECA_DB_*}.
 * <b>Atenção:</b> apaga os dados do banco {@code biblioteca} antes de cada teste (use {@code --reiniciar-dados} na CLI
 * depois para recriar os dados de exemplo).
 *
 * <p>"Reiniciar a aplicação" = montar uma {@link Aplicacao} nova, com conexão nova: o que ela enxerga veio do banco.
 */
@Tag("mysql")
class R08MySqlIntegracaoTest {

    private static final String CPF_ANA = "123.456.789-09";
    private static final String CPF_BRUNO = "111.444.777-35";
    private static final String CPF_CARLA = "529.982.247-25";
    private static final long LIVRO = 10;

    private final RelogioAjustavel relogio = RelogioAjustavel.em(LocalDate.of(2026, 11, 25));
    private Aplicacao app;
    private long ana;
    private long carla;
    private long bruno;

    @BeforeEach
    void bancoLimpoComCadastroMinimo() {
        app = novaAplicacao();
        ((FabricaRelacional) app.fabrica()).apagarTodosOsDados();

        app.tratadorDePessoa().inserirPessoa("Ana", "Souza", CPF_ANA);
        app.tratadorDePessoa().inserirPessoa("Bruno", "Lima", CPF_BRUNO);
        app.tratadorDePessoa().inserirPessoa("Carla", "Dias", CPF_CARLA);
        ana = app.tratadorDeEstudante().inserirEstudante(CPF_ANA).cadastro().id();
        carla = app.tratadorDeEstudante().inserirEstudante(CPF_CARLA).cadastro().id();
        bruno = app.tratadorDeAtendente().inserirAtendente(CPF_BRUNO).cadastro().id();
        app.tratadorDeAcervo().inserirLivro(LIVRO, "Padrões de Projeto", null);
    }

    @Test
    void R08_mysqlFluxoCompletoGravadoESobreviveAoReinicio() {
        app.tratadorDeEmprestimo().solicitarReserva(SolicitacaoDeReserva.nova("ana", relogio, LIVRO, ana));
        ComprovanteEmprestimo emprestimo = app.tratadorDeEmprestimo().realizarEmprestimo(LIVRO, ana, bruno);
        app.tratadorDeEmprestimo().renovarEmprestimo(SolicitacaoDeRenovacao.nova("ana", relogio, LIVRO, ana));

        // renovação gravada: +30 dias e numRenovacoes = 1
        Emprestimo gravado = novaAplicacao().fabrica().movimentacao().buscarEmprestimoAtivo(LIVRO).orElseThrow();
        assertThat(gravado.getNumRenovacoes()).isEqualTo(1);
        assertThat(gravado.getVencimento()).isEqualTo(emprestimo.vencimento().plusDays(30));

        relogio.avancarDias(65);                          // vencimento em +60 → 5 dias de atraso
        ComprovanteDevolucao devolucao = app.tratadorDeDevolucao().registrarDevolucao(LIVRO, ana, bruno);
        assertThat(devolucao.diasDeAtraso()).isEqualTo(5);

        // pagamento no banco simulado (que vive só nesta execução) → quitação gravada; repetição ignorada
        Multa multa = app.servicoDeMultas().multaDoEmprestimo(emprestimo.idEmprestimo()).orElseThrow();
        String notificacao = app.simuladorDePagamento().pagar(multa.getBoleto().orElseThrow().getIdentificadorNoBanco());
        assertThat(app.servicoDeMultas().registrarPagamento(notificacao)).isEqualTo(ResultadoDaNotificacao.MULTA_QUITADA);
        assertThat(app.servicoDeMultas().registrarPagamento(notificacao))
                .isEqualTo(ResultadoDaNotificacao.IGNORADA_JA_PROCESSADA);

        // "reinicia" a aplicação: tudo vem do MySQL
        Aplicacao reiniciada = novaAplicacao();
        FabricaDePersistencia fabrica = reiniciada.fabrica();
        assertThat(fabrica.livros().buscarPorId(LIVRO).orElseThrow().getSituacao()).isEqualTo(SituacaoLivro.DEVOLVIDO);
        assertThat(fabrica.movimentacao().buscarEmprestimoAtivo(LIVRO)).isEmpty();
        assertThat(fabrica.movimentacao().buscarUltimaDevolucao(LIVRO, ana)).isPresent();

        Multa relida = reiniciada.servicoDeMultas().multaDoEmprestimo(emprestimo.idEmprestimo()).orElseThrow();
        assertThat(relida.getValor()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(relida.getCodigoPolitica()).isEqualTo(PoliticaOriginal.CODIGO);
        assertThat(relida.getDiasDeAtraso()).isEqualTo(5);
        assertThat(relida.getEstado()).isEqualTo(EstadoMulta.QUITADA);
        assertThat(relida.getBoleto().orElseThrow().getSituacao()).isEqualTo(SituacaoBoleto.PAGO);

        // a notificação processada também ficou no banco: reenviada à aplicação reiniciada, não quita de novo
        assertThat(reiniciada.servicoDeMultas().registrarPagamento(notificacao))
                .isEqualTo(ResultadoDaNotificacao.IGNORADA_JA_PROCESSADA);

        // a inspeção (comando do atendente) continua de onde parou, na aplicação reiniciada
        reiniciada.tratadorDeDevolucao().inspecionarLivro(LIVRO, true);
        assertThat(novaAplicacao().fabrica().livros().buscarPorId(LIVRO).orElseThrow().getSituacao())
                .isEqualTo(SituacaoLivro.LIVRE);
    }

    @Test
    void R08_mysqlEmprestimoRecusadoNaoAlteraOBanco() {
        app.tratadorDeEmprestimo().solicitarReserva(SolicitacaoDeReserva.nova("ana", relogio, LIVRO, ana));

        // reserva é da Ana: emprestar para a Carla é recusado e nada muda no banco
        assertThatThrownBy(() -> app.tratadorDeEmprestimo().realizarEmprestimo(LIVRO, carla, bruno))
                .isInstanceOf(RegraDeNegocioException.class);

        FabricaDePersistencia fabrica = novaAplicacao().fabrica();
        assertThat(fabrica.livros().buscarPorId(LIVRO).orElseThrow().getSituacao()).isEqualTo(SituacaoLivro.RESERVADO);
        assertThat(fabrica.movimentacao().buscarReservaDoLivro(LIVRO)).isPresent();
        assertThat(fabrica.movimentacao().buscarEmprestimoAtivo(LIVRO)).isEmpty();
    }

    @Test
    void R08_mysqlDesfazerEhRollbackDeTodosOsRepositorios() {
        FabricaDePersistencia fabrica = app.fabrica();
        UnidadeDeTrabalho uow = fabrica.unidadeDeTrabalho();

        uow.iniciar();
        fabrica.livros().salvar(new Livro(99, "Refatoração"));
        fabrica.relacionamento().salvarPessoa(
                new Pessoa("Davi", "Rocha", "39053344705", LocalDateTime.of(2026, 11, 25, 9, 0)));
        fabrica.movimentacao().inserir(new Reserva(99, ana, LocalDate.of(2026, 11, 25)));
        uow.desfazer();

        FabricaDePersistencia outraConexao = novaAplicacao().fabrica();
        assertThat(outraConexao.livros().buscarPorId(99)).isEmpty();
        assertThat(outraConexao.relacionamento().buscarPessoaPorCpf("39053344705")).isEmpty();
        assertThat(outraConexao.movimentacao().listarTodas()).isEmpty();
        assertThat(uow.emAndamento()).isFalse();
    }

    @Test
    void R08_mysqlEstadoGravadoPeloNomeEReconstituido() {
        Livro livro = app.fabrica().livros().buscarPorId(LIVRO).orElseThrow();
        livro.reservar();
        app.fabrica().livros().salvar(livro);

        Livro relido = novaAplicacao().fabrica().livros().buscarPorId(LIVRO).orElseThrow();
        assertThat(relido.getSituacao()).isEqualTo(SituacaoLivro.RESERVADO);
        assertThatThrownBy(relido::reservar).hasMessageContaining("RESERVADO");   // comportamento do estado voltou
    }

    /**
     * D-13, como a CLI/web sobem: depois de reiniciar sem apagar os dados, (1) um pagamento novo não é tomado por
     * notificação repetida — o evento EVT-A-1 da execução anterior já está em {@code notificacao_processada} — e
     * (2) o boleto emitido antes do reinício, ainda em aberto, continua pagável.
     */
    @Test
    void R08_mysqlBancoSimuladoRetomaBoletosEEventosAposReinicio() {
        long outroLivro = LIVRO + 1;
        app.tratadorDeAcervo().inserirLivro(outroLivro, "Refatoração", null);
        app.tratadorDeEmprestimo().solicitarReserva(SolicitacaoDeReserva.nova("ana", relogio, LIVRO, ana));
        app.tratadorDeEmprestimo().solicitarReserva(SolicitacaoDeReserva.nova("ana", relogio, outroLivro, ana));
        long emprestimoPago = app.tratadorDeEmprestimo().realizarEmprestimo(LIVRO, ana, bruno).idEmprestimo();
        long emprestimoEmAberto = app.tratadorDeEmprestimo().realizarEmprestimo(outroLivro, ana, bruno).idEmprestimo();
        relogio.avancarDias(35);                                         // 5 dias de atraso nos dois
        app.tratadorDeDevolucao().registrarDevolucao(LIVRO, ana, bruno);
        app.tratadorDeDevolucao().registrarDevolucao(outroLivro, ana, bruno);
        Multa paga = app.servicoDeMultas().multaDoEmprestimo(emprestimoPago).orElseThrow();
        assertThat(app.servicoDeMultas().registrarPagamento(
                app.simuladorDePagamento().pagar(paga.getBoleto().orElseThrow().getIdentificadorNoBanco())))
                .isEqualTo(ResultadoDaNotificacao.MULTA_QUITADA);              // grava o evento EVT-A-1

        Aplicacao reiniciada = novaAplicacao();
        reiniciada.prepararParaUso(false);                               // como o Main faz ao subir
        Multa emAberto = reiniciada.servicoDeMultas().multaDoEmprestimo(emprestimoEmAberto).orElseThrow();
        String notificacao = reiniciada.simuladorDePagamento()
                .pagar(emAberto.getBoleto().orElseThrow().getIdentificadorNoBanco());

        assertThat(notificacao).doesNotContain("EVT-A-1;");
        assertThat(reiniciada.servicoDeMultas().registrarPagamento(notificacao))
                .isEqualTo(ResultadoDaNotificacao.MULTA_QUITADA);
        Multa relida = novaAplicacao().servicoDeMultas().multaDoEmprestimo(emprestimoEmAberto).orElseThrow();
        assertThat(relida.getEstado()).isEqualTo(EstadoMulta.QUITADA);
        assertThat(relida.getBoleto().orElseThrow().getSituacao()).isEqualTo(SituacaoBoleto.PAGO);
    }

    // ---------------------------------------------------------------------------------------------

    private Aplicacao novaAplicacao() {
        Properties props = Aplicacao.carregarPropriedades();
        props.setProperty("perfil", "producao");
        return Aplicacao.montar(props, relogio);
    }
}
