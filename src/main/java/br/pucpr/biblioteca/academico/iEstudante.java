package br.pucpr.biblioteca.academico;

import br.pucpr.biblioteca.cadastro.Resultado;
import br.pucpr.biblioteca.rh.FichaDoPapel;
import java.util.Optional;

/** Interface do componente Acadêmico (nome preservado do Trabalho 02). Realizada por {@link TratadorDeEstudante}. */
public interface iEstudante {

    /**
     * Dá o papel de estudante à pessoa do CPF (R7). Estudante já cadastrado devolve o existente; CPF sem pessoa
     * cadastrada falha antes de qualquer gravação.
     */
    Resultado<FichaDoPapel> inserirEstudante(String cpf);

    Optional<FichaDoPapel> consultarEstudante(long idEstudante);

    void excluirEstudante(long idEstudante);
}
