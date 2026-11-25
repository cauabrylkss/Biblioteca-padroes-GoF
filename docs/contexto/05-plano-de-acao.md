# 05 — Plano de ação

**Hoje: ter 24/11/2026 (noite). Entrega: sex 27/11/2026, 09h40.** ≈ 2,5 dias úteis. O plano prioriza o que pesa
mais na nota: padrões corretos (40%) e testes passando (20%) primeiro; UML (15%) em paralelo; docs (10%) no fim.

## Situação do ambiente (verificado em 24/11)
- ✅ **JDK 25** (Microsoft, `C:\Program Files\Microsoft\jdk-25.0.5.102-hotspot`) e **Maven 3.10.2**
  (`C:\Program Files\Apache\maven\apache-maven-3.10.2`). Terminais abertos antes da instalação ainda veem o Java 8 —
  abrir terminal novo. **`JAVA_HOME` não está definido** (definir).
- ✅ **MySQL** (serviço `MySQL`, `localhost:3306`) + Workbench. Sem Docker (não é necessário).
- ✅ `JAVA_HOME` definido; release 21, Maven Wrapper, `br.pucpr.biblioteca`, JUnit 5 + AssertJ decididos.
- ✅ Astah UML instalado (`C:\Program Files\astah-UML`) — exportação PNG/PDF e leitura do modelo via `tools/astah/extrair.sh`.
- Repo: só README, `docs/`, `tools/` e CLAUDE.md (nada commitado além do commit inicial).

## Decisões
Todas as decisões (decididas e pendentes, com recomendação) estão em **`06-decisoes.md`**. Decididas: CLI, Maven,
JDK 25, diagramas de classes relevados (PR1).

---

## Fase 0 — Fundação (ter 24/11 noite → qua 25/11 manhã cedo) · ~2h
- [x] Instalar JDK e Maven.
- [x] Definir `JAVA_HOME` (E-02). [x] Instalar MySQL (D-05).
- [x] Gerar projeto Maven (`pom.xml`, JUnit 5, AssertJ, mysql-connector-j, shade → `target/biblioteca.jar`, exec) + **Maven Wrapper**; `./mvnw test` verde (`FundacaoTest`).
- [x] Estrutura de pacotes de `04-arquitetura-alvo.md` (com `package-info.java`), `application.properties`, `docs/mysql/criar-banco.sql`, README. *(`schema.sql` movido para a Fase 3, junto da família JDBC.)*
- [x] Branch `fase0/fundacao`. [ ] Commit/PR para `main` (aguardando pedido da equipe).
- [x] Fechar as decisões 🔴 de `06-decisoes.md`.

## Fase 1 — Núcleo do domínio (qua 25/11 manhã) · padrões sem dependências
Cada item = classes + teste `Rxx_` verde.
- [x] **R1 State**: `Livro`, `EstadoLivro` + 7 estados, `SituacaoLivro`, `TransicaoInvalidaException`, `Emprestimo.prorrogar` (branch `r01-state`, 19 testes).
- [x] **R2 Strategy**: `PoliticaDeMulta` + 3, `ServicoDeMultas` (parte de cálculo), `ConfiguracaoAdministrativa`, `Multa` (branch `r02-strategy`, 7 testes).
- [ ] **R10 Composite**: `NoDoAcervo`, `GrupoDoAcervo`, `ItemDoAcervo`, `OrganizacaoDoAcervo`.
- [ ] Entidades: `Pessoa`, `Papel`, `Estudante`, `Atendente`, `Transacao`, `Reserva`, `Devolucao`.

## Fase 2 — Persistência e circulação (qua 25/11 tarde)
- [ ] **R8 (memória)**: `FabricaDePersistencia`, `UnidadeDeTrabalho`, repositórios em memória; `config.Aplicacao` + `PerfilDeExecucao`.
- [ ] Pontos de acesso `Acervo`, `Relacionamento`, `Movimentacao` sobre os repositórios.
- [ ] **R7 Template Method**: `CadastroTemplate` + 4 cadastros; tratadores `TratadorDeAcervo/Pessoa/Estudante/Atendente`.
- [ ] **R4 Facade**: `FachadaDeCirculacao` + comprovantes; `TratadorDeEmprestimo`, `TratadorDeDevolucao` delegando.
- [ ] **R5 Observer**: `PublicadorDeDevolucao`, `EventoDevolucao`, 4 ouvintes; publicação pós-commit.
- [ ] **R6 Command**: `SolicitacaoDeCirculacao`, `SolicitacaoDeReserva/Renovacao`, `ExecutorDeSolicitacoes` (concorrência testada).

