package br.pucpr.biblioteca.financeiro;

/** Situação do boleto, como no diagrama de atividades: {@code Boleto [EMITIDO]} → {@code Boleto [PAGO]}. */
public enum SituacaoBoleto {
    EMITIDO,
    PAGO
}
