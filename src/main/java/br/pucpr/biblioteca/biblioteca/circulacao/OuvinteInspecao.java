package br.pucpr.biblioteca.biblioteca.circulacao;

import java.util.Objects;

/**
 * Papel GoF: <b>ConcreteObserver</b> (Observer, R5) — põe o livro devolvido na fila de inspeção.
 * Não muda o estado do livro: ele continua DEVOLVIDO até o atendente registrar a inspeção.
 */
public class OuvinteInspecao implements OuvinteDeDevolucao {

    private final FilaDeInspecao fila;

    public OuvinteInspecao(FilaDeInspecao fila) {
        this.fila = Objects.requireNonNull(fila, "fila");
    }

    @Override
    public void aoDevolver(EventoDevolucao evento) {
        fila.adicionar(new FilaDeInspecao.Pendencia(evento.idLivro(), evento.idEstudante(), evento.dataDevolucao(),
                evento.diasDeAtraso()));
    }
}