## Fase 3 — Integrações e produção (qui 26/11 manhã)
- [x] **R3 Adapter**: `BancoAApi`, `BancoBApi` (simulados), `ServicoDeCobranca`, `AdaptadorBancoA/B`, quitação idempotente (branch `r03-adapter`, 8 testes; adiantado para depois do R2 por dependência).
- [ ] **R9 Decorator**: `ConsultaAcervo`, `ConsultaAcervoPadrao`, `ConsultaComMedicao`, `ConsultaComCache`, `Medidor`.
- [ ] **R8 (relacional)**: `FabricaRelacional`, `UnidadeDeTrabalhoJdbc` (conexão preguiçosa, transação única), repositórios JDBC, gravação/reconstituição do estado do Livro.
- [ ] **CLI**: menu que chama **só tratadores**; roteiro do fluxo da demo (reserva → empréstimo → renovação → devolução atrasada → multa), com relógio ajustável (`Clock` injetado) para simular atraso.
- [ ] `./mvnw test` → todos os `Rxx_` verdes. Rodar contra MySQL real ao menos uma vez.

## Fase 4 — UML no Astah (qua 25 → qui 26, **em paralelo** com o código, 1–2 pessoas)
Seguir `03-revisao-uml.md`. **Diagramas de classes relevados pelo professor (PR1)** — não entram. Escopo em `06-decisoes.md` (A-02/A-03):
1. [ ] **Máquina de estados de Livro** = máquina-alvo (E1–E9), nomes iguais aos métodos de `EstadoLivro`.
2. [ ] Atividades (A1–A15).
3. [ ] Componentes (C1–C8), com "Interface Web" → "Interface CLI".
4. [ ] Implantação (I1–I7) para CLI: `biblioteca.jar` em JVM, MySQL, Banco externo, SMTP.
5. [ ] Sequências do fluxo da demo (S1–S8), se A-03 confirmar.
6. [ ] Casos de uso: limpeza rápida (U1–U3).
7. [ ] Exportar para **PDF** (`docs/uml/pdf/`); conferir com `tools/astah/extrair.sh` que o modelo bate com o código.

> Dica: consolidar tudo num **único `.asta`** (`docs/uml/Biblioteca.asta`) facilita manter coerência; manter os originais
> como histórico.

## Fase 5 — Documentação e ensaio (qui 26/11 tarde/noite)
- [ ] `README.md`: requisitos, comando único (`mvnw test`), como subir MySQL, como rodar a CLI, perfis, **ferramentas/modelos LLM usados e onde**.
- [ ] `docs/RASTREABILIDADE.md` completa (requisito → padrão → classes por papel → teste) — conferir que cada classe existe.
- [ ] `REGISTRO_LLM.md` atualizado (pedidos, aceito/corrigido/descartado).
- [ ] **Ensaio da demo cronometrado (≤ 5 min)**: testes → fluxo CLI.
- [ ] **Treino de arguição**: cada integrante explica os 10 padrões e as perguntas "Na arguição, saiba explicar" de `01-enunciado.md`.
- [ ] **Treino de alteração ao vivo** (cada um faz pelo menos uma vez, < 3 min):
  - nova política de multa (ex. `PoliticaDobroAposDezDias`) → só nova classe + registro na configuração;
  - novo ouvinte da devolução (ex. `OuvinteAuditoria`) → só nova classe + `registrar` no composition root.

## Sex 27/11 manhã
- [ ] `git clone` limpo em outra pasta → `mvnw test` verde → push final / tag `entrega` **antes das 09h40**.

---

## Divisão sugerida (Vicente, Gabriel, Clara, Cauã — todos revisam tudo; arguição não tem divisão)
| Pessoa | Código | UML |
|---|---|---|
| A | R1, R4, R6 (circulação) | Máquina de estados + sequências R1/R4/R6 |
| B | R2, R3, R5 (financeiro/devolução) | Classes + sequências R2/R3/R5 |
| C | R7, R8, R9, R10 (cadastro/persistência/acervo) | Classes + sequências R7–R10 |
| D | CLI, README, rastreabilidade, REGISTRO_LLM | Atividades, componentes, implantação, casos de uso |

Regra: todo PR revisado por outra pessoa, que precisa conseguir explicar o código revisado.

## Riscos
| Risco | Mitigação |
|---|---|
| Prazo curto | Fases 1–2 primeiro (cobrem 7 padrões e a maior parte da nota); MySQL real por último. |
| Relevo verbal PR1 não reconhecido na correção (PDF exige diagramas de classes) | Citado no README (equipe optou por não pedir confirmação escrita). |
| MySQL indisponível na máquina da demo | Família relacional testada via tipos/UoW sem conexão (R08); demo roda em perfil memória se preciso. |
| Divergência código × UML | Nomes de métodos idênticos; rodar `extrair.sh` e comparar antes de exportar PDF. |
| Integrante não sabe explicar parte do código | Revisão cruzada obrigatória + sessão de treino na quinta à noite. |
