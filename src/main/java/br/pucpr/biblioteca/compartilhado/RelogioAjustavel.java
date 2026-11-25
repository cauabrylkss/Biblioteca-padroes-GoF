package br.pucpr.biblioteca.compartilhado;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Relógio que pode ser adiantado (decisão C-11). Toda data do sistema vem de um {@link Clock} injetado; nos testes e
 * na demonstração usamos este relógio para simular dias passando (ex.: devolução atrasada) sem esperar de verdade.
 */
public final class RelogioAjustavel extends Clock {

    private final ZoneId zona;
    private volatile Instant agora;

    public RelogioAjustavel(Instant inicio, ZoneId zona) {
        this.agora = Objects.requireNonNull(inicio, "inicio");
        this.zona = Objects.requireNonNull(zona, "zona");
    }

    /** Relógio parado no início do dia informado, no fuso de São Paulo. */
    public static RelogioAjustavel em(LocalDate dia) {
        ZoneId zona = ZoneId.of("America/Sao_Paulo");
        return new RelogioAjustavel(dia.atStartOfDay(zona).toInstant(), zona);
    }

    /** Relógio que começa no instante atual do sistema. */
    public static RelogioAjustavel agoraDoSistema() {
        return new RelogioAjustavel(Instant.now(), ZoneId.systemDefault());
    }

    public void avancarDias(long dias) {
        if (dias < 0) {
            throw new IllegalArgumentException("Só é possível avançar o relógio");
        }
        agora = agora.plus(Duration.ofDays(dias));
    }

    public LocalDate hoje() {
        return LocalDate.now(this);
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId novaZona) {
        return new RelogioAjustavel(agora, novaZona);
    }

    @Override
    public Instant instant() {
        return agora;
    }
}
