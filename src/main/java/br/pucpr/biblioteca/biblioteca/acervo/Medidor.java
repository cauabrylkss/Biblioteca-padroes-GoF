package br.pucpr.biblioteca.biblioteca.acervo;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Onde a {@link ConsultaComMedicao} registra as medições (R9): quantas chamadas cada operação recebeu e o tempo total
 * gasto nelas. Consultado pela administração/CLI e pelos testes.
 */
public class Medidor {

    /** Resumo de uma operação medida. */
    public record Estatistica(long chamadas, Duration tempoTotal) {
    }

    private final Map<String, Estatistica> porOperacao = new TreeMap<>();

    public synchronized void registrar(String operacao, Duration tempo) {
        porOperacao.merge(operacao, new Estatistica(1, tempo),
                (atual, nova) -> new Estatistica(atual.chamadas() + 1, atual.tempoTotal().plus(nova.tempoTotal())));
    }

    public synchronized long chamadas(String operacao) {
        Estatistica e = porOperacao.get(operacao);
        return e == null ? 0 : e.chamadas();
    }

    /** Cópia das medições, por operação (em ordem alfabética). */
    public synchronized Map<String, Estatistica> resumo() {
        return Collections.unmodifiableMap(new TreeMap<>(porOperacao));
    }
}
