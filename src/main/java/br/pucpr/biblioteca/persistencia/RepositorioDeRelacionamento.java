package br.pucpr.biblioteca.persistencia;

import br.pucpr.biblioteca.academico.Estudante;
import br.pucpr.biblioteca.rh.Atendente;
import br.pucpr.biblioteca.rh.Pessoa;
import java.util.List;
import java.util.Optional;

/**
 * Papel GoF: <b>AbstractProduct</b> (Abstract Factory, R8) — pessoas e seus papéis (estudante, atendente).
 * Cada leitura devolve um objeto novo.
 */
public interface RepositorioDeRelacionamento {

    /** Inclui (atribuindo id) ou atualiza a pessoa. */
    void salvarPessoa(Pessoa pessoa);

    Optional<Pessoa> buscarPessoa(long idPessoa);

    Optional<Pessoa> buscarPessoaPorCpf(String cpfSomenteDigitos);

    List<Pessoa> listarPessoas();

    void removerPessoa(long idPessoa);

    /** Inclui o estudante, atribuindo id. */
    void inserirEstudante(Estudante estudante);

    Optional<Estudante> buscarEstudante(long idEstudante);

    Optional<Estudante> buscarEstudanteDaPessoa(long idPessoa);

    void removerEstudante(long idEstudante);

    /** Inclui o atendente, atribuindo id. */
    void inserirAtendente(Atendente atendente);

    Optional<Atendente> buscarAtendente(long idAtendente);

    Optional<Atendente> buscarAtendenteDaPessoa(long idPessoa);

    void removerAtendente(long idAtendente);
}
