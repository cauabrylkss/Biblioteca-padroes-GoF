package br.pucpr.biblioteca.persistencia;

import java.util.function.Supplier;

/**
 * Papel GoF: <b>AbstractProduct</b> (Abstract Factory, R8) — unidade de trabalho (transação).
 *
 * <p>Tudo o que os repositórios de uma mesma família gravam entre {@link #iniciar()} e {@link #confirmar()} vale como
 * uma coisa só: se algo falhar no meio, {@link #desfazer()} devolve os dados ao que eram no início. Cada fábrica cria
 * <b>uma</b> unidade de trabalho e entrega <b>a mesma</b> a todos os seus repositórios — é assim que eles
 * compartilham a transação.
 */
public interface UnidadeDeTrabalho {

    /** Abre a transação. Não é permitido abrir uma segunda transação dentro da primeira. */
    void iniciar();

    /** Torna definitivas as gravações feitas desde {@link #iniciar()}. */
    void confirmar();

    /** Descarta as gravações feitas desde {@link #iniciar()}. */
    void desfazer();

    /** Há transação aberta pela thread atual? */
    boolean emAndamento();

    /**
     * Executa a operação inteira numa transação: confirma se ela terminar normalmente; desfaz tudo e repassa a exceção
     * se qualquer passo falhar. É o que a fachada de circulação usa em cada operação (R4).
     */
    default <T> T executar(Supplier<T> operacao) {
        iniciar();
        T resultado;
        try {
            resultado = operacao.get();
        } catch (RuntimeException | Error falha) {
            desfazer();
            throw falha;
        }
        confirmar();
        return resultado;
    }

    /** Variante de {@link #executar(Supplier)} para operações sem resultado. */
    default void executar(Runnable operacao) {
        executar(() -> {
            operacao.run();
            return null;
        });
    }
}
