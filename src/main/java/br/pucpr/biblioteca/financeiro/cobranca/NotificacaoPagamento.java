package br.pucpr.biblioteca.financeiro.cobranca;

import java.math.BigDecimal;

/**
 * Notificação de pagamento já traduzida para o formato institucional.
 *
 * @param idNotificacao        identificador do evento no banco — usado para reconhecer notificações repetidas
 * @param identificadorNoBanco título/boleto a que se refere (mesmo texto de {@link CobrancaRegistrada})
 * @param valorPago            valor em reais, 2 casas
 * @param situacao             situação traduzida
 */
public record NotificacaoPagamento(String idNotificacao, String identificadorNoBanco, BigDecimal valorPago,
                                   SituacaoPagamento situacao) {
}
