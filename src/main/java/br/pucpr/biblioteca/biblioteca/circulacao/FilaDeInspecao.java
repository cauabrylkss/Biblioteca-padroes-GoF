package br.pucpr.biblioteca.biblioteca.circulacao;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Livros devolvidos que aguardam a inspeção do atendente (B-17). O {@link OuvinteInspecao} acrescenta; a fachada
 * retira quando o atendente registra a inspeção. A fila só informa pendências: quem decide o que acontece com o livro
 * é o evento {@code inspecionar(emBomEstado)} no estado DEVOLVIDO.
 */
public class FilaDeInspecao {

    public record Pendencia(long idLivro, long idEstudante, LocalDate dataDevolucao, long diasDeAtraso) {
    }

    private final Map<Long, Pendencia> pendencias = new ConcurrentSkipListMap<>();

    public void adicionar(Pendencia pendencia) {
        pendencias.put(pendencia.idLivro(), pendencia);
    }

    public void concluir(long idLivro) {
        pendencias.remove(idLivro);
    }

    public boolean estaPendente(long idLivro) {
        return pendencias.containsKey(idLivro);
    }

    public List<Pendencia> pendentes() {
        return List.copyOf(pendencias.values());
    }
}
