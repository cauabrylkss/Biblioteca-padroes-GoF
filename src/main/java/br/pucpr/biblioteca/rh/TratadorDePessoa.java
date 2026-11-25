package br.pucpr.biblioteca.rh;

import br.pucpr.biblioteca.cadastro.CadastroDePessoa;
import br.pucpr.biblioteca.cadastro.Resultado;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Tratador de pessoas (Trabalho 02), realiza {@link iPessoa}. Delega a inclusão ao {@link CadastroDePessoa}
 * (Template Method, R7) e as demais operações ao {@link Relacionamento}. Não contém regras.
 */
public class TratadorDePessoa implements iPessoa {

    private final CadastroDePessoa cadastro;
    private final Relacionamento relacionamento;

    public TratadorDePessoa(CadastroDePessoa cadastro, Relacionamento relacionamento) {
        this.cadastro = Objects.requireNonNull(cadastro, "cadastro");
        this.relacionamento = Objects.requireNonNull(relacionamento, "relacionamento");
    }

    @Override
    public Resultado<FichaDaPessoa> inserirPessoa(String nome, String sobrenome, String cpf) {
        return cadastro.incluir(new CadastroDePessoa.Dados(nome, sobrenome, cpf)).mapear(FichaDaPessoa::de);
    }

    @Override
    public FichaDaPessoa atualizarPessoa(long idPessoa, String nome, String sobrenome) {
        Pessoa pessoa = relacionamento.exigirPessoa(idPessoa);
        pessoa.alterarNome(nome, sobrenome);
        relacionamento.atualizarPessoa(pessoa);
        return FichaDaPessoa.de(pessoa);
    }

    @Override
    public Optional<FichaDaPessoa> consultarPessoa(long idPessoa) {
        return relacionamento.consultarPessoa(idPessoa).map(FichaDaPessoa::de);
    }

    @Override
    public List<FichaDaPessoa> consultarPessoas() {
        return relacionamento.listarPessoas().stream().map(FichaDaPessoa::de).toList();
    }

    @Override
    public void excluirPessoa(long idPessoa) {
        relacionamento.excluirPessoa(idPessoa);
    }
}
