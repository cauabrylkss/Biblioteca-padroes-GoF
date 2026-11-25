package br.pucpr.biblioteca.cadastro;

import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;
import br.pucpr.biblioteca.rh.Cpf;
import br.pucpr.biblioteca.rh.Pessoa;
import br.pucpr.biblioteca.rh.Relacionamento;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Papel GoF: <b>ConcreteClass</b> (Template Method, R7) — inclusão de pessoa. Duplicidade pelo <b>CPF</b> (PDF R7).
 * Não tem pré-requisitos: usa o gancho {@code verificarPreRequisitos} com o comportamento padrão (vazio).
 */
public class CadastroDePessoa extends CadastroTemplate<CadastroDePessoa.Dados, Pessoa> {

    public record Dados(String nome, String sobrenome, String cpf) {
    }

    private final Relacionamento relacionamento;
    private final Clock relogio;

    public CadastroDePessoa(Relacionamento relacionamento, Clock relogio) {
        this.relacionamento = Objects.requireNonNull(relacionamento, "relacionamento");
        this.relogio = Objects.requireNonNull(relogio, "relogio");
    }

    @Override
    protected Optional<Pessoa> buscarExistente(Dados dados) {
        return relacionamento.consultarPessoaPorCpf(dados.cpf());     // CPF inválido → vazio; validar() acusa
    }

    @Override
    protected void validar(Dados dados) {
        if (dados.nome() == null || dados.nome().isBlank()
                || dados.sobrenome() == null || dados.sobrenome().isBlank()) {
            throw new RegraDeNegocioException("Nome e sobrenome são obrigatórios.");
        }
        Cpf.normalizar(dados.cpf());                                    // lança exceção se o CPF for inválido
    }

    @Override
    protected Pessoa criar(Dados dados) {
        return new Pessoa(dados.nome(), dados.sobrenome(), dados.cpf(), LocalDateTime.now(relogio));
    }

    @Override
    protected void armazenar(Pessoa pessoa) {
        relacionamento.inserirPessoa(pessoa);
    }
}
