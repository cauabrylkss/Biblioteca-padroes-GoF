package br.pucpr.biblioteca.financeiro.cobranca;

import br.pucpr.biblioteca.banco.BancoAApi;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Papel GoF: <b>Adapter</b> (Adapter, R3) — adapta a {@link BancoAApi} ao contrato {@link ServicoDeCobranca}.
 *
 * <p>Conversões feitas aqui (e só aqui):
 * <table>
 *   <caption>Institucional ↔ Banco A</caption>
 *   <tr><th>Dado</th><th>Institucional</th><th>Banco A</th></tr>
 *   <tr><td>valor</td><td>{@code BigDecimal 7.00}</td><td>{@code long 700} (centavos)</td></tr>
 *   <tr><td>CPF</td><td>11 dígitos</td><td>11 dígitos (sem conversão)</td></tr>
 *   <tr><td>identificador</td><td>id da multa / texto</td><td>nosso número {@code long} / código {@code long}</td></tr>
 *   <tr><td>situação</td><td>{@link SituacaoPagamento}</td><td>{@code 1}, {@code 2}, {@code 9}</td></tr>
 * </table>
 */
public class AdaptadorBancoA implements ServicoDeCobranca {

    public static final String CODIGO_BANCO = "A";

    private final BancoAApi banco;

    public AdaptadorBancoA(BancoAApi banco) {
        this.banco = Objects.requireNonNull(banco, "banco");
    }

    @Override
    public CobrancaRegistrada registrar(SolicitacaoCobranca s) {
        long centavos = paraCentavos(s.valor());
        long codigo = banco.registrarTitulo(s.idMulta(), centavos, s.cpfPagador());
        return new CobrancaRegistrada(CODIGO_BANCO, Long.toString(codigo));
    }

    /** Formato do Banco A: {@code evento=EVT-A-1;titulo=1001;valorCentavos=700;status=2}. */
    @Override
    public NotificacaoPagamento traduzirNotificacao(String notificacaoBruta) {
        Map<String, String> campos = new HashMap<>();
        for (String par : Objects.requireNonNull(notificacaoBruta, "notificacaoBruta").split(";")) {
            String[] chaveValor = par.split("=", 2);
            if (chaveValor.length != 2) {
                throw new IllegalArgumentException("Notificação do Banco A mal formada: " + notificacaoBruta);
            }
            campos.put(chaveValor[0].strip(), chaveValor[1].strip());
        }
        try {
            return new NotificacaoPagamento(
                    exigir(campos, "evento"),
                    exigir(campos, "titulo"),
                    deCentavos(Long.parseLong(exigir(campos, "valorCentavos"))),
                    traduzirSituacao(Integer.parseInt(exigir(campos, "status"))));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Notificação do Banco A mal formada: " + notificacaoBruta, e);
        }
    }

    @Override
    public String banco() {
        return CODIGO_BANCO;
    }

    // ---- conversões ----

    static long paraCentavos(BigDecimal reais) {
        return reais.movePointRight(2).longValueExact();   // 7.00 → 700
    }

    static BigDecimal deCentavos(long centavos) {
        return BigDecimal.valueOf(centavos, 2);            // 700 → 7.00
    }

    static SituacaoPagamento traduzirSituacao(int status) {
        return switch (status) {
            case BancoAApi.STATUS_REGISTRADO -> SituacaoPagamento.EM_ABERTO;
            case BancoAApi.STATUS_PAGO -> SituacaoPagamento.PAGO;
            case BancoAApi.STATUS_CANCELADO -> SituacaoPagamento.CANCELADO;
            default -> throw new IllegalArgumentException("Status desconhecido do Banco A: " + status);
        };
    }

    private static String exigir(Map<String, String> campos, String chave) {
        String valor = campos.get(chave);
        if (valor == null || valor.isEmpty()) {
            throw new IllegalArgumentException("Notificação do Banco A sem o campo '" + chave + "'");
        }
        return valor;
    }
}
