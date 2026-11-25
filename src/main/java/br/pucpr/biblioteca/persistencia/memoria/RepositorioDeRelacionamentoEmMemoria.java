package br.pucpr.biblioteca.persistencia.memoria;

import br.pucpr.biblioteca.academico.Estudante;
import br.pucpr.biblioteca.persistencia.RepositorioDeRelacionamento;
import br.pucpr.biblioteca.rh.Atendente;
import br.pucpr.biblioteca.rh.Pessoa;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/** Papel GoF: <b>ConcreteProduct</b> (Abstract Factory, R8) — pessoas, estudantes e atendentes em memória. */
public final class RepositorioDeRelacionamentoEmMemoria implements RepositorioDeRelacionamento {

    private record RegistroPessoa(long id, String nome, String sobrenome, String cpf, LocalDateTime dataCriacao) {
    }

    private record RegistroPapel(long id, long idPessoa) {
    }

    private final UnidadeDeTrabalhoEmMemoria unidade;
    private final TabelaEmMemoria<RegistroPessoa> pessoas;
    private final TabelaEmMemoria<RegistroPapel> estudantes;
    private final TabelaEmMemoria<RegistroPapel> atendentes;
    private final AtomicLong proximaPessoa = new AtomicLong(1);
    private final AtomicLong proximoEstudante = new AtomicLong(1);
    private final AtomicLong proximoAtendente = new AtomicLong(1);

    RepositorioDeRelacionamentoEmMemoria(UnidadeDeTrabalhoEmMemoria unidade) {
        this.unidade = unidade;
        this.pessoas = new TabelaEmMemoria<>(unidade);
        this.estudantes = new TabelaEmMemoria<>(unidade);
        this.atendentes = new TabelaEmMemoria<>(unidade);
    }

    UnidadeDeTrabalhoEmMemoria unidadeDeTrabalho() {
        return unidade;
    }

    // ---- Pessoa ----

    @Override
    public void salvarPessoa(Pessoa pessoa) {
        unidade.comTrava(() -> {
            if (pessoa.getId() == null) {
                pessoa.atribuirId(proximaPessoa.getAndIncrement());
            }
            pessoas.gravar(pessoa.getId(), new RegistroPessoa(pessoa.getId(), pessoa.getNome(),
                    pessoa.getSobrenome(), pessoa.getCpf(), pessoa.getDataCriacao()));
        });
    }

    @Override
    public Optional<Pessoa> buscarPessoa(long idPessoa) {
        return unidade.comTrava(() -> pessoas.buscar(idPessoa).map(RepositorioDeRelacionamentoEmMemoria::paraPessoa));
    }

    @Override
    public Optional<Pessoa> buscarPessoaPorCpf(String cpfSomenteDigitos) {
        return unidade.comTrava(() -> pessoas.filtrar(p -> p.cpf().equals(cpfSomenteDigitos)).stream()
                .findFirst().map(RepositorioDeRelacionamentoEmMemoria::paraPessoa));
    }

    @Override
    public List<Pessoa> listarPessoas() {
        return unidade.comTrava(() -> pessoas.todos().stream()
                .map(RepositorioDeRelacionamentoEmMemoria::paraPessoa).toList());
    }

    @Override
    public void removerPessoa(long idPessoa) {
        unidade.comTrava(() -> pessoas.remover(idPessoa));
    }

    // ---- Estudante ----

    @Override
    public void inserirEstudante(Estudante estudante) {
        unidade.comTrava(() -> {
            estudante.atribuirId(proximoEstudante.getAndIncrement());
            estudantes.gravar(estudante.getId(), new RegistroPapel(estudante.getId(), estudante.getIdPessoa()));
        });
    }

    @Override
    public Optional<Estudante> buscarEstudante(long idEstudante) {
        return unidade.comTrava(() -> estudantes.buscar(idEstudante)
                .map(r -> Estudante.reconstituir(r.id(), r.idPessoa())));
    }

    @Override
    public Optional<Estudante> buscarEstudanteDaPessoa(long idPessoa) {
        return unidade.comTrava(() -> estudantes.filtrar(r -> r.idPessoa() == idPessoa).stream().findFirst()
                .map(r -> Estudante.reconstituir(r.id(), r.idPessoa())));
    }

    @Override
    public void removerEstudante(long idEstudante) {
        unidade.comTrava(() -> estudantes.remover(idEstudante));
    }

    // ---- Atendente ----

    @Override
    public void inserirAtendente(Atendente atendente) {
        unidade.comTrava(() -> {
            atendente.atribuirId(proximoAtendente.getAndIncrement());
            atendentes.gravar(atendente.getId(), new RegistroPapel(atendente.getId(), atendente.getIdPessoa()));
        });
    }

    @Override
    public Optional<Atendente> buscarAtendente(long idAtendente) {
        return unidade.comTrava(() -> atendentes.buscar(idAtendente)
                .map(r -> Atendente.reconstituir(r.id(), r.idPessoa())));
    }

    @Override
    public Optional<Atendente> buscarAtendenteDaPessoa(long idPessoa) {
        return unidade.comTrava(() -> atendentes.filtrar(r -> r.idPessoa() == idPessoa).stream().findFirst()
                .map(r -> Atendente.reconstituir(r.id(), r.idPessoa())));
    }

    @Override
    public void removerAtendente(long idAtendente) {
        unidade.comTrava(() -> atendentes.remover(idAtendente));
    }

    private static Pessoa paraPessoa(RegistroPessoa r) {
        return Pessoa.reconstituir(r.id(), r.nome(), r.sobrenome(), r.cpf(), r.dataCriacao());
    }
}
