package br.pucpr.biblioteca.compartilhado;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Único lugar onde os dias de atraso são calculados (decisão B-07).
 *
 * <p>Dias <b>corridos</b> entre as datas (sem horário, sem descontar fins de semana). Devolver no próprio dia do
 * vencimento, ou antes, é zero dia de atraso.
 */
public final class DiasDeAtraso {

    private DiasDeAtraso() {
    }

    public static long entre(LocalDate vencimento, LocalDate dataDevolucao) {
        Objects.requireNonNull(vencimento, "vencimento");
        Objects.requireNonNull(dataDevolucao, "dataDevolucao");
        return Math.max(0, ChronoUnit.DAYS.between(vencimento, dataDevolucao));
    }
}
