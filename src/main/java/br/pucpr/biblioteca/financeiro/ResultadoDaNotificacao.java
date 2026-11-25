package br.pucpr.biblioteca.financeiro;

/** O que o serviço de multas fez com uma notificação de pagamento recebida do banco. */
public enum ResultadoDaNotificacao {
    /** Primeira notificação de pagamento: boleto PAGO e multa QUITADA. */
    MULTA_QUITADA,
    /** Mesma notificação recebida de novo, ou multa já quitada: nada foi alterado. */
    IGNORADA_JA_PROCESSADA,
    /** A notificação não informa pagamento (em aberto/cancelado): nada foi alterado. */
    IGNORADA_SEM_PAGAMENTO
}
