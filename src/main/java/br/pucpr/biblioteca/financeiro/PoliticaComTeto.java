package br.pucpr.biblioteca.financeiro;

import java.math.BigDecimal;

/**
 * Papel GoF: <b>ConcreteStrategy</b> (Strategy, R2) — R$ 1,00 por dia, limitado a um teto de R$ 20,00.
 * Ex.: 5 dias → R$ 5,00; 25 dias → R$ 20,00.
 */
public final class PoliticaComTeto implements PoliticaDeMulta {

    public static final String CODIGO = "teto";
    public static final BigDecimal TETO = new BigDecimal("20.00");

    @Override
    public BigDecimal calcular(long diasDeAtraso) {
        PoliticaDeMulta.exigirDiasNaoNegativos(diasDeAtraso);
        BigDecimal valor = UM_REAL_POR_DIA.multiply(BigDecimal.valueOf(diasDeAtraso));
        return valor.min(TETO);
    }

    @Override
    public String codigo() {
        return CODIGO;
    }

    @Override
    public String descricao() {
        return "R$ 1,00 por dia, no máximo R$ 20,00";
    }
}
