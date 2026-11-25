package br.pucpr.biblioteca.cadastro;

import java.util.Objects;
import java.util.function.Function;

/**
 * Resultado de uma inclusão (R7): o cadastro e se ele foi criado agora ({@code novo = true}) ou já existia
 * ({@code novo = false}, inclusão duplicada devolve o existente).
 */
public record Resultado<T>(T cadastro, boolean novo) {

    public Resultado {
        Objects.requireNonNull(cadastro, "cadastro");
    }

    /** Converte o cadastro (ex.: entidade → ficha para a interface), mantendo o indicador {@code novo}. */
    public <U> Resultado<U> mapear(Function<? super T, ? extends U> conversao) {
        return new Resultado<>(conversao.apply(cadastro), novo);
    }
}
