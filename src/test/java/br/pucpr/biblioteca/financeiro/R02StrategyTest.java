package br.pucpr.biblioteca.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** R2 — Strategy: políticas de cálculo de multa. */
class R02StrategyTest {

    private static final long ID_EMPRESTIMO = 100;
    private static final long ESTUDANTE = 1;
    private static final LocalDate VENCIMENTO = LocalDate.of(2026, 11, 20);
    private static final LocalDate CINCO_DIAS_DEPOIS = VENCIMENTO.plusDays(5);

    private final ConfiguracaoAdministrativa configuracao = new ConfiguracaoAdministrativa(
            List.of(new PoliticaOriginal(), new PoliticaComTeto(), new PoliticaComTolerancia()),
            PoliticaOriginal.CODIGO);
    private final ServicoDeMultas servico = new ServicoDeMultas(configuracao);

    // ---------------------------------------------------------------------------------------------
    // Cenários de verificação do enunciado
    // ---------------------------------------------------------------------------------------------

    @Test
    void R02_cincoDiasAtrasoPoliticaOriginalCincoReais() {
        configuracao.definirPoliticaVigente(PoliticaOriginal.CODIGO);

        Multa multa = servico.emitirSeHouverAtraso(ID_EMPRESTIMO, ESTUDANTE, VENCIMENTO, CINCO_DIAS_DEPOIS).orElseThrow();

        assertThat(multa.getDiasDeAtraso()).isEqualTo(5);
        assertThat(multa.getValor()).isEqualTo(new BigDecimal("5.00"));
    }

    @Test
    void R02_cincoDiasAtrasoPoliticaToleranciaTresReais() {
        configuracao.definirPoliticaVigente(PoliticaComTolerancia.CODIGO);

        Multa multa = servico.emitirSeHouverAtraso(ID_EMPRESTIMO, ESTUDANTE, VENCIMENTO, CINCO_DIAS_DEPOIS).orElseThrow();

        assertThat(multa.getValor()).isEqualTo(new BigDecimal("3.00"));
    }

    @Test
    void R02_semAtrasoMultaZero() {
        for (PoliticaDeMulta politica : configuracao.politicasDisponiveis()) {
            configuracao.definirPoliticaVigente(politica.codigo());

            assertThat(servico.calcularValor(0)).as(politica.codigo()).isEqualTo(new BigDecimal("0.00"));
            assertThat(servico.emitirSeHouverAtraso(ID_EMPRESTIMO, ESTUDANTE, VENCIMENTO, VENCIMENTO))
                    .as("devolução no dia do vencimento, %s", politica.codigo()).isEmpty();
            assertThat(servico.emitirSeHouverAtraso(ID_EMPRESTIMO, ESTUDANTE, VENCIMENTO, VENCIMENTO.minusDays(3)))
                    .as("devolução antecipada, %s", politica.codigo()).isEmpty();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Apoio à arguição
    // ---------------------------------------------------------------------------------------------

    @Test
    void R02_politicaComTetoNuncaPassaDeVinteReais() {
        configuracao.definirPoliticaVigente(PoliticaComTeto.CODIGO);

        assertThat(servico.calcularValor(5)).isEqualTo(new BigDecimal("5.00"));
        assertThat(servico.calcularValor(20)).isEqualTo(new BigDecimal("20.00"));
        assertThat(servico.calcularValor(45)).isEqualTo(new BigDecimal("20.00"));
    }

    @Test
    void R02_multaGravaValorEPoliticaDaEmissaoETrocaDePoliticaNaoRecalcula() {
        configuracao.definirPoliticaVigente(PoliticaOriginal.CODIGO);
        Multa emitida = servico.emitirSeHouverAtraso(ID_EMPRESTIMO, ESTUDANTE, VENCIMENTO, CINCO_DIAS_DEPOIS).orElseThrow();

        configuracao.definirPoliticaVigente(PoliticaComTolerancia.CODIGO);

        assertThat(emitida.getValor()).isEqualTo(new BigDecimal("5.00"));
        assertThat(emitida.getCodigoPolitica()).isEqualTo(PoliticaOriginal.CODIGO);
        assertThat(emitida.getEstado()).isEqualTo(EstadoMulta.EMITIDA);
        // a próxima multa já usa a política nova
        Multa proxima = servico.emitirSeHouverAtraso(ID_EMPRESTIMO + 1, ESTUDANTE, VENCIMENTO, CINCO_DIAS_DEPOIS)
                .orElseThrow();
        assertThat(proxima.getValor()).isEqualTo(new BigDecimal("3.00"));
        assertThat(proxima.getCodigoPolitica()).isEqualTo(PoliticaComTolerancia.CODIGO);
    }

    /** Simula a "alteração ao vivo": uma política nova entra só com uma classe nova + registro na configuração. */
    @Test
    void R02_novaPoliticaEntraSemAlterarOServicoDeMultas() {
        PoliticaDeMulta dobroAposDezDias = new PoliticaDeMulta() {
            @Override
            public BigDecimal calcular(long dias) {
                long normais = Math.min(dias, 10);
                long dobrados = Math.max(0, dias - 10);
                return UM_REAL_POR_DIA.multiply(BigDecimal.valueOf(normais + 2 * dobrados));
            }

            @Override
            public String codigo() {
                return "dobro-apos-10";
            }

            @Override
            public String descricao() {
                return "R$ 1,00 por dia até 10 dias; R$ 2,00 por dia depois";
            }
        };
        configuracao.registrar(dobroAposDezDias);
        configuracao.definirPoliticaVigente("dobro-apos-10");

        Optional<Multa> multa = servico.emitirSeHouverAtraso(ID_EMPRESTIMO, ESTUDANTE, VENCIMENTO, VENCIMENTO.plusDays(12));

        assertThat(multa).get().extracting(Multa::getValor).isEqualTo(new BigDecimal("14.00"));
        assertThat(multa).get().extracting(Multa::getCodigoPolitica).isEqualTo("dobro-apos-10");
    }

    @Test
    void R02_politicaDesconhecidaRecusadaEVigenteNaoMuda() {
        configuracao.definirPoliticaVigente(PoliticaComTeto.CODIGO);

        assertThatThrownBy(() -> configuracao.definirPoliticaVigente("inexistente"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("inexistente");
        assertThat(configuracao.politicaVigente().codigo()).isEqualTo(PoliticaComTeto.CODIGO);
    }
}
