package br.pucpr.biblioteca.banco;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Papel GoF: <b>Adaptee</b> (Adapter, R3) — simulação da API do <b>Banco A</b> (sistema externo).
 *
 * <p>Formato do Banco A, incompatível com o contrato institucional:
 * <ul>
 *   <li>valores em <b>centavos</b> ({@code long}): R$ 7,00 = {@code 700};</li>
 *   <li>CPF <b>só com dígitos</b>: {@code "12345678909"};</li>
 *   <li>o título recebe o "nosso número" (número do cliente) e devolve um <b>código numérico</b> próprio;</li>
 *   <li>situação como número: {@code 1} = registrado (em aberto), {@code 2} = pago, {@code 9} = cancelado;</li>
 *   <li>notificação de pagamento em texto: {@code evento=EVT-A-1;titulo=1001;valorCentavos=700;status=2}.</li>
 * </ul>
 * Esta classe não conhece nada do sistema da biblioteca e não deve ser alterada para se adaptar a ele.
 */
public class BancoAApi {

    public static final int STATUS_REGISTRADO = 1;
    public static final int STATUS_PAGO = 2;
    public static final int STATUS_CANCELADO = 9;

    /** Título como o Banco A o guarda. */
    public record Titulo(long codigo, long nossoNumero, long valorEmCentavos, String cpfSomenteDigitos, int status) {
    }

    private final AtomicLong proximoCodigo = new AtomicLong(1001);
    private final AtomicLong proximoEvento = new AtomicLong(1);
    private final Map<Long, Titulo> titulos = new LinkedHashMap<>();

    /** Registra um título de cobrança. */
    public synchronized long registrarTitulo(long nossoNumero, long valorEmCentavos, String cpfSomenteDigitos) {
        if (valorEmCentavos <= 0) {
            throw new IllegalArgumentException("BANCO A: valor inválido");
        }
        if (cpfSomenteDigitos == null || !cpfSomenteDigitos.matches("\\d{11}")) {
            throw new IllegalArgumentException("BANCO A: CPF deve ter 11 dígitos, sem pontuação");
        }
        long codigo = proximoCodigo.getAndIncrement();
        titulos.put(codigo, new Titulo(codigo, nossoNumero, valorEmCentavos, cpfSomenteDigitos, STATUS_REGISTRADO));
        return codigo;
    }

    public synchronized Titulo consultarTitulo(long codigo) {
        Titulo t = titulos.get(codigo);
        if (t == null) {
            throw new IllegalArgumentException("BANCO A: título inexistente " + codigo);
        }
        return t;
    }

    /**
     * Simula o pagamento do título e devolve a notificação que o Banco A enviaria ao cliente.
     * Cada chamada gera um evento novo (com outro identificador).
     */
    public synchronized String pagarESimularNotificacao(long codigo) {
        Titulo t = consultarTitulo(codigo);
        titulos.put(codigo, new Titulo(t.codigo(), t.nossoNumero(), t.valorEmCentavos(), t.cpfSomenteDigitos(),
                STATUS_PAGO));
        return "evento=EVT-A-" + proximoEvento.getAndIncrement()
                + ";titulo=" + codigo
                + ";valorCentavos=" + t.valorEmCentavos()
                + ";status=" + STATUS_PAGO;
    }

    /** Último título registrado (usado pelos testes para inspecionar o que chegou ao banco). */
    public synchronized Titulo ultimoTituloRegistrado() {
        return titulos.values().stream().reduce((a, b) -> b)
                .orElseThrow(() -> new IllegalStateException("BANCO A: nenhum título registrado"));
    }
}
