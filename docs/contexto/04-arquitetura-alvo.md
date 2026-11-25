# 04 — Arquitetura-alvo e desenho dos 10 padrões

Princípios (vêm do enunciado, não negociáveis):
1. **Interface (CLI/Web) → Tratadores → Fachada/Serviços → Pontos de acesso (Acervo, Relacionamento, Movimentacao) → Domínio.**
   A interface **só** conhece tratadores. Tratadores **não** têm `if` sobre o estado do livro.
2. Regras de domínio ficam no domínio (estados, empréstimo, multa). Comandos, fachada e ouvintes só orquestram.
3. Toda persistência passa pela `FabricaDePersistencia` escolhida **uma vez** pelo perfil.
4. Os nomes `iLivro`, `iPessoa`, `iEstudante`, `iAtendente`, `iEmprestimo`, `iDevolucao`, `TratadorDe*`, `Acervo`,
   `Relacionamento`, `Movimentacao` **são mantidos** (exigência do Trabalho 02/R7).

## Stack
- JDK 25 instalado, compilando com `maven.compiler.release=21` (≥17 exigido; ver E-01), **Maven** com **Maven Wrapper** (`mvnw`) → comando único `./mvnw test` / `mvnw.cmd test`.
- Interface: **CLI** (D-01).
- JUnit 5 + AssertJ. MySQL Connector/J para a família relacional. Sem Spring (menos mágica para explicar na arguição).
- Composition root manual (`config.Aplicacao`), sem framework de injeção.

## Pacotes = componentes do diagrama de componentes
```
br.pucpr.biblioteca
├── biblioteca/            Componente Biblioteca
│   ├── livro/             Livro, EstadoLivro + 7 estados (R1), SituacaoLivro (código persistido)
│   ├── acervo/            Acervo (ponto de acesso), ConsultaAcervo + decoradores (R9), árvore do acervo (R10)
│   ├── movimentacao/      Movimentacao (ponto de acesso), Transacao, Reserva, Emprestimo, Devolucao
│   ├── circulacao/        FachadaDeCirculacao + comprovantes (R4), solicitações/executor (R6),
│   │                      PublicadorDeDevolucao/EventoDevolucao/ouvintes (R5)
│   └── tratadores/        TratadorDeEmprestimo (iEmprestimo), TratadorDeDevolucao (iDevolucao), TratadorDeAcervo (iLivro)
├── rh/                    Pessoa, Papel, Atendente, Relacionamento (ponto de acesso), TratadorDePessoa (iPessoa),
│                          TratadorDeAtendente (iAtendente)
├── academico/             Estudante, TratadorDeEstudante (iEstudante)
├── cadastro/              CadastroTemplate (R7) + CadastroDeLivro/Pessoa/Estudante/Atendente
├── financeiro/            Multa (EMITIDA→QUITADA), Boleto (EMITIDO→PAGO, da atividade UML), ServicoDeMultas, PoliticaDeMulta + 3 políticas (R2),
│   └── cobranca/          ServicoDeCobranca (Target), SolicitacaoCobranca, NotificacaoPagamento,
│                          AdaptadorBancoA, AdaptadorBancoB (R3)
├── banco/                 <<external>> simulados: BancoAApi, BancoBApi (Adaptees, APIs incompatíveis)
├── persistencia/          FabricaDePersistencia, UnidadeDeTrabalho, Repositorio* (R8)
│   ├── memoria/           FabricaEmMemoria + repositórios/UoW em memória
│   └── jdbc/              FabricaRelacional (MySQL) + repositórios/UoW JDBC, schema.sql
├── interfaceusuario/      InterfaceCli, EntradaSaida (roteiro), Tratadores, OpcoesDaLinhaDeComando (só chama tratadores)
└── config/                Aplicacao (composition root), PerfilDeExecucao, BancoDaInstalacao, DadosDeExemplo,
                           iAdministracao/TratadorDeAdministracao (Administrador + ferramentas da demonstração)
```
Decisão de modelagem: **um único `Relacionamento`** (no RH), exposto por `iPessoa`/`iAtendente`; o Acadêmico
requer `iPessoa`. Corrige a duplicação C5.

---

