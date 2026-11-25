package br.pucpr.biblioteca.financeiro.cobranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucpr.biblioteca.banco.BancoAApi;
import br.pucpr.biblioteca.banco.BancoBApi;
import br.pucpr.biblioteca.config.BancoDaInstalacao;
import br.pucpr.biblioteca.config.BancoDaInstalacao.BancoConfigurado;
import br.pucpr.biblioteca.financeiro.ConfiguracaoAdministrativa;
import br.pucpr.biblioteca.financeiro.ConsultaDePagador;
import br.pucpr.biblioteca.financeiro.EstadoMulta;
import br.pucpr.biblioteca.financeiro.Multa;
import br.pucpr.biblioteca.financeiro.PoliticaOriginal;
import br.pucpr.biblioteca.financeiro.ResultadoDaNotificacao;
import br.pucpr.biblioteca.financeiro.ServicoDeMultas;
import br.pucpr.biblioteca.financeiro.SituacaoBoleto;
import br.pucpr.biblioteca.persistencia.memoria.RepositorioDeMultasEmMemoria;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** R3 — Adapter: integração com bancos incompatíveis. */
class R03AdapterTest {

    private static final long ID_EMPRESTIMO = 100;
    private static final long ESTUDANTE = 1;
    private static final LocalDate VENCIMENTO = LocalDate.of(2026, 11, 20);
    private static final LocalDate SETE_DIAS_DEPOIS = VENCIMENTO.plusDays(7);   // R$ 7,00 na política original
    private static final ConsultaDePagador ANA = id -> new ConsultaDePagador.Pagador("Ana Souza", "123.456.789-09");

    /** Monta o serviço de multas exatamente igual para qualquer banco: só o adaptador muda. */
    private static ServicoDeMultas servicoCom(ServicoDeCobranca cobranca) {
        ConfiguracaoAdministrativa configuracao =
                new ConfiguracaoAdministrativa(List.of(new PoliticaOriginal()), PoliticaOriginal.CODIGO);
        return new ServicoDeMultas(configuracao, new RepositorioDeMultasEmMemoria(), cobranca, ANA);
    }

    // ---------------------------------------------------------------------------------------------
    // Cenários de verificação do enunciado
    // ---------------------------------------------------------------------------------------------

