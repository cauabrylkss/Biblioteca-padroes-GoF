package br.pucpr.biblioteca.cadastro;

import br.pucpr.biblioteca.academico.Estudante;
import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;
import br.pucpr.biblioteca.rh.Cpf;
import br.pucpr.biblioteca.rh.Pessoa;
import br.pucpr.biblioteca.rh.Relacionamento;
import java.util.Objects;
import java.util.Optional;

/**
 * Papel GoF: <b>ConcreteClass</b> (Template Method, R7) — inclusão do papel de estudante para a pessoa do CPF
 * informado.
 *
 * <ul>
 *   <li>duplicidade: a pessoa do CPF <b>já é estudante</b> → devolve o estudante existente;</li>
 *   <li>pré-requisito (gancho sobrescrito): existir <b>pessoa cadastrada com o CPF</b>. Se não existir, a inclusão
 *       falha aqui — antes de {@code criar}/{@code armazenar} —, então nada é gravado.</li>
 * </ul>
 */
public class CadastroDeEstudante extends CadastroTemplate<String, Estudante> {

    private final Relacionamento relacionamento;

    public CadastroDeEstudante(Relacionamento relacionamento) {
        this.relacionamento = Objects.requireNonNull(relacionamento, "relacionamento");
    }

    @Override
    protected Optional<Estudante> buscarExistente(String cpf) {
        return relacionamento.consultarPessoaPorCpf(cpf)
                .flatMap(pessoa -> relacionamento.consultarEstudanteDaPessoa(pessoa.getId()));
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
    protected Estudante criar(String cpf) {
        return new Estudante(pessoaDoCpf(cpf).getId());
    }

    @Override
    protected void armazenar(Estudante estudante) {
        relacionamento.inserirEstudante(estudante);
    }

    private Pessoa pessoaDoCpf(String cpf) {
        return relacionamento.consultarPessoaPorCpf(cpf).orElseThrow(() -> new RegraDeNegocioException(
                "Não há pessoa cadastrada com o CPF " + cpf + ". Cadastre a pessoa antes do estudante."));
    }
}
