package br.pucpr.biblioteca.financeiro;

import java.math.BigDecimal;

/**
 * Papel GoF: <b>Strategy</b> (R2) — regra de cálculo do valor da multa.
 *
 * <p>Cada política é uma classe. O {@link ServicoDeMultas} não sabe qual está em uso: pede a vigente à
 * {@link ConfiguracaoAdministrativa} e chama {@link #calcular(long)}. Para criar uma política nova basta escrever
 * outra implementação e registrá-la na configuração — o serviço de multas não muda.
 *
 * <p>Valores monetários são {@link BigDecimal} com 2 casas decimais (nunca {@code float}/{@code double}).
 */
public interface PoliticaDeMulta {

    /** Valor cobrado por dia de atraso nas políticas do enunciado: R$ 1,00. */
    BigDecimal UM_REAL_POR_DIA = new BigDecimal("1.00");

    /**
     * @param diasDeAtraso dias corridos de atraso (zero ou mais)
     * @return valor da multa com 2 casas decimais; zero quando não há o que cobrar
     */
    BigDecimal calcular(long diasDeAtraso);

    /** Identificador gravado na multa e usado na configuração ({@code multa.politica}). */
    String codigo();

    /** Texto curto para a interface do administrador. */
    String descricao();

    /** Validação comum às políticas: dias de atraso nunca são negativos. */
    static void exigirDiasNaoNegativos(long diasDeAtraso) {
        if (diasDeAtraso < 0) {
            throw new IllegalArgumentException("Dias de atraso não podem ser negativos: " + diasDeAtraso);
        }
    }
}
