# R8 — Abstract Factory: guia de estudo para a arguição

Código: `src/main/java/br/pucpr/biblioteca/persistencia/` (`FabricaDePersistencia`, `UnidadeDeTrabalho`,
`Repositorio*`) e `persistencia/memoria/` (`FabricaEmMemoria`, `UnidadeDeTrabalhoEmMemoria`, `TabelaEmMemoria`,
`Repositorio*EmMemoria`) e `persistencia/jdbc/` (`FabricaRelacional`, `UnidadeDeTrabalhoJdbc`, `Repositorio*Jdbc`,
`ConfiguracaoDoBanco`; tabelas em `src/main/resources/br/pucpr/biblioteca/persistencia/jdbc/schema.sql`) ·
Escolha: `config/Aplicacao.criarFabrica` + `PerfilDeExecucao` ·
Testes: `R08AbstractFactoryTest.java` (sem banco, roda no `mvnw test`) e `R08MySqlIntegracaoTest.java` (MySQL real,
`mvnw test -Pmysql`).

## A ideia em uma frase
Uma fábrica cria **a família inteira** de acesso a dados (transação + repositórios) de um tipo só — memória ou MySQL —,
e o sistema escolhe a fábrica **uma vez**, pelo perfil; ninguém mais sabe qual família está em uso.

## Papéis
| Papel GoF | Classe |
|---|---|
| AbstractFactory | `FabricaDePersistencia` (`unidadeDeTrabalho()`, `livros()`, `movimentacao()`, `relacionamento()`, `multas()`) |
| ConcreteFactory | `FabricaEmMemoria` (perfil `teste`/`memoria`); `FabricaRelacional` (perfil `producao`, MySQL) |
| AbstractProduct | `UnidadeDeTrabalho`, `RepositorioDeLivros`, `RepositorioDeMovimentacao`, `RepositorioDeRelacionamento` (+ `RepositorioDeMultas`) |
| ConcreteProduct (memória) | `UnidadeDeTrabalhoEmMemoria`, `RepositorioDeLivrosEmMemoria`, `RepositorioDeMovimentacaoEmMemoria`, `RepositorioDeRelacionamentoEmMemoria`, `RepositorioDeMultasEmMemoria` |
| ConcreteProduct (MySQL) | `UnidadeDeTrabalhoJdbc`, `RepositorioDeLivrosJdbc`, `RepositorioDeMovimentacaoJdbc`, `RepositorioDeRelacionamentoJdbc`, `RepositorioDeMultasJdbc` |

## Perguntas do PDF
**Onde a família é escolhida?**
Em um único lugar: `Aplicacao.criarFabrica(perfil, urlPadrao)` (um `switch` sobre `PerfilDeExecucao`, lido de `perfil=` no
`application.properties` ou `-Dperfil=`). Daí para frente `Acervo`, `Movimentacao` e `Relacionamento` recebem só as
interfaces (`R08_perfilTesteSoObjetosEmMemoria`, `R08_perfilProducaoRepositoriosMysqlMesmaUnidade`).

**Como os repositórios compartilham a transação?**
A fábrica cria **uma** unidade de trabalho e passa **a mesma** no construtor de cada repositório. Na memória: ao
`iniciar()`, cada `TabelaEmMemoria` tira uma foto do seu mapa; `desfazer()` restaura todas, `confirmar()` descarta as
fotos (`R08_desfazerDescartaTudoQueOsRepositoriosGravaramNaTransacao`). Uma trava única (`ReentrantLock`) impede que
duas transações se misturem. No MySQL: a `UnidadeDeTrabalhoJdbc` tem **uma** `Connection`, e todos os repositórios
JDBC a usam; `iniciar()` faz `setAutoCommit(false)`, `confirmar()` = `COMMIT`, `desfazer()` = `ROLLBACK` de tudo o
que os repositórios gravaram (`R08_mysqlDesfazerEhRollbackDeTodosOsRepositorios`,
`R08_mysqlEmprestimoRecusadoNaoAlteraOBanco`). A mesma trava única serializa o uso da conexão.

**O que impede misturar repositório em memória com relacional?**
Os construtores dos repositórios em memória são **package-private** e exigem `UnidadeDeTrabalhoEmMemoria` (o tipo
concreto). Só a `FabricaEmMemoria`, no mesmo pacote, consegue criá-los — e ela só tem a sua unidade de trabalho. O
mesmo vale para a família JDBC. E a fábrica é escolhida uma vez só, no composition root.

## Perguntas prováveis além do PDF
- **Por que os repositórios devolvem cópias?** Para se comportarem como um banco: alterar um objeto lido sem salvar
  não muda nada (`R08_objetoLidoAlteradoSemSalvarNaoMudaOsDados`) e o desfazer é confiável.
- **Como o estado do livro é gravado e reconstituído?** Grava-se o nome (`SituacaoLivro`); na leitura,
  `Livro.reconstituir` → `EstadoLivro.de(situacao)` devolve o objeto de estado
  (`R08_estadoDoLivroGravadoPeloNomeEReconstituidoNaLeitura`).
- **E o Oracle?** É réplica somente leitura: não existe fábrica nem repositório de escrita para ele.
- **Multas também estão na fábrica?** Sim (`multas()`), mas fora da transação de circulação — a multa nasce no
  ouvinte, depois do commit da devolução. No MySQL ela usa a mesma conexão, em auto-commit (sem transação aberta).
- **Como o teste do perfil de produção passa sem MySQL?** A conexão é **preguiçosa**: criar a fábrica (e montar a
  aplicação inteira) não conecta; a primeira operação é que abre a conexão. O teste confere tipos, pacote e unidade
  compartilhada e que `conexaoAberta()` continua `false`. O uso real do banco está no teste `@Tag("mysql")`.
- **Quem cria as tabelas?** A própria aplicação, na primeira conexão: executa o `schema.sql` (`CREATE TABLE IF NOT
  EXISTS`, D-06). O banco e o usuário são criados uma vez por `docs/mysql/criar-banco.sql`.
- **Onde fica a senha?** Na variável de ambiente `BIBLIOTECA_DB_SENHA` (D-07), nunca no Git;
  `ConfiguracaoDoBanco.toString()` esconde a senha.
- **Por que uma tabela só para reserva/empréstimo/devolução?** Espelha a hierarquia `Transacao` (sealed) com uma coluna
  `tipo`; é o mesmo formato do repositório em memória. As regras ("empréstimo ativo = sem devolução") são as mesmas
  nas duas famílias.
- **Por que só papel → pessoa tem chave estrangeira?** Transações e multas são histórico: continuam valendo depois que
  um livro ou estudante é excluído. Excluir pessoa com papel já é recusado pelo `Relacionamento`.
- **Dinheiro no banco?** `DECIMAL(10,2)` ↔ `BigDecimal` — nunca `double`.
- **Quitação repetida no MySQL?** `notificacao_processada` tem chave primária (banco, id do evento) e o `INSERT IGNORE`
  diz se é a primeira vez — vale mesmo depois de reiniciar a aplicação (teste de integração).
