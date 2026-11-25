package br.pucpr.biblioteca.financeiro;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Interface que o Financeiro provê à Biblioteca (diagrama de componentes, decisão C-15). Realizada por
 * {@link ServicoDeMultas}.
 */
public interface iMulta {

    /**
     * Emite a multa de uma devolução atrasada (com o boleto registrado no banco da instalação), se a política vigente
     * cobrar algum valor pelos dias de atraso.
     *
     * @return a multa emitida, ou vazio se não houver valor a cobrar
     */
    Optional<Multa> emitirSeHouverAtraso(long idEmprestimo, long idEstudante, LocalDate vencimento,
                                         LocalDate dataDevolucao);

    /** Multa emitida para o empréstimo, se houver (consulta/reemissão do boleto pelo atendente). */
    Optional<Multa> multaDoEmprestimo(long idEmprestimo);
}
