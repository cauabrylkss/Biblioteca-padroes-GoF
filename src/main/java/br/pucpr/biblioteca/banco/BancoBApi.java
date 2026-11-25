package br.pucpr.biblioteca.banco;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Papel GoF: <b>Adaptee</b> (Adapter, R3) — simulação da API do <b>Banco B</b> (sistema externo).
 *
 * <p>Formato do Banco B, incompatível com o contrato institucional e com o Banco A:
 * <ul>
 *   <li>valores em <b>reais, como texto com vírgula</b>: R$ 7,00 = {@code "7,00"};</li>
 *   <li>CPF <b>formatado</b>: {@code "123.456.789-09"};</li>
 *   <li>o boleto recebe uma referência textual do cliente e devolve um <b>identificador alfanumérico</b>
 *       ({@code "B-000001"});</li>
 *   <li>situação como texto: {@code "ABERTO"}, {@code "LIQUIDADO"} (pago), {@code "BAIXADO"} (cancelado);</li>
 *   <li>notificação de pagamento em texto separado por barras: {@code LIQUIDADO|B-000001|7,00|ev-b-1}.</li>
 * </ul>
 * Esta classe não conhece nada do sistema da biblioteca e não deve ser alterada para se adaptar a ele.
 */
public class BancoBApi {

    public static final String ABERTO = "ABERTO";
    public static final String LIQUIDADO = "LIQUIDADO";
    public static final String BAIXADO = "BAIXADO";

    /** Boleto como o Banco B o guarda. */
    public record BoletoB(String id, String referencia, String valorReais, String documentoPagador, String situacao) {
    }

    private final AtomicLong proximoId = new AtomicLong(1);
    private final AtomicLong proximoEvento = new AtomicLong(1);
    private final Map<String, BoletoB> boletos = new LinkedHashMap<>();

    /** Continua a numeração depois do maior número já usado (ver {@code BancoAApi.continuarNumeracaoApos}, D-12). */
    public void continuarNumeracaoApos(long ultimoNumeroUsado) {
        proximoId.accumulateAndGet(ultimoNumeroUsado + 1, Math::max);
    }

    /** Cria um boleto. */
    public synchronized String criarBoleto(String referencia, String valorReais, String documentoPagador) {
        if (valorReais == null || !valorReais.matches("\\d+,\\d{2}")) {
            throw new IllegalArgumentException("BANCO B: valor deve estar no formato 0,00");
        }
        if (documentoPagador == null || !documentoPagador.matches("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}")) {
            throw new IllegalArgumentException("BANCO B: CPF deve estar no formato 000.000.000-00");
        }
        String id = String.format("B-%06d", proximoId.getAndIncrement());
        boletos.put(id, new BoletoB(id, referencia, valorReais, documentoPagador, ABERTO));
        return id;
    }

    public synchronized BoletoB consultarBoleto(String id) {
        BoletoB b = boletos.get(id);
        if (b == null) {
            throw new IllegalArgumentException("BANCO B: boleto inexistente " + id);
        }
        return b;
    }

    /** Simula a liquidação e devolve a notificação que o Banco B enviaria. Cada chamada gera um evento novo. */
    public synchronized String liquidarESimularNotificacao(String id) {
        BoletoB b = consultarBoleto(id);
        boletos.put(id, new BoletoB(b.id(), b.referencia(), b.valorReais(), b.documentoPagador(), LIQUIDADO));
        return LIQUIDADO + "|" + id + "|" + b.valorReais() + "|ev-b-" + proximoEvento.getAndIncrement();
    }

    /** Último boleto criado (usado pelos testes para inspecionar o que chegou ao banco). */
    public synchronized BoletoB ultimoBoletoCriado() {
        return boletos.values().stream().reduce((a, b) -> b)
                .orElseThrow(() -> new IllegalStateException("BANCO B: nenhum boleto criado"));
    }
}