## R1 — State
| Papel GoF | Classe |
|---|---|
| Context | `Livro` (campo `private EstadoLivro estado`) |
| State | `EstadoLivro` (classe abstrata: todo evento por padrão lança `TransicaoInvalidaException` **sem tocar no livro**) |
| ConcreteState | `Livre`, `Reservado`, `Emprestado`, `Renovado`, `Devolvido`, `Reparando`, `Incinerado` |

Eventos (1 método por transição na classe do estado de origem — ver máquina-alvo em `03-revisao-uml.md`) — ✅ implementado:
`reservar(Livro)`, `emprestar(Livro, Reserva, idEstudante)`, `renovar(Livro, Emprestimo)`,
`registrarDevolucao(Livro)`, `inspecionar(Livro, boolean emBomEstado)`, `enviarParaReparo(Livro)`,
`retornarDoReparo(Livro)`, `registrarLaudoIrrecuperavel(Livro)`, `excluir(Livro)`.
Classes de estado são package-private e singletons (`INSTANCIA`); fora do pacote só se vê `Livro`, `EstadoLivro`
(tipo), `SituacaoLivro` e `TransicaoInvalidaException`.

- Guarda da renovação em `Emprestado.renovar` / `Renovado.renovar`: **primeiro** `if (emprestimo.getNumRenovacoes() >= 2) throw`,
  **depois** `emprestimo.prorrogar(30)` e troca de estado → a recusa nunca altera o vencimento.
- `Livro` expõe apenas os eventos (`livro.renovar(emp)` → `estado.renovar(this, emp)`); `mudarPara(EstadoLivro)` é
  **package-private** (só os estados chamam).
- Persistência: grava `SituacaoLivro` (enum: `LIVRE`…`INCINERADO`) via `estado.situacao()`; reconstitui com
  `EstadoLivro.de(SituacaoLivro)` (fábrica estática — estados são *stateless*, podem ser singletons).
- `Devolvido.reservar(...)` não é sobrescrito → recusado (cenário R1-b).

## R2 — Strategy  ✅ implementado (`financeiro`)
| Papel | Classe |
|---|---|
| Strategy | `PoliticaDeMulta { Dinheiro calcular(long diasAtraso); String codigo(); }` |
| ConcreteStrategy | `PoliticaOriginal` (R$1/dia), `PoliticaComTeto` (min(dias×1, 20)), `PoliticaComTolerancia` (max(0, dias−2)×1) |
| Context | `ServicoDeMultas` (obtém a política vigente de `ConfiguracaoAdministrativa`) |

- Dias de atraso: `ChronoUnit.DAYS.between(vencimento.toLocalDate(), devolucao.toLocalDate())`, mínimo 0.
- Dinheiro: `BigDecimal` escala 2 (ou record `Dinheiro(long centavos)`). Nunca `float`.
- `Multa` grava `valor` **e** `codigoPolitica` na emissão → trocar a política depois não recalcula.
- Nova política = nova classe + registro na configuração; `ServicoDeMultas` intocado (alteração ao vivo).
- Implementado: `ServicoDeMultas.emitirSeHouverAtraso(idEmprestimo, idEstudante, vencimento, dataDevolucao)` devolve
  `Optional<Multa>` (vazio quando o valor é zero — no prazo ou dentro da tolerância). Armazenamento das multas e boleto
  entram no R3; a chamada pelo ouvinte de multa, no R5. `PoliticaDeMulta.UM_REAL_POR_DIA` e
  `PoliticaDeMulta.exigirDiasNaoNegativos` evitam que uma política dependa de outra.

## R3 — Adapter  ✅ implementado (`financeiro.cobranca`, `banco`)
| Papel | Classe |
|---|---|
| Target | `ServicoDeCobranca { ReciboCobranca emitir(SolicitacaoCobranca); NotificacaoPagamento traduzir(Object notificacaoBruta); }` |
| Adaptee | `BancoAApi` (valor em **centavos `long`**, CPF só dígitos, status `int`), `BancoBApi` (valor **`"7,00"`/BigDecimal**, CPF formatado, status `String`) |
| Adapter | `AdaptadorBancoA`, `AdaptadorBancoB` (todas as conversões de valor, documento, identificador e situação) |
| Client | `ServicoDeMultas` (sem nenhum `if` por banco) |

