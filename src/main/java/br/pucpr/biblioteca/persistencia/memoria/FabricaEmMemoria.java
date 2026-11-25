package br.pucpr.biblioteca.persistencia.memoria;

import br.pucpr.biblioteca.financeiro.RepositorioDeMultas;
import br.pucpr.biblioteca.persistencia.FabricaDePersistencia;
import br.pucpr.biblioteca.persistencia.RepositorioDeLivros;
import br.pucpr.biblioteca.persistencia.RepositorioDeMovimentacao;
import br.pucpr.biblioteca.persistencia.RepositorioDeRelacionamento;
import br.pucpr.biblioteca.persistencia.UnidadeDeTrabalho;

/**
 * Papel GoF: <b>ConcreteFactory</b> (Abstract Factory, R8) — família em memória (perfil de teste).
 *
 * <p>Cria <b>uma</b> {@link UnidadeDeTrabalhoEmMemoria} e a entrega, no construtor, a cada repositório da família.
 * Os construtores dos repositórios são visíveis só neste pacote e exigem a unidade de trabalho <b>em memória</b>:
 * fora daqui não há como montar um repositório em memória ligado a outra transação.
 *
 * <p>O repositório de multas guarda os objetos diretamente e não participa da transação de circulação: a multa é
 * emitida pelo ouvinte de devolução <b>depois</b> do commit (R5).
 */
public final class FabricaEmMemoria implements FabricaDePersistencia {

    private final UnidadeDeTrabalhoEmMemoria unidade = new UnidadeDeTrabalhoEmMemoria();
    private final RepositorioDeLivrosEmMemoria livros = new RepositorioDeLivrosEmMemoria(unidade);
    private final RepositorioDeMovimentacaoEmMemoria movimentacao = new RepositorioDeMovimentacaoEmMemoria(unidade);
    private final RepositorioDeRelacionamentoEmMemoria relacionamento =
            new RepositorioDeRelacionamentoEmMemoria(unidade);
    private final RepositorioDeMultasEmMemoria multas = new RepositorioDeMultasEmMemoria();

    @Override
    public UnidadeDeTrabalho unidadeDeTrabalho() {
        return unidade;
    }

    @Override
    public RepositorioDeLivros livros() {
        return livros;
    }

    @Override
    public RepositorioDeMovimentacao movimentacao() {
        return movimentacao;
    }

    @Override
    public RepositorioDeRelacionamento relacionamento() {
        return relacionamento;
    }

    @Override
    public RepositorioDeMultas multas() {
        return multas;
    }

    /** Usado pelo teste R08 para comprovar que todos os repositórios compartilham a mesma unidade de trabalho. */
    public boolean repositoriosCompartilhamAUnidade() {
        return livros.unidadeDeTrabalho() == unidade
                && movimentacao.unidadeDeTrabalho() == unidade
                && relacionamento.unidadeDeTrabalho() == unidade;
    }
}
