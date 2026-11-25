package br.pucpr.biblioteca.financeiro.cobranca;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Pedido de cobrança no formato <b>institucional</b> (independente de banco).
 *
 * @param idMulta     identificador da multa na biblioteca (vira o "nosso número"/referência no banco)
 * @param valor       valor em reais, {@link BigDecimal} com 2 casas
 * @param cpfPagador  CPF do estudante, guardado só com os 11 dígitos (pontuação é removida)
 * @param nomePagador nome do estudante
 */
public record SolicitacaoCobranca(long idMulta, BigDecimal valor, String cpfPagador, String nomePagador) {

    public SolicitacaoCobranca {
        Objects.requireNonNull(valor, "valor");
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException("Valor da cobrança deve ser positivo");
        }
        valor = valor.setScale(2, RoundingMode.HALF_EVEN);
        cpfPagador = Objects.requireNonNull(cpfPagador, "cpfPagador").replaceAll("\\D", "");
        if (cpfPagador.length() != 11) {
            throw new IllegalArgumentException("CPF do pagador deve ter 11 dígitos");
        }
        Objects.requireNonNull(nomePagador, "nomePagador");
    }
}
