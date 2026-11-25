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
├── interfaceusuario/      CLI (só chama tratadores)
└── config/                Aplicacao (composition root), PerfilDeExecucao, ConfiguracaoAdministrativa
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

## R2 — Strategy
| Papel | Classe |
|---|---|
| Strategy | `PoliticaDeMulta { Dinheiro calcular(long diasAtraso); String codigo(); }` |
| ConcreteStrategy | `PoliticaOriginal` (R$1/dia), `PoliticaComTeto` (min(dias×1, 20)), `PoliticaComTolerancia` (max(0, dias−2)×1) |
| Context | `ServicoDeMultas` (obtém a política vigente de `ConfiguracaoAdministrativa`) |

- Dias de atraso: `ChronoUnit.DAYS.between(vencimento.toLocalDate(), devolucao.toLocalDate())`, mínimo 0.
- Dinheiro: `BigDecimal` escala 2 (ou record `Dinheiro(long centavos)`). Nunca `float`.
- `Multa` grava `valor` **e** `codigoPolitica` na emissão → trocar a política depois não recalcula.
- Nova política = nova classe + registro na configuração; `ServicoDeMultas` intocado (alteração ao vivo).

## R3 — Adapter
| Papel | Classe |
|---|---|
| Target | `ServicoDeCobranca { ReciboCobranca emitir(SolicitacaoCobranca); NotificacaoPagamento traduzir(Object notificacaoBruta); }` |
| Adaptee | `BancoAApi` (valor em **centavos `long`**, CPF só dígitos, status `int`), `BancoBApi` (valor **`"7,00"`/BigDecimal**, CPF formatado, status `String`) |
| Adapter | `AdaptadorBancoA`, `AdaptadorBancoB` (todas as conversões de valor, documento, identificador e situação) |
| Client | `ServicoDeMultas` (sem nenhum `if` por banco) |

- Banco da instalação escolhido em `config.Aplicacao` (propriedade `banco=A|B`).
- Idempotência: `ServicoDeMultas.registrarPagamento(NotificacaoPagamento)` ignora se a multa já está `QUITADA` ou se o
  `idNotificacao` já foi processado.

## R4 — Facade
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

## R5 — Observer
| Papel | Classe |
|---|---|
| Subject | `PublicadorDeDevolucao` (`registrar`, `remover`, `publicar`) |
| Observer | `OuvinteDeDevolucao { void aoDevolver(EventoDevolucao) }` |
| ConcreteObserver | `OuvinteInspecao` (enfileira p/ inspeção), `OuvinteMulta` (→ ServicoDeMultas), `OuvinteIndicadores`, `OuvinteEmail` |
| Evento | `record EventoDevolucao(idLivro, idEstudante, idEmprestimo, LocalDate vencimento, LocalDate dataDevolucao)` |

- A fachada publica **depois de `uow.confirmar()`**. `publicar` faz `try/catch` **por ouvinte** e registra a falha.
- Livro continua `DEVOLVIDO`; só `inspecionar(...)` muda o estado.

## R6 — Command
| Papel | Classe |
|---|---|
| Command | `SolicitacaoDeCirculacao` (abstrata: `UUID id`, `String autor`, `Instant criadaEm`, `abstract Comprovante executar(FachadaDeCirculacao)`) |
| ConcreteCommand | `SolicitacaoDeReserva`, `SolicitacaoDeRenovacao` |
| Invoker | `ExecutorDeSolicitacoes` (`ConcurrentHashMap<UUID, CompletableFuture<Comprovante>>.computeIfAbsent` → 2 submissões simultâneas com o mesmo id executam uma vez) |
| Receiver | `FachadaDeCirculacao` → domínio |
| Client | CLI cria a solicitação → `TratadorDeEmprestimo.submeter(solicitacao)` → executor |

Sem `desfazer`: circulação é fato registrado com efeitos externos (e-mail, boleto); correção é nova operação de domínio.

## R7 — Template Method
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

## R8 — Abstract Factory
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

## R9 — Decorator
| Papel | Classe |
|---|---|
| Component | `ConsultaAcervo { MetadadosLivro consultarMetadados(long idLivro); List<MetadadosLivro> buscarPorTitulo(String) }` |
| ConcreteComponent | `ConsultaAcervoPadrao` (original, sem alterações) |
| Decorator | `DecoradorDeConsulta` (abstrato, guarda `ConsultaAcervo interna`) |
| ConcreteDecorator | `ConsultaComMedicao` (conta chamadas/tempo em `Medidor`), `ConsultaComCache` (só `MetadadosLivro`; se o cache falhar → delega) |

- Ordem: `new ConsultaComMedicao(new ConsultaComCache(new ConsultaAcervoPadrao(...)))` → medição por fora, então
  acerto de cache **também é medido**.
- `MetadadosLivro` **não tem estado** do livro. A fachada, ao reservar, lê o `Livro` do repositório (estado atual).

## R10 — Composite
| Papel | Classe |
|---|---|
| Component | `NoDoAcervo` (abstrato: `int contarLivros()`, `List<Long> listarLivros()`) |
| Composite | `GrupoDoAcervo` (`TipoGrupo` UNIDADE/SECAO/SUBSECAO; `adicionar`, `remover`, filhos) |
| Leaf | `ItemDoAcervo(long idLivro)` |
| Gerência | `OrganizacaoDoAcervo` (raiz + índice `idLivro → grupo`; `mover(idLivro, destino)`) |

- `adicionar` recusa: grupo que é ancestral do destino (ciclo) e item cujo `idLivro` já está em outro grupo.
- `mover` só realoca o `ItemDoAcervo`; não acessa `Livro` nem `Movimentacao`.

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
| R10 | `R10_unidadeComDuasSecoesTotalCinco`, `R10_moverLivroNaoAlteraIdEstadoNemEmprestimo` |
