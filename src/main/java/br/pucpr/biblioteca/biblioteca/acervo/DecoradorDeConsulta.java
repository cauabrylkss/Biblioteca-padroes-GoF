package br.pucpr.biblioteca.biblioteca.acervo;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Papel GoF: <b>Decorator</b> (R9) — base dos recursos opcionais da consulta ao acervo.
 *
 * <p>É uma {@link ConsultaAcervo} que <b>guarda outra</b> {@link ConsultaAcervo} ({@link #interna()}) e, por padrão,
 * repassa tudo a ela. Cada decorador concreto sobrescreve só o que acrescenta. Como decorador e consulta original têm
 * o mesmo tipo, a cadeia pode ter qualquer combinação: {@code Medicao(Cache(Padrao))}, {@code Medicao(Padrao)}...
 */
public abstract class DecoradorDeConsulta implements ConsultaAcervo {

    private final ConsultaAcervo interna;

    protected DecoradorDeConsulta(ConsultaAcervo interna) {
        this.interna = Objects.requireNonNull(interna, "interna");
    }

    /** A consulta que este decorador envolve (usado também para mostrar/testar a ordem da cadeia). */
    public ConsultaAcervo interna() {
        return interna;
    }

    @Override
    public Optional<MetadadosLivro> consultarMetadados(long idLivro) {
        return interna.consultarMetadados(idLivro);
    }

    @Override
    public List<MetadadosLivro> buscarPorTitulo(String trecho) {
        return interna.buscarPorTitulo(trecho);
    }

    @Override
    public void livroAlterado(long idLivro) {
        interna.livroAlterado(idLivro);
    }
}