- Banco da instalação escolhido em `config.Aplicacao` (propriedade `banco=A|B`).
- Idempotência: `ServicoDeMultas.registrarPagamento(String notificacaoBruta)` (traduz pelo adaptador) ignora se o
  `idNotificacao` já foi processado **ou** se a multa já está `QUITADA`; método `synchronized`.
- Implementado: Target `registrar(SolicitacaoCobranca) → CobrancaRegistrada` e `traduzirNotificacao(String) →
  NotificacaoPagamento`. Formatos — A: centavos `long`, CPF 11 dígitos, nosso número/código `long`, status `1/2/9`,
  notificação `evento=…;titulo=…;valorCentavos=…;status=…`; B: `"7,00"`, CPF `000.000.000-00`, referência
  `MULTA-<id>`/id `B-000001`, `ABERTO/LIQUIDADO/BAIXADO`, notificação `SITUACAO|id|valor|evento`.
- `Boleto` (EMITIDO → PAGO) pertence à `Multa`; `ConsultaDePagador` é a porta pela qual o Financeiro obtém nome/CPF do
  estudante (Financeiro requer RH no diagrama de componentes). `RepositorioDeMultas` (memória por ora; família JDBC na
  Fase 3). `BancoDaInstalacao.criar(banco)` devolve o adaptador + um `SimuladorDePagamento` para a CLI.

## R4 — Facade  ✅ implementado (`biblioteca.circulacao`, `biblioteca.tratadores`)
| Papel | Classe |
|---|---|
| Facade | `FachadaDeCirculacao` |
| Subsistemas | `Relacionamento`, `Movimentacao`, `Acervo`, `Livro` (+ `UnidadeDeTrabalho`) |
| Clientes | `TratadorDeEmprestimo`, `TratadorDeDevolucao` |

Operações → comprovantes (records imutáveis): `reservar → ComprovanteReserva`, `efetivarEmprestimo → ComprovanteEmprestimo`,
`renovar → ComprovanteRenovacao`, `registrarDevolucao → ComprovanteDevolucao`, `inspecionar → ComprovanteInspecao`.

`efetivarEmprestimo(idLivro, idEstudante, idAtendente)`: `uow.iniciar()` → Relacionamento valida estudante/atendente →
Movimentacao busca **reserva ativa** do par → Acervo busca Livro → `livro.emprestar(idEstudante)` (State) →
Movimentacao remove reserva e insere `Emprestimo` (vencimento +N dias) → repositório salva Livro → `uow.confirmar()`.
Qualquer exceção → `uow.desfazer()` (nada fica parcial).

- Implementado: todas as operações de circulação (C-01) + consultas (`consultarReserva/Emprestimo/Devolucao`), todas
  devolvendo `Comprovante` (interface `sealed`) ou `Optional` de comprovante. Inspeção, reparo e laudo devolvem um único
  record `ComprovanteDeSituacao(operacao, idLivro, situacaoAnterior, situacaoLivro, data)` (em vez de um
  `ComprovanteInspecao` próprio). `efetivarEmprestimo` busca a reserva **do livro** e deixa a guarda
  "mesmo estudante" para `Reservado.emprestar` (R1). Só o estudante do empréstimo renova/devolve
  (`Emprestimo.exigirEstudante`). Transação: `UnidadeDeTrabalho.executar(...)` (try → confirmar; catch → desfazer).
  Laudo irrecuperável também retira o livro da árvore do acervo (B-19).

## R5 — Observer  ✅ implementado (`biblioteca.circulacao`)
| Papel | Classe |
|---|---|
| Subject | `PublicadorDeDevolucao` (`registrar`, `remover`, `publicar`) |
| Observer | `OuvinteDeDevolucao { void aoDevolver(EventoDevolucao) }` |
| ConcreteObserver | `OuvinteInspecao` (enfileira p/ inspeção), `OuvinteMulta` (→ ServicoDeMultas), `OuvinteIndicadores`, `OuvinteEmail` |
| Evento | `record EventoDevolucao(idLivro, idEstudante, idEmprestimo, LocalDate vencimento, LocalDate dataDevolucao)` |

