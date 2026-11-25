package br.pucpr.biblioteca.financeiro;

import java.math.BigDecimal;

/** Papel GoF: <b>ConcreteStrategy</b> (Strategy, R2) — política original: R$ 1,00 por dia de atraso. */
public final class PoliticaOriginal implements PoliticaDeMulta {

    public static final String CODIGO = "original";

    @Override
    public BigDecimal calcular(long diasDeAtraso) {
        PoliticaDeMulta.exigirDiasNaoNegativos(diasDeAtraso);
        return UM_REAL_POR_DIA.multiply(BigDecimal.valueOf(diasDeAtraso));
    }

    @Override
    public String codigo() {
        return CODIGO;
    }

    @Override
    public String descricao() {
        return "R$ 1,00 por dia de atraso";
    }
}
