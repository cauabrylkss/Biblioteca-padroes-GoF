package br.pucpr.biblioteca.biblioteca.acervo;

import br.pucpr.biblioteca.biblioteca.livro.Livro;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Papel GoF: <b>ConcreteComponent</b> (Decorator, R9) — a consulta original ao acervo, sem medição e sem cache.
 *
 * <p>Medição e cache são acrescentados <b>por fora</b>, pelos decoradores; esta classe não muda quando um recurso é
 * ligado, desligado ou criado.
 */
public class ConsultaAcervoPadrao implements ConsultaAcervo {

    private final Acervo acervo;

    public ConsultaAcervoPadrao(Acervo acervo) {
        this.acervo = Objects.requireNonNull(acervo, "acervo");
    }

    @Override
    public Optional<MetadadosLivro> consultarMetadados(long idLivro) {
        return acervo.consultarLivro(idLivro).map(ConsultaAcervoPadrao::metadados);
    }

    @Override
    public List<MetadadosLivro> buscarPorTitulo(String trecho) {
        String procurado = trecho == null ? "" : trecho.strip().toLowerCase(Locale.ROOT);
        return acervo.listarLivros().stream()
                .filter(livro -> livro.getTitulo().toLowerCase(Locale.ROOT).contains(procurado))
                .map(ConsultaAcervoPadrao::metadados)
                .toList();
    }

    private static MetadadosLivro metadados(Livro livro) {
        return new MetadadosLivro(livro.getId(), livro.getTitulo());
    }
}
