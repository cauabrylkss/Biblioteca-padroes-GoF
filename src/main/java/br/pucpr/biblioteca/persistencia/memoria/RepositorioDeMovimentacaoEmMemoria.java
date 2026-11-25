package br.pucpr.biblioteca.persistencia.memoria;

import br.pucpr.biblioteca.biblioteca.movimentacao.Devolucao;
import br.pucpr.biblioteca.biblioteca.movimentacao.Emprestimo;
import br.pucpr.biblioteca.biblioteca.movimentacao.Reserva;
import br.pucpr.biblioteca.biblioteca.movimentacao.Transacao;
import br.pucpr.biblioteca.persistencia.RepositorioDeMovimentacao;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Papel GoF: <b>ConcreteProduct</b> (Abstract Factory, R8) — transações de circulação em memória.
 *
 * <p>Cada transação vira um registro imutável (como uma linha de tabela) e volta a ser objeto na leitura.
 * Um empréstimo é <b>ativo</b> enquanto nenhuma devolução o referencia.
 */
public final class RepositorioDeMovimentacaoEmMemoria implements RepositorioDeMovimentacao {

    private enum Tipo { RESERVA, EMPRESTIMO, DEVOLUCAO }

    /** Uma linha da "tabela" de transações; campos que não se aplicam ao tipo ficam zerados/nulos. */
    private record RegistroTransacao(long id, Tipo tipo, long idLivro, long idEstudante, LocalDate data,
                                     long idAtendente, LocalDate vencimento, int numRenovacoes, long idEmprestimo) {
    }

    private final UnidadeDeTrabalhoEmMemoria unidade;
    private final TabelaEmMemoria<RegistroTransacao> tabela;
    private final AtomicLong proximoId = new AtomicLong(1);

    RepositorioDeMovimentacaoEmMemoria(UnidadeDeTrabalhoEmMemoria unidade) {
        this.unidade = unidade;
        this.tabela = new TabelaEmMemoria<>(unidade);
    }

    UnidadeDeTrabalhoEmMemoria unidadeDeTrabalho() {
        return unidade;
    }

    @Override
    public void inserir(Transacao transacao) {
        unidade.comTrava(() -> {
            if (transacao.getId() != null) {
                throw new IllegalStateException("Transação já gravada: " + transacao.getId());
            }
            transacao.atribuirId(proximoId.getAndIncrement());
            tabela.gravar(transacao.getId(), paraRegistro(transacao));
        });
    }

    @Override
    public void atualizar(Emprestimo emprestimo) {
        unidade.comTrava(() -> {
            exigirGravada(emprestimo);
            tabela.gravar(emprestimo.getId(), paraRegistro(emprestimo));
        });
    }

    @Override
    public void remover(Transacao transacao) {
        unidade.comTrava(() -> {
            exigirGravada(transacao);
            tabela.remover(transacao.getId());
        });
    }

    @Override
    public Optional<Reserva> buscarReservaDoLivro(long idLivro) {
        return unidade.comTrava(() -> tabela.filtrar(r -> r.tipo() == Tipo.RESERVA && r.idLivro() == idLivro)
                .stream().findFirst().map(r -> (Reserva) paraTransacao(r)));
    }

    @Override
    public Optional<Emprestimo> buscarEmprestimoAtivo(long idLivro) {
        return unidade.comTrava(() -> {
            Set<Long> devolvidos = tabela.filtrar(r -> r.tipo() == Tipo.DEVOLUCAO).stream()
                    .map(RegistroTransacao::idEmprestimo).collect(Collectors.toSet());
            return tabela.filtrar(r -> r.tipo() == Tipo.EMPRESTIMO && r.idLivro() == idLivro
                            && !devolvidos.contains(r.id()))
                    .stream().findFirst().map(r -> (Emprestimo) paraTransacao(r));
        });
    }

    @Override
    public Optional<Devolucao> buscarUltimaDevolucao(long idLivro, long idEstudante) {
        return unidade.comTrava(() -> tabela.filtrar(r -> r.tipo() == Tipo.DEVOLUCAO && r.idLivro() == idLivro
                        && r.idEstudante() == idEstudante)
                .stream().max(Comparator.comparingLong(RegistroTransacao::id))
                .map(r -> (Devolucao) paraTransacao(r)));
    }

    @Override
    public List<Transacao> listarTodas() {
        return unidade.comTrava(() -> tabela.todos().stream()
                .map(RepositorioDeMovimentacaoEmMemoria::paraTransacao).toList());
    }

    private static void exigirGravada(Transacao transacao) {
        if (transacao.getId() == null) {
            throw new IllegalStateException("Transação ainda não gravada");
        }
    }

    private static RegistroTransacao paraRegistro(Transacao t) {
        return switch (t) {
            case Reserva r -> new RegistroTransacao(r.getId(), Tipo.RESERVA, r.getIdLivro(), r.getIdEstudante(),
                    r.getDataTransacao(), 0, null, 0, 0);
            case Emprestimo e -> new RegistroTransacao(e.getId(), Tipo.EMPRESTIMO, e.getIdLivro(), e.getIdEstudante(),
                    e.getDataTransacao(), e.getIdAtendente(), e.getVencimento(), e.getNumRenovacoes(), 0);
            case Devolucao d -> new RegistroTransacao(d.getId(), Tipo.DEVOLUCAO, d.getIdLivro(), d.getIdEstudante(),
                    d.getDataTransacao(), d.getIdAtendente(), d.getVencimento(), 0, d.getIdEmprestimo());
        };
    }

    private static Transacao paraTransacao(RegistroTransacao r) {
        return switch (r.tipo()) {
            case RESERVA -> Reserva.reconstituir(r.id(), r.idLivro(), r.idEstudante(), r.data());
            case EMPRESTIMO -> Emprestimo.reconstituir(r.id(), r.idLivro(), r.idEstudante(), r.idAtendente(),
                    r.data(), r.vencimento(), r.numRenovacoes());
            case DEVOLUCAO -> Devolucao.reconstituir(r.id(), r.idEmprestimo(), r.idLivro(), r.idEstudante(),
                    r.idAtendente(), r.vencimento(), r.data());
        };
    }
}