    @Test
    void R03_seteReaisViram700CentavosNoBancoAe7NoBancoB() {
        BancoAApi bancoA = new BancoAApi();
        BancoBApi bancoB = new BancoBApi();
        ServicoDeMultas comBancoA = servicoCom(new AdaptadorBancoA(bancoA));
        ServicoDeMultas comBancoB = servicoCom(new AdaptadorBancoB(bancoB));

        Multa multaA = comBancoA.emitirSeHouverAtraso(ID_EMPRESTIMO, ESTUDANTE, VENCIMENTO, SETE_DIAS_DEPOIS).orElseThrow();
        Multa multaB = comBancoB.emitirSeHouverAtraso(ID_EMPRESTIMO, ESTUDANTE, VENCIMENTO, SETE_DIAS_DEPOIS).orElseThrow();

        // mesma solicitação institucional...
        assertThat(multaA.getValor()).isEqualTo(new BigDecimal("7.00"));
        assertThat(multaB.getValor()).isEqualTo(new BigDecimal("7.00"));

        // ...chega ao Banco A em centavos, CPF só com dígitos, nosso número = id da multa
        BancoAApi.Titulo titulo = bancoA.ultimoTituloRegistrado();
        assertThat(titulo.valorEmCentavos()).isEqualTo(700L);
        assertThat(titulo.cpfSomenteDigitos()).isEqualTo("12345678909");
        assertThat(titulo.nossoNumero()).isEqualTo(multaA.getId());

        // ...e ao Banco B como "7,00", CPF formatado, referência textual
        BancoBApi.BoletoB boletoB = bancoB.ultimoBoletoCriado();
        assertThat(boletoB.valorReais()).isEqualTo("7,00");
        assertThat(boletoB.documentoPagador()).isEqualTo("123.456.789-09");
        assertThat(boletoB.referencia()).isEqualTo("MULTA-" + multaB.getId());

        // identificadores devolvidos pelos bancos, guardados no formato institucional (texto)
        assertThat(multaA.getBoleto()).get().satisfies(b -> {
            assertThat(b.getBanco()).isEqualTo("A");
            assertThat(b.getIdentificadorNoBanco()).isEqualTo(Long.toString(titulo.codigo()));
            assertThat(b.getSituacao()).isEqualTo(SituacaoBoleto.EMITIDO);
        });
        assertThat(multaB.getBoleto()).get().satisfies(b -> {
            assertThat(b.getBanco()).isEqualTo("B");
            assertThat(b.getIdentificadorNoBanco()).isEqualTo(boletoB.id());
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"A", "B"})
    void R03_notificacaoRepetidaNaoQuitaDuasVezes(String banco) {
        BancoConfigurado instalacao = BancoDaInstalacao.criar(banco);
        ServicoDeMultas servico = servicoCom(instalacao.cobranca());
        Multa multa = servico.emitirSeHouverAtraso(ID_EMPRESTIMO, ESTUDANTE, VENCIMENTO, SETE_DIAS_DEPOIS).orElseThrow();
        String idBoleto = multa.getBoleto().orElseThrow().getIdentificadorNoBanco();

        String notificacao = instalacao.simulador().pagar(idBoleto);

        assertThat(servico.registrarPagamento(notificacao)).isEqualTo(ResultadoDaNotificacao.MULTA_QUITADA);
        assertThat(servico.registrarPagamento(notificacao))
                .as("mesma notificação entregue de novo").isEqualTo(ResultadoDaNotificacao.IGNORADA_JA_PROCESSADA);
        String outroEventoDoMesmoBoleto = instalacao.simulador().pagar(idBoleto);
        assertThat(servico.registrarPagamento(outroEventoDoMesmoBoleto))
                .as("evento novo sobre multa já quitada").isEqualTo(ResultadoDaNotificacao.IGNORADA_JA_PROCESSADA);

        assertThat(multa.getEstado()).isEqualTo(EstadoMulta.QUITADA);
        assertThat(multa.getBoleto().orElseThrow().getSituacao()).isEqualTo(SituacaoBoleto.PAGO);
    }

    // ---------------------------------------------------------------------------------------------
    // Apoio à arguição
    // ---------------------------------------------------------------------------------------------

    @Test
    void R03_notificacoesDeCadaBancoSaoTraduzidasPeloSeuAdaptador() {
        ServicoDeCobranca a = new AdaptadorBancoA(new BancoAApi());
        ServicoDeCobranca b = new AdaptadorBancoB(new BancoBApi());

        assertThat(a.traduzirNotificacao("evento=EVT-A-9;titulo=1001;valorCentavos=700;status=2"))
                .isEqualTo(new NotificacaoPagamento("EVT-A-9", "1001", new BigDecimal("7.00"), SituacaoPagamento.PAGO));
        assertThat(a.traduzirNotificacao("evento=EVT-A-10;titulo=1001;valorCentavos=700;status=1").situacao())
                .isEqualTo(SituacaoPagamento.EM_ABERTO);
        assertThat(b.traduzirNotificacao("LIQUIDADO|B-000001|7,00|ev-b-9"))
                .isEqualTo(new NotificacaoPagamento("ev-b-9", "B-000001", new BigDecimal("7.00"), SituacaoPagamento.PAGO));
        assertThat(b.traduzirNotificacao("BAIXADO|B-000001|7,00|ev-b-10").situacao())
                .isEqualTo(SituacaoPagamento.CANCELADO);

        assertThatThrownBy(() -> a.traduzirNotificacao("LIQUIDADO|B-000001|7,00|ev-b-9"))
                .as("formato do Banco B não é aceito pelo adaptador do A").isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> b.traduzirNotificacao("evento=EVT-A-9;titulo=1001;valorCentavos=700;status=2"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void R03_notificacaoSemPagamentoNaoQuita() {
        BancoBApi bancoB = new BancoBApi();
        ServicoDeMultas servico = servicoCom(new AdaptadorBancoB(bancoB));
        Multa multa = servico.emitirSeHouverAtraso(ID_EMPRESTIMO, ESTUDANTE, VENCIMENTO, SETE_DIAS_DEPOIS).orElseThrow();
        String id = multa.getBoleto().orElseThrow().getIdentificadorNoBanco();

        ResultadoDaNotificacao resultado = servico.registrarPagamento("ABERTO|" + id + "|7,00|ev-b-77");

        assertThat(resultado).isEqualTo(ResultadoDaNotificacao.IGNORADA_SEM_PAGAMENTO);
        assertThat(multa.getEstado()).isEqualTo(EstadoMulta.EMITIDA);
    }

    @Test
    void R03_notificacoesSimultaneasQuitamUmaVezSo() throws Exception {
        BancoConfigurado instalacao = BancoDaInstalacao.criar("A");
        ServicoDeMultas servico = servicoCom(instalacao.cobranca());
        Multa multa = servico.emitirSeHouverAtraso(ID_EMPRESTIMO, ESTUDANTE, VENCIMENTO, SETE_DIAS_DEPOIS).orElseThrow();
        String notificacao = instalacao.simulador().pagar(multa.getBoleto().orElseThrow().getIdentificadorNoBanco());

        ExecutorService threads = Executors.newFixedThreadPool(8);
        try {
            List<Callable<ResultadoDaNotificacao>> tarefas = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                tarefas.add(() -> servico.registrarPagamento(notificacao));
            }
            List<ResultadoDaNotificacao> resultados = new ArrayList<>();
            for (Future<ResultadoDaNotificacao> f : threads.invokeAll(tarefas)) {
                resultados.add(f.get());
            }
            assertThat(resultados).filteredOn(r -> r == ResultadoDaNotificacao.MULTA_QUITADA).hasSize(1);
        } finally {
            threads.shutdownNow();
        }
    }

    @Test
    void R03_bancoDaInstalacaoEscolhidoPelaConfiguracao() {
        assertThat(BancoDaInstalacao.criar("A").cobranca()).isInstanceOf(AdaptadorBancoA.class);
        assertThat(BancoDaInstalacao.criar("b").cobranca()).isInstanceOf(AdaptadorBancoB.class);
        assertThatThrownBy(() -> BancoDaInstalacao.criar("C")).isInstanceOf(IllegalArgumentException.class);
    }

    /** O serviço de multas só conhece o contrato: nenhum campo ou parâmetro de construtor é de banco ou adaptador. */
    @Test
    void R03_servicoDeMultasNaoDependeDeNenhumBanco() {
        Stream<Class<?>> tiposDeCampos = Arrays.stream(ServicoDeMultas.class.getDeclaredFields()).map(Field::getType);
        Stream<Class<?>> tiposDeParametros = Arrays.stream(ServicoDeMultas.class.getConstructors())
                .flatMap(c -> Arrays.stream(c.getParameters())).map(Parameter::getType);

        assertThat(Stream.concat(tiposDeCampos, tiposDeParametros).toList())
                .contains(ServicoDeCobranca.class)
                .noneMatch(t -> t.getPackageName().equals("br.pucpr.biblioteca.banco"))
                .noneMatch(t -> t == AdaptadorBancoA.class || t == AdaptadorBancoB.class);
    }
}
