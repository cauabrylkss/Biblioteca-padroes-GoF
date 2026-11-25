package br.pucpr.biblioteca.financeiro.cobranca;

/** Situação de uma cobrança no vocabulário institucional (cada adaptador traduz os códigos do seu banco). */
public enum SituacaoPagamento {
    EM_ABERTO,
    PAGO,
    CANCELADO
}
