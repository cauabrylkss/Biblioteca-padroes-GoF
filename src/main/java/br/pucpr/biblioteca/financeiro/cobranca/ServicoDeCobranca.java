package br.pucpr.biblioteca.financeiro.cobranca;

/**
 * Papel GoF: <b>Target</b> (Adapter, R3) — contrato institucional de cobrança (iCobranca no diagrama de componentes).
 *
 * <p>É a única forma de o Financeiro falar com um banco. Cada banco tem um adaptador que implementa este contrato e
 * faz todas as conversões de valores, documentos, identificadores e situações — nos dois sentidos: no registro da
 * cobrança e na tradução das notificações de pagamento.
 */
public interface ServicoDeCobranca {

    /** Registra a cobrança no banco da instalação. */
    CobrancaRegistrada registrar(SolicitacaoCobranca solicitacao);

    /**
     * Traduz a notificação de pagamento recebida do banco (no formato bruto dele) para o formato institucional.
     *
     * @throws IllegalArgumentException se o conteúdo não estiver no formato do banco
     */
    NotificacaoPagamento traduzirNotificacao(String notificacaoBruta);

    /** Código do banco adaptado ("A" ou "B"). */
    String banco();
}