- A fachada publica **depois de `uow.confirmar()`**. `publicar` faz `try/catch` **por ouvinte** e registra a falha.
- Livro continua `DEVOLVIDO`; só `inspecionar(...)` muda o estado.
- Implementado: `publicar` devolve a lista de falhas, que vira `ComprovanteDevolucao.avisos`. `FilaDeInspecao` guarda
  as pendências (o ouvinte acrescenta; `FachadaDeCirculacao.inspecionar` retira). `OuvinteMulta` chama
  `iMulta.emitirSeHouverAtraso` (Financeiro → Biblioteca, C-15; realizada por `ServicoDeMultas`). `OuvinteEmail` envia
  o aviso de devolução por `ServicoDeEmail` (simulado, `email.falhar=true` força falha). `OuvinteIndicadores` conta
  devoluções, atrasadas e dias de atraso (B-21). Registro em `config.Aplicacao.registrarOuvintes()`.

## R6 — Command  ✅ implementado (`biblioteca.circulacao`)
| Papel | Classe |
|---|---|
| Command | `SolicitacaoDeCirculacao` (abstrata: `UUID id`, `String autor`, `Instant criadaEm`, `abstract Comprovante executar(FachadaDeCirculacao)`) |
| ConcreteCommand | `SolicitacaoDeReserva`, `SolicitacaoDeRenovacao` |
| Invoker | `ExecutorDeSolicitacoes` (`ConcurrentHashMap<UUID, CompletableFuture<Comprovante>>.computeIfAbsent` → 2 submissões simultâneas com o mesmo id executam uma vez) |
| Receiver | `FachadaDeCirculacao` → domínio |
| Client | CLI cria a solicitação → `TratadorDeEmprestimo.submeter(solicitacao)` → executor |

Sem `desfazer`: circulação é fato registrado com efeitos externos (e-mail, boleto); correção é nova operação de domínio.
- Implementado: `ExecutorDeSolicitacoes` usa `putIfAbsent` de um `CompletableFuture` (a execução acontece **fora** do
  mapa, então ids diferentes não se bloqueiam); repetição devolve o mesmo comprovante ou a mesma exceção (C-03).
  `iEmprestimo.solicitarReserva(SolicitacaoDeReserva)` e `renovarEmprestimo(SolicitacaoDeRenovacao)` mantêm os nomes
  do Trabalho 02 recebendo o objeto-comando. Fábricas `SolicitacaoDeX.nova(autor, relogio, idLivro, idEstudante)`.

## R7 — Template Method  ✅ implementado (`cadastro`)
```java
public abstract class CadastroTemplate<D, T> {
    public final Resultado<T> incluir(D dados) {          // método-modelo, final
        Optional<T> existente = buscarExistente(dados);
        if (existente.isPresent()) return apresentar(existente.get(), false);
        validar(dados);
        verificarPreRequisitos(dados);                   // falha aqui → nada foi gravado
        T novo = criar(dados);
        armazenar(novo);
        aposArmazenar(novo);                             // GANCHO (default vazio)
        return apresentar(novo, true);
    }
    protected abstract Optional<T> buscarExistente(D d);
    protected abstract void validar(D d);
    protected void verificarPreRequisitos(D d) {}        // gancho com padrão vazio
    protected abstract T criar(D d);
    protected abstract void armazenar(T t);
    protected Resultado<T> apresentar(T t, boolean novo) { return new Resultado<>(t, novo); }
    protected void aposArmazenar(T t) {}
}
```
Subclasses: `CadastroDeLivro`, `CadastroDePessoa`, `CadastroDeEstudante` (pré-requisito: Pessoa com o CPF existe),
`CadastroDeAtendente` (idem). `TratadorDeAcervo implements iLivro`, `TratadorDePessoa implements iPessoa`, etc. delegam
`inserir*` ao cadastro correspondente.

