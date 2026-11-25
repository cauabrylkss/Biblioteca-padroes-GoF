package br.pucpr.biblioteca.biblioteca.acervo;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Papel GoF: <b>ConcreteDecorator</b> (Decorator, R9) — mede cada consulta (chamadas e tempo) no {@link Medidor}.
 *
 * <p>Na instalação, fica <b>por fora</b> do cache: {@code Medicao(Cache(Padrao))}. Assim toda consulta é medida,
 * inclusive as respondidas pelo cache (cenário R9). Se ficasse por dentro ({@code Cache(Medicao(Padrao))}), só as
 * consultas que chegam ao original seriam medidas.
 */
public class ConsultaComMedicao extends DecoradorDeConsulta {

    public static final String OPERACAO_METADADOS = "consultarMetadados";
    public static final String OPERACAO_BUSCA = "buscarPorTitulo";

    private final Medidor medidor;

    public ConsultaComMedicao(ConsultaAcervo interna, Medidor medidor) {
        super(interna);
        this.medidor = Objects.requireNonNull(medidor, "medidor");
    }

    @Override
    public Optional<MetadadosLivro> consultarMetadados(long idLivro) {
        return medir(OPERACAO_METADADOS, () -> super.consultarMetadados(idLivro));
    }

    @Override
    public List<MetadadosLivro> buscarPorTitulo(String trecho) {
        return medir(OPERACAO_BUSCA, () -> super.buscarPorTitulo(trecho));
    }

    /** Executa a consulta interna e registra o tempo, mesmo que ela falhe. */
    private <T> T medir(String operacao, Supplier<T> consulta) {
        long inicio = System.nanoTime();
        try {
            return consulta.get();
        } finally {
            medidor.registrar(operacao, Duration.ofNanos(System.nanoTime() - inicio));
        }
    }
}
