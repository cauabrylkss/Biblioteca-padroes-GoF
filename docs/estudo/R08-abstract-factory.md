# R8 — Abstract Factory: guia de estudo para a arguição

Código: `src/main/java/br/pucpr/biblioteca/persistencia/` (`FabricaDePersistencia`, `UnidadeDeTrabalho`,
`Repositorio*`) e `persistencia/memoria/` (`FabricaEmMemoria`, `UnidadeDeTrabalhoEmMemoria`, `TabelaEmMemoria`,
`Repositorio*EmMemoria`) · Escolha: `config/Aplicacao.criarFabrica` + `PerfilDeExecucao` ·
Testes: `src/test/java/.../persistencia/R08AbstractFactoryTest.java` · **Família relacional (MySQL): Fase 3.**

## A ideia em uma frase
Uma fábrica cria **a família inteira** de acesso a dados (transação + repositórios) de um tipo só — memória ou MySQL —,
e o sistema escolhe a fábrica **uma vez**, pelo perfil; ninguém mais sabe qual família está em uso.

## Papéis
| Papel GoF | Classe |
|---|---|
| AbstractFactory | `FabricaDePersistencia` (`unidadeDeTrabalho()`, `livros()`, `movimentacao()`, `relacionamento()`, `multas()`) |
| ConcreteFactory | `FabricaEmMemoria` (perfil `teste`/`memoria`); `FabricaRelacional` (perfil `producao`, Fase 3) |
| AbstractProduct | `UnidadeDeTrabalho`, `RepositorioDeLivros`, `RepositorioDeMovimentacao`, `RepositorioDeRelacionamento` |
| ConcreteProduct | `UnidadeDeTrabalhoEmMemoria`, `RepositorioDeLivrosEmMemoria`, `RepositorioDeMovimentacaoEmMemoria`, `RepositorioDeRelacionamentoEmMemoria` |

## Perguntas do PDF
**Onde a família é escolhida?**
Em um único lugar: `Aplicacao.criarFabrica(perfil)` (um `switch` sobre `PerfilDeExecucao`, lido de `perfil=` no
`application.properties` ou `-Dperfil=`). Daí para frente `Acervo`, `Movimentacao` e `Relacionamento` recebem só as
interfaces (`R08_perfilTesteSoObjetosEmMemoria`).

**Como os repositórios compartilham a transação?**
A fábrica cria **uma** unidade de trabalho e passa **a mesma** no construtor de cada repositório. Na memória: ao
`iniciar()`, cada `TabelaEmMemoria` tira uma foto do seu mapa; `desfazer()` restaura todas, `confirmar()` descarta as
fotos (`R08_desfazerDescartaTudoQueOsRepositoriosGravaramNaTransacao`). Uma trava única (`ReentrantLock`) impede que
duas transações se misturem. No MySQL (Fase 3): uma única `Connection` com `autoCommit=false` compartilhada.

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
  ouvinte, depois do commit da devolução.
