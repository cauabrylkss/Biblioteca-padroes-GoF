package br.pucpr.biblioteca.biblioteca.circulacao;

import br.pucpr.biblioteca.financeiro.iMulta;
import java.util.Objects;

/**
 * Papel GoF: <b>ConcreteObserver</b> (Observer, R5) — pede ao Financeiro a multa da devolução (B-12).
 *
 * <p>Repassa os dados do evento a {@link iMulta}; o Financeiro calcula com a política vigente (R2) e, se houver valor,
 * emite a multa e registra o boleto no banco (R3). Devolução no prazo — ou dentro da tolerância — não gera multa.
 */
public class OuvinteMulta implements OuvinteDeDevolucao {

    private final iMulta multas;

    public OuvinteMulta(iMulta multas) {
        this.multas = Objects.requireNonNull(multas, "multas");
    }

    @Override
    public void aoDevolver(EventoDevolucao evento) {
        multas.emitirSeHouverAtraso(evento.idEmprestimo(), evento.idEstudante(), evento.vencimento(),
                evento.dataDevolucao());
    }
}
