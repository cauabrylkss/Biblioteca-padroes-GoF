package br.pucpr.biblioteca.financeiro.cobranca;

import br.pucpr.biblioteca.banco.BancoBApi;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Papel GoF: <b>Adapter</b> (Adapter, R3) — adapta a {@link BancoBApi} ao contrato {@link ServicoDeCobranca}.
 *
 * <p>Conversões feitas aqui (e só aqui):
 * <table>
 *   <caption>Institucional ↔ Banco B</caption>
 *   <tr><th>Dado</th><th>Institucional</th><th>Banco B</th></tr>
 *   <tr><td>valor</td><td>{@code BigDecimal 7.00}</td><td>texto {@code "7,00"}</td></tr>
 *   <tr><td>CPF</td><td>11 dígitos</td><td>{@code "123.456.789-09"}</td></tr>
 *   <tr><td>identificador</td><td>id da multa / texto</td><td>referência {@code "MULTA-15"} / id {@code "B-000001"}</td></tr>
 *   <tr><td>situação</td><td>{@link SituacaoPagamento}</td><td>{@code "ABERTO"}, {@code "LIQUIDADO"}, {@code "BAIXADO"}</td></tr>
 * </table>
 */
public class AdaptadorBancoB implements ServicoDeCobranca {

    public static final String CODIGO_BANCO = "B";

    private final BancoBApi banco;

    public AdaptadorBancoB(BancoBApi banco) {
        this.banco = Objects.requireNonNull(banco, "banco");
    }

    @Override
    public CobrancaRegistrada registrar(SolicitacaoCobranca s) {
        String id = banco.criarBoleto("MULTA-" + s.idMulta(), paraTextoComVirgula(s.valor()), formatarCpf(s.cpfPagador()));
        return new CobrancaRegistrada(CODIGO_BANCO, id);
    }

    /** Formato do Banco B: {@code LIQUIDADO|B-000001|7,00|ev-b-1}. */
    @Override
    public NotificacaoPagamento traduzirNotificacao(String notificacaoBruta) {
        String[] partes = Objects.requireNonNull(notificacaoBruta, "notificacaoBruta").split("\\|");
        if (partes.length != 4) {
            throw new IllegalArgumentException("Notificação do Banco B mal formada: " + notificacaoBruta);
        }
        try {
            return new NotificacaoPagamento(
                    partes[3].strip(),
                    partes[1].strip(),
                    deTextoComVirgula(partes[2].strip()),
                    traduzirSituacao(partes[0].strip()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Notificação do Banco B mal formada: " + notificacaoBruta, e);
        }
    }

    @Override
    public String banco() {
        return CODIGO_BANCO;
    }

    // ---- conversões ----

    /** Também usada pelo simulador ao restaurar boletos (D-13): o formato do Banco B fica definido só aqui. */
    public static String paraTextoComVirgula(BigDecimal reais) {
        return reais.setScale(2).toPlainString().replace('.', ',');   // 7.00 → "7,00"
    }

    static BigDecimal deTextoComVirgula(String texto) {
        return new BigDecimal(texto.replace(',', '.')).setScale(2);  // "7,00" → 7.00
    }

    static String formatarCpf(String onzeDigitos) {                   // "12345678909" → "123.456.789-09"
        return onzeDigitos.substring(0, 3) + "." + onzeDigitos.substring(3, 6) + "."
                + onzeDigitos.substring(6, 9) + "-" + onzeDigitos.substring(9);
    }

    static SituacaoPagamento traduzirSituacao(String situacao) {
        return switch (situacao) {
            case BancoBApi.ABERTO -> SituacaoPagamento.EM_ABERTO;
            case BancoBApi.LIQUIDADO -> SituacaoPagamento.PAGO;
            case BancoBApi.BAIXADO -> SituacaoPagamento.CANCELADO;
            default -> throw new IllegalArgumentException("Situação desconhecida do Banco B: " + situacao);
        };
    }
}
