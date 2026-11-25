package br.pucpr.biblioteca.rh;

import br.pucpr.biblioteca.cadastro.Resultado;
import java.util.List;
import java.util.Optional;

/** Interface do componente RH para pessoas (nome preservado do Trabalho 02). Realizada por {@link TratadorDePessoa}. */
public interface iPessoa {

    /** Inclusão pela rotina comum de cadastro (R7). CPF já cadastrado devolve a pessoa existente. */
    Resultado<FichaDaPessoa> inserirPessoa(String nome, String sobrenome, String cpf);

    FichaDaPessoa atualizarPessoa(long idPessoa, String nome, String sobrenome);

    Optional<FichaDaPessoa> consultarPessoa(long idPessoa);

    List<FichaDaPessoa> consultarPessoas();

    void excluirPessoa(long idPessoa);
}
