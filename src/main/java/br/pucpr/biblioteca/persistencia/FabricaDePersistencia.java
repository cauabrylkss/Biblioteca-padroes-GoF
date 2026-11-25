package br.pucpr.biblioteca.persistencia;

import br.pucpr.biblioteca.financeiro.RepositorioDeMultas;

/**
 * Papel GoF: <b>AbstractFactory</b> (R8) — cria uma família compatível de objetos de acesso a dados: a unidade de
 * trabalho e os repositórios de Livro, Movimentacao e Relacionamento (e o de multas, do Financeiro).
 *
 * <p>Implementações: {@code FabricaEmMemoria} (perfil de teste) e {@code FabricaRelacional} (MySQL, perfil de
 * produção). A fábrica é escolhida <b>uma única vez</b>, pelo perfil de execução, no composition root
 * ({@code config.Aplicacao}); o resto do sistema só conhece estas interfaces. Os repositórios de uma fábrica recebem a
 * unidade de trabalho concreta da própria família, então não é possível misturar um repositório em memória com um
 * relacional na mesma transação.
 */
public interface FabricaDePersistencia {

    /** A unidade de trabalho da família — sempre a mesma instância, compartilhada pelos repositórios abaixo. */
    UnidadeDeTrabalho unidadeDeTrabalho();

    RepositorioDeLivros livros();

    RepositorioDeMovimentacao movimentacao();

    RepositorioDeRelacionamento relacionamento();

    RepositorioDeMultas multas();
}
