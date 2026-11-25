package br.pucpr.biblioteca.financeiro;

import br.pucpr.biblioteca.compartilhado.DiasDeAtraso;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Papel GoF: <b>Context</b> (Strategy, R2) — serviço de multas.
 *
 * <p>Calcula os dias de atraso, pede à {@link ConfiguracaoAdministrativa} a política vigente e delega a ela o valor.
 * Não há nenhum {@code if} sobre qual política está em uso: uma política nova entra sem alterar esta classe.
 *
 * <p>Nas próximas etapas este serviço também emite o boleto pelo contrato de cobrança (R3) e é chamado pelo ouvinte
 * de multa da devolução (R5).
 */
public class ServicoDeMultas {

    private final ConfiguracaoAdministrativa configuracao;

    public ServicoDeMultas(ConfiguracaoAdministrativa configuracao) {
        this.configuracao = Objects.requireNonNull(configuracao, "configuracao");
    }

    /** Valor que a política vigente cobraria pelos dias informados (2 casas decimais). */
    public BigDecimal calcularValor(long diasDeAtraso) {
        return configuracao.politicaVigente().calcular(diasDeAtraso).setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * Emite a multa de uma devolução, se houver valor a cobrar.
     *
     * @return a multa emitida, ou vazio quando não há atraso (ou a política não cobra esses dias, como na tolerância)
     */
    public Optional<Multa> emitirSeHouverAtraso(long idEmprestimo, long idEstudante,
                                                LocalDate vencimento, LocalDate dataDevolucao) {
        long dias = DiasDeAtraso.entre(vencimento, dataDevolucao);
        PoliticaDeMulta politica = configuracao.politicaVigente();   // lida uma vez: valor e código coerentes
        BigDecimal valor = politica.calcular(dias).setScale(2, RoundingMode.HALF_EVEN);
        if (valor.signum() == 0) {
            return Optional.empty();
        }
        return Optional.of(new Multa(idEmprestimo, idEstudante, dias, valor, politica.codigo(), dataDevolucao));
    }
}
