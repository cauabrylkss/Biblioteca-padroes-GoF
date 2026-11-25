package br.pucpr.biblioteca.biblioteca.circulacao;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Papel GoF: <b>ConcreteObserver</b> (Observer, R5) — atualiza os indicadores de devolução (B-21): total de devoluções,
 * devoluções atrasadas e dias de atraso acumulados.
 */
public class OuvinteIndicadores implements OuvinteDeDevolucao {

    public record Indicadores(long devolucoes, long devolucoesAtrasadas, long diasDeAtrasoAcumulados) {
    }

    private final AtomicLong devolucoes = new AtomicLong();
    private final AtomicLong atrasadas = new AtomicLong();
    private final AtomicLong diasDeAtraso = new AtomicLong();

    @Override
    public void aoDevolver(EventoDevolucao evento) {
        devolucoes.incrementAndGet();
        if (evento.atrasada()) {
            atrasadas.incrementAndGet();
            diasDeAtraso.addAndGet(evento.diasDeAtraso());
        }
    }

    public Indicadores indicadores() {
        return new Indicadores(devolucoes.get(), atrasadas.get(), diasDeAtraso.get());
    }
}
