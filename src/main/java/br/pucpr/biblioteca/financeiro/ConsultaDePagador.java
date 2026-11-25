package br.pucpr.biblioteca.financeiro;

/**
 * O que o Financeiro precisa saber de um estudante para cobrá-lo: nome e CPF.
 *
 * <p>No diagrama de componentes o Financeiro <b>requer</b> dados de pessoa do RH; esta interface é esse ponto de
 * dependência. Quem a implementa é o lado do RH/Acadêmico (via {@code Relacionamento}), montado no composition root.
 */
@FunctionalInterface
public interface ConsultaDePagador {

    record Pagador(String nome, String cpf) {
    }

    /**
     * @throws br.pucpr.biblioteca.compartilhado.RegraDeNegocioException se o estudante não existir
     */
    Pagador pagadorDoEstudante(long idEstudante);
}
