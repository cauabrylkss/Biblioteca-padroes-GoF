package br.pucpr.biblioteca.biblioteca.circulacao;

/**
 * Papel GoF: <b>Observer</b> (R5) — reação independente a uma devolução confirmada.
 *
 * <p>Para criar uma reação nova basta implementar esta interface e registrá-la no publicador, no composition root
 * ({@code config.Aplicacao}). Nem a fachada nem os tratadores mudam.
 */
@FunctionalInterface
public interface OuvinteDeDevolucao {

    void aoDevolver(EventoDevolucao evento);

    /** Nome usado nos avisos de falha. */
    default String nome() {
        return getClass().getSimpleName();
    }
}
