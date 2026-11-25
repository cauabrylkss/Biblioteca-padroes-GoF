package br.pucpr.biblioteca.rh;

import br.pucpr.biblioteca.academico.Estudante;
import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;
import br.pucpr.biblioteca.persistencia.RepositorioDeRelacionamento;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Ponto interno de acesso <b>Relacionamento</b> (Trabalho 02): pessoas e seus papéis (estudante, atendente).
 *
 * <p>Há um único Relacionamento no sistema (decisão C-16), no RH; o Acadêmico o usa para os estudantes. Os dados
 * ficam no repositório criado pela fábrica de persistência do perfil (R8). As regras de inclusão (duplicidade,
 * pré-requisitos) ficam nos cadastros (R7); aqui ficam as operações de acesso e as verificações usadas pela
 * fachada de circulação ({@link #exigirEstudante}, {@link #exigirAtendente}).
 */
public class Relacionamento {

    private final RepositorioDeRelacionamento repositorio;

    public Relacionamento(RepositorioDeRelacionamento repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
    }

    // ---- Pessoa ----

    public Pessoa inserirPessoa(Pessoa pessoa) {
        repositorio.salvarPessoa(pessoa);
        return pessoa;
    }

    public void atualizarPessoa(Pessoa pessoa) {
        exigirPessoa(pessoa.getId());
        repositorio.salvarPessoa(pessoa);
    }

    public Optional<Pessoa> consultarPessoa(long idPessoa) {
        return repositorio.buscarPessoa(idPessoa);
    }

    public Optional<Pessoa> consultarPessoaPorCpf(String cpf) {
        return Cpf.normalizarSeValido(cpf).flatMap(repositorio::buscarPessoaPorCpf);
    }

    public List<Pessoa> listarPessoas() {
        return repositorio.listarPessoas();
    }

    /** Exclui a pessoa; recusado enquanto ela tiver algum papel (estudante ou atendente). */
    public void excluirPessoa(long idPessoa) {
        exigirPessoa(idPessoa);
        if (repositorio.buscarEstudanteDaPessoa(idPessoa).isPresent()
                || repositorio.buscarAtendenteDaPessoa(idPessoa).isPresent()) {
            throw new RegraDeNegocioException(
                    "A pessoa " + idPessoa + " ainda tem papel de estudante ou atendente; exclua o papel antes.");
        }
        repositorio.removerPessoa(idPessoa);
    }

    public Pessoa exigirPessoa(long idPessoa) {
        return consultarPessoa(idPessoa)
                .orElseThrow(() -> new RegraDeNegocioException("Pessoa não cadastrada: " + idPessoa + "."));
    }

    // ---- Estudante ----

    public Estudante inserirEstudante(Estudante estudante) {
        repositorio.inserirEstudante(estudante);
        return estudante;
    }

    public Optional<Estudante> consultarEstudante(long idEstudante) {
        return repositorio.buscarEstudante(idEstudante);
    }

    public Optional<Estudante> consultarEstudanteDaPessoa(long idPessoa) {
        return repositorio.buscarEstudanteDaPessoa(idPessoa);
    }

    public void excluirEstudante(long idEstudante) {
        exigirEstudante(idEstudante);
        repositorio.removerEstudante(idEstudante);
    }

    /** Usado pela fachada: reserva e empréstimo exigem estudante cadastrado (B-05). */
    public Estudante exigirEstudante(long idEstudante) {
        return consultarEstudante(idEstudante)
                .orElseThrow(() -> new RegraDeNegocioException("Estudante não cadastrado: " + idEstudante + "."));
    }

    // ---- Atendente ----

    public Atendente inserirAtendente(Atendente atendente) {
        repositorio.inserirAtendente(atendente);
        return atendente;
    }

    public Optional<Atendente> consultarAtendente(long idAtendente) {
        return repositorio.buscarAtendente(idAtendente);
    }

    public Optional<Atendente> consultarAtendenteDaPessoa(long idPessoa) {
        return repositorio.buscarAtendenteDaPessoa(idPessoa);
    }

    public void excluirAtendente(long idAtendente) {
        exigirAtendente(idAtendente);
        repositorio.removerAtendente(idAtendente);
    }

    /** Usado pela fachada: empréstimo e devolução exigem atendente (B-26). */
    public Atendente exigirAtendente(long idAtendente) {
        return consultarAtendente(idAtendente)
                .orElseThrow(() -> new RegraDeNegocioException("Atendente não cadastrado: " + idAtendente + "."));
    }
}
