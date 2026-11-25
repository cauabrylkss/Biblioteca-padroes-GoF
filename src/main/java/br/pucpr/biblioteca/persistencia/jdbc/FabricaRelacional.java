package br.pucpr.biblioteca.persistencia.jdbc;

import br.pucpr.biblioteca.financeiro.RepositorioDeMultas;
import br.pucpr.biblioteca.persistencia.FabricaDePersistencia;
import br.pucpr.biblioteca.persistencia.RepositorioDeLivros;
import br.pucpr.biblioteca.persistencia.RepositorioDeMovimentacao;
import br.pucpr.biblioteca.persistencia.RepositorioDeRelacionamento;
import br.pucpr.biblioteca.persistencia.UnidadeDeTrabalho;

/**
 * Papel GoF: <b>ConcreteFactory</b> (Abstract Factory, R8) — família relacional (MySQL via JDBC, perfil
 * {@code producao}).
 *
 * <p>Cria <b>uma</b> {@link UnidadeDeTrabalhoJdbc} e a entrega, no construtor, a cada repositório da família — todos
 * usam a mesma conexão e, portanto, a mesma transação. Os construtores dos repositórios exigem a unidade
 * <b>JDBC</b> e só são visíveis neste pacote: não há como ligar um repositório relacional à transação em memória
 * (nem o contrário).
 *
 * <p>Criar a fábrica não conecta no banco (conexão preguiçosa). O Oracle, réplica somente leitura, não tem
 * repositório nem conexão de escrita aqui.
 */
public final class FabricaRelacional implements FabricaDePersistencia {

    private final UnidadeDeTrabalhoJdbc unidade;
    private final RepositorioDeLivrosJdbc livros;
    private final RepositorioDeMovimentacaoJdbc movimentacao;
    private final RepositorioDeRelacionamentoJdbc relacionamento;
    private final RepositorioDeMultasJdbc multas;

    public FabricaRelacional(ConfiguracaoDoBanco configuracao) {
        this.unidade = new UnidadeDeTrabalhoJdbc(configuracao);
        this.livros = new RepositorioDeLivrosJdbc(unidade);
        this.movimentacao = new RepositorioDeMovimentacaoJdbc(unidade);
        this.relacionamento = new RepositorioDeRelacionamentoJdbc(unidade);
        this.multas = new RepositorioDeMultasJdbc(unidade);
    }

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
                && relacionamento.unidadeDeTrabalho() == unidade
                && multas.unidadeDeTrabalho() == unidade;
    }

    /** A conexão com o MySQL já foi aberta? (montar a família não conecta) */
    public boolean conexaoAberta() {
        return unidade.conexaoAberta();
    }

    /** Esvazia todas as tabelas (opção {@code --reiniciar-dados} da CLI e teste de integração). */
    public void apagarTodosOsDados() {
        unidade.apagarTodosOsDados();
    }
}
