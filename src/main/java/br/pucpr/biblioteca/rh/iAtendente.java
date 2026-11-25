package br.pucpr.biblioteca.rh;

import br.pucpr.biblioteca.cadastro.Resultado;
import java.util.Optional;

/** Interface do componente RH para atendentes (nome preservado do Trabalho 02). Realizada por {@link TratadorDeAtendente}. */
public interface iAtendente {

    /** Dá o papel de atendente à pessoa do CPF (R7). A pessoa precisa estar cadastrada. */
    Resultado<FichaDoPapel> inserirAtendente(String cpf);

    Optional<FichaDoPapel> consultarAtendente(long idAtendente);

    void excluirAtendente(long idAtendente);
}