Implementado com uma diferença: o gancho final recebe também os dados de entrada — `aposArmazenar(D dados, E entidade)`
—, porque o `CadastroDeLivro` precisa do grupo informado para catalogar o livro (R10). `CadastroDeLivro` usa os dois
ganchos (`verificarPreRequisitos`: grupo existe; `aposArmazenar`: catalogar). Estudante/Atendente recebem o **CPF** da
pessoa (`inserirEstudante(String cpf)`); validação de CPF com dígitos verificadores em `rh.Cpf` (B-23). Tratadores
devolvem fichas imutáveis (`FichaDoLivro`, `FichaDaPessoa`, `FichaDoPapel`) dentro de `Resultado`, nunca a entidade.

## R8 — Abstract Factory  ✅ implementado (`persistencia`, `persistencia.memoria`, `persistencia.jdbc`)
| Papel | Classe |
|---|---|
| AbstractFactory | `FabricaDePersistencia { UnidadeDeTrabalho unidadeDeTrabalho(); RepositorioDeLivros livros(); RepositorioDeMovimentacao movimentacao(); RepositorioDeRelacionamento relacionamento(); }` |
| ConcreteFactory | `FabricaEmMemoria`, `FabricaRelacional` (MySQL/JDBC) |
| AbstractProduct | `UnidadeDeTrabalho`, `RepositorioDeLivros`, `RepositorioDeMovimentacao`, `RepositorioDeRelacionamento` |
| ConcreteProduct | `*EmMemoria`, `*Jdbc` |

- Escolha **uma vez** em `config.Aplicacao` a partir de `PerfilDeExecucao` (`teste` | `producao`, via `-Dperfil=` ou `application.properties`).
- Cada fábrica cria **uma** UoW e injeta **a mesma** em todos os repositórios dela. Repositórios JDBC recebem
  `UnidadeDeTrabalhoJdbc` no construtor (tipo concreto, construtor package-private) → impossível misturar famílias.
- UoW JDBC com conexão **preguiçosa**: o teste de produção (R08) verifica tipos e a UoW compartilhada sem precisar de MySQL rodando.
- Oracle: não existe repositório/conexão de escrita para ele (só documentado como réplica).
- Implementado (memória): a fábrica também cria o `RepositorioDeMultas` (fora da transação de circulação — a multa
  nasce no ouvinte, após o commit). `UnidadeDeTrabalhoEmMemoria`: ao `iniciar`, cada `TabelaEmMemoria` tira uma foto
  do mapa; `desfazer` restaura, `confirmar` descarta; uma `ReentrantLock` única serializa transações e operações
  avulsas. Os repositórios guardam **records imutáveis** e devolvem objetos novos (como um banco): o estado do
  livro é gravado como `SituacaoLivro` e reconstituído por `Livro.reconstituir`. Perfil: `teste`/`memoria` →
  `FabricaEmMemoria`; `producao` → `FabricaRelacional`.
- Implementado (MySQL): `UnidadeDeTrabalhoJdbc` com **uma** `Connection` aberta no primeiro uso (cria o schema, D-06), trava única como na memória; `iniciar` = `setAutoCommit(false)`, `confirmar` = `COMMIT`, `desfazer` = `ROLLBACK`; fora de transação, auto-commit (multas, após o commit). Tabelas: `livro` (`situacao` = nome do estado), `pessoa`, `estudante`, `atendente` (FK → pessoa), `transacao` (coluna `tipo`), `multa` (com colunas do boleto), `notificacao_processada` (PK = idempotência). Credenciais: `ConfiguracaoDoBanco.doAmbiente` (D-07). `FabricaRelacional.apagarTodosOsDados()` serve ao `--reiniciar-dados` e ao teste de integração.

## R9 — Decorator  ✅ implementado (`biblioteca.acervo`)
| Papel | Classe |
|---|---|
| Component | `ConsultaAcervo { MetadadosLivro consultarMetadados(long idLivro); List<MetadadosLivro> buscarPorTitulo(String) }` |
| ConcreteComponent | `ConsultaAcervoPadrao` (original, sem alterações) |
| Decorator | `DecoradorDeConsulta` (abstrato, guarda `ConsultaAcervo interna`) |
| ConcreteDecorator | `ConsultaComMedicao` (conta chamadas/tempo em `Medidor`), `ConsultaComCache` (só `MetadadosLivro`; se o cache falhar → delega) |

