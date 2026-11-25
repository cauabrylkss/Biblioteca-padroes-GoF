package br.pucpr.biblioteca.financeiro;

import java.math.BigDecimal;

/**
 * Papel GoF: <b>ConcreteStrategy</b> (Strategy, R2) — tolerância de 2 dias: os 2 primeiros dias de atraso não são
 * cobrados; a partir do 3º, R$ 1,00 por dia. Ex.: 2 dias → R$ 0,00; 5 dias → R$ 3,00 (decisão B-08).
 */
public final class PoliticaComTolerancia implements PoliticaDeMulta {

    public static final String CODIGO = "tolerancia";
    public static final long DIAS_DE_TOLERANCIA = 2;

    @Override
    public BigDecimal calcular(long diasDeAtraso) {
        PoliticaDeMulta.exigirDiasNaoNegativos(diasDeAtraso);
        long diasCobrados = Math.max(0, diasDeAtraso - DIAS_DE_TOLERANCIA);
        return UM_REAL_POR_DIA.multiply(BigDecimal.valueOf(diasCobrados));
    }

    @Override
    public String codigo() {
        return CODIGO;
    }

    @Override
    public String descricao() {
        return "Tolerância de 2 dias; depois R$ 1,00 por dia";
    }
}
