package br.pucpr.biblioteca.persistencia.memoria;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Uma "tabela" em memória: registros por id, participando da unidade de trabalho (foto ao iniciar, restaurada ao
 * desfazer). Os registros guardados são imutáveis (records), por isso a foto pode ser uma simples cópia do mapa.
 *
 * @param <R> tipo do registro gravado (record imutável)
 */
final class TabelaEmMemoria<R> implements UnidadeDeTrabalhoEmMemoria.Participante {

    private Map<Long, R> registros = new LinkedHashMap<>();
    private Map<Long, R> foto;

    TabelaEmMemoria(UnidadeDeTrabalhoEmMemoria unidade) {
        unidade.inscrever(this);
    }

    void gravar(long id, R registro) {
        registros.put(id, registro);
    }

    Optional<R> buscar(long id) {
        return Optional.ofNullable(registros.get(id));
    }

    List<R> filtrar(Predicate<R> condicao) {
        return registros.values().stream().filter(condicao).toList();
    }

    List<R> todos() {
        return List.copyOf(registros.values());
    }

    boolean remover(long id) {
        return registros.remove(id) != null;
    }

    @Override
    public void fotografar() {
        foto = new LinkedHashMap<>(registros);
    }

    @Override
    public void restaurarFoto() {
        registros = foto;
        foto = null;
    }

    @Override
    public void descartarFoto() {
        foto = null;
    }
}
