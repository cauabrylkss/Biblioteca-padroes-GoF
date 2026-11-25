package br.pucpr.biblioteca.cadastro;

import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;
import br.pucpr.biblioteca.rh.Atendente;
import br.pucpr.biblioteca.rh.Cpf;
import br.pucpr.biblioteca.rh.Pessoa;
import br.pucpr.biblioteca.rh.Relacionamento;
import java.util.Objects;
import java.util.Optional;

/**
 * Papel GoF: <b>ConcreteClass</b> (Template Method, R7) — inclusão do papel de atendente para a pessoa do CPF
 * informado. Mesmas regras do {@link CadastroDeEstudante}: duplicidade = a pessoa já é atendente; pré-requisito =
 * pessoa cadastrada com o CPF (verificado antes de qualquer gravação).
 */
public class CadastroDeAtendente extends CadastroTemplate<String, Atendente> {

    private final Relacionamento relacionamento;

    public CadastroDeAtendente(Relacionamento relacionamento) {
        this.relacionamento = Objects.requireNonNull(relacionamento, "relacionamento");
    }

    @Override
    protected Optional<Atendente> buscarExistente(String cpf) {
        return relacionamento.consultarPessoaPorCpf(cpf)
                .flatMap(pessoa -> relacionamento.consultarAtendenteDaPessoa(pessoa.getId()));
    }

    @Override
    protected void validar(String cpf) {
        Cpf.normalizar(cpf);
    }

    @Override
    protected void verificarPreRequisitos(String cpf) {
        pessoaDoCpf(cpf);
    }

    @Override
    protected Atendente criar(String cpf) {
        return new Atendente(pessoaDoCpf(cpf).getId());
    }

    @Override
    protected void armazenar(Atendente atendente) {
        relacionamento.inserirAtendente(atendente);
    }

    private Pessoa pessoaDoCpf(String cpf) {
        return relacionamento.consultarPessoaPorCpf(cpf).orElseThrow(() -> new RegraDeNegocioException(
                "Não há pessoa cadastrada com o CPF " + cpf + ". Cadastre a pessoa antes do atendente."));
    }
}