- Ordem: `new ConsultaComMedicao(new ConsultaComCache(new ConsultaAcervoPadrao(...)))` → medição por fora, então
  acerto de cache **também é medido**.
- `MetadadosLivro` **não tem estado** do livro. A fachada, ao reservar, lê o `Livro` do repositório (estado atual).
- Implementado: `ConsultaAcervo.consultarMetadados` devolve `Optional<MetadadosLivro>` (id, título); `livroAlterado(id)`
  (default vazio) atravessa a cadeia e faz o cache descartar o livro — chamado pelo `TratadorDeAcervo` ao alterar o
  título ou excluir (C-10). Cache em `ConcurrentHashMap` (armazenamento trocável no construtor, para o teste de falha),
  contadores `acertos`/`faltas`; falha do armazenamento → log `System.Logger` + delega. `Medidor`: chamadas e tempo
  total por operação. Cadeia montada em `Aplicacao.montarConsultaAcervo` por `consulta.cache`/`consulta.medicao`;
  `iLivro` ganhou `consultarMetadados` e `buscarPorTitulo` (a CLI só fala com tratadores).

## R10 — Composite  ✅ implementado (`biblioteca.acervo`)
| Papel | Classe |
|---|---|
| Component | `NoDoAcervo` (abstrato: `int contarLivros()`, `List<Long> listarLivros()`) |
| Composite | `GrupoDoAcervo` (`TipoGrupo` UNIDADE/SECAO/SUBSECAO; `adicionar`, `remover`, filhos) |
| Leaf | `ItemDoAcervo(long idLivro)` |
| Gerência | `OrganizacaoDoAcervo` (raiz `ACERVO` id 0; `criarGrupo`, `catalogar`, `mover(idLivro, destino)`, `retirar`, `grupoDoLivro`) |

- `adicionar` recusa: grupo que é ele mesmo ou ancestral do destino (ciclo), item cujo `idLivro` já está em qualquer
  grupo da mesma árvore, e nó que já tem pai. As regras ficam **no próprio grupo** (não há índice separado: a busca
  percorre a árvore a partir da raiz com `localizarItem`).
- `mover` só realoca o mesmo `ItemDoAcervo`; se o destino recusar, o item volta à origem. Não acessa `Livro` nem
  `Movimentacao`. `TipoGrupo` é informativo (profundidade variável; `ACERVO` reservado à raiz).

---

## Cenários de teste (nomes exigidos pelo padrão `Rxx_descricao`)
| Req | Teste(s) |
|---|---|
| R1 | `R01_terceiraRenovacaoRecusada`, `R01_devolvidoNaoInspecionadoNaoPodeSerReservado` |
| R2 | `R02_cincoDiasAtrasoPoliticaOriginalCincoReais`, `R02_cincoDiasAtrasoPoliticaToleranciaTresReais`, `R02_semAtrasoMultaZero` |
| R3 | `R03_seteReaisViram700CentavosNoBancoAe7NoBancoB`, `R03_notificacaoRepetidaNaoQuitaDuasVezes` |
| R4 | `R04_efetivarEmprestimoValidaReservaRemoveReservaCriaEmprestimoAtualizaLivro` |
| R5 | `R05_devolucaoAtrasadaAlimentaInspecaoMultaIndicadores`, `R05_devolucaoNoPrazoNaoGeraMulta`, `R05_falhaNoEmailNaoAfetaDemais` |
| R6 | `R06_renovacaoRepetidaMesmoIdAcrescentaSo30Dias`, `R06_solicitacoesDiferentesRespeitamLimiteDeDuas` |
| R7 | `R07_estudanteDuplicadoRetornaExistente`, `R07_cpfNaoCadastradoFalhaAntesDeGravar` |
| R8 | `R08_perfilTesteSoObjetosEmMemoria`, `R08_perfilProducaoRepositoriosMysqlMesmaUnidade` |
| R9 | `R09_segundaConsultaUsaCacheEGeraMedicao`, `R09_cacheAntigoNaoAutorizaReserva` |
| R10 | `R10_unidadeComDuasSecoesTotalCinco`, `R10_moverLivroEntreSecoesNaoAlteraIdEstadoNemEmprestimo` |
