# 05 — Plano de ação

**Atualizado: qua 25/11/2026 (noite). Entrega: sex 27/11/2026, 09h40.** O plano prioriza o que pesa mais na nota:
padrões corretos (40%) e testes passando (20%) primeiro; docs (10%) no fim.

> **Situação em 25/11 (noite):** Fases 0–3 concluídas — os 10 padrões implementados, 99 testes verdes no `mvnw test`
> (103 com `-Pmysql`) e a CLI rodando o fluxo da demonstração contra o MySQL. **A Fase 4 (refatoração da UML) foi
> retirada do plano por decisão da equipe (25/11)** — ver Riscos. Falta a Fase 5 (documentação final, ensaio e treino)
> e a entrega.

## Situação do ambiente (verificado em 24/11)
- ✅ **JDK 25** (Microsoft, `C:\Program Files\Microsoft\jdk-25.0.5.102-hotspot`) e **Maven 3.10.2**
  (`C:\Program Files\Apache\maven\apache-maven-3.10.2`). Terminais abertos antes da instalação ainda veem o Java 8 —
  abrir terminal novo.
- ✅ **MySQL** (serviço `MySQL`, `localhost:3306`) + Workbench. Sem Docker (não é necessário).
- ✅ `JAVA_HOME` definido; release 21, Maven Wrapper, `br.pucpr.biblioteca`, JUnit 5 + AssertJ decididos.
- ✅ MySQL configurado para o perfil produção (25/11): banco `biblioteca`, usuário `biblioteca`, variáveis
  `BIBLIOTECA_DB_USUARIO`/`BIBLIOTECA_DB_SENHA` (usuário do Windows). As tabelas são criadas pela aplicação.
- ✅ Astah UML instalado (`C:\Program Files\astah-UML`) — não será usado para refatorar a UML (Fase 4 retirada).

## Decisões
Todas as decisões (decididas e pendentes, com recomendação) estão em **`06-decisoes.md`**. Decididas: CLI, Maven,
JDK 25, diagramas de classes relevados (PR1), demo em MySQL (D-09), **UML não será refatorada (A-09)**.

---

## Fase 0 — Fundação (ter 24/11 noite → qua 25/11 manhã cedo) · ~2h
- [x] Instalar JDK e Maven.
- [x] Definir `JAVA_HOME` (E-02). [x] Instalar MySQL (D-05).
- [x] Gerar projeto Maven (`pom.xml`, JUnit 5, AssertJ, mysql-connector-j, shade → `target/biblioteca.jar`, exec) + **Maven Wrapper**; `./mvnw test` verde (`FundacaoTest`).
- [x] Estrutura de pacotes de `04-arquitetura-alvo.md` (com `package-info.java`), `application.properties`, `docs/mysql/criar-banco.sql`, README. *(`schema.sql` movido para a Fase 3, junto da família JDBC.)*
- [x] Branch `fase0/fundacao`. [x] Commit/PR para `main`.
- [x] Fechar as decisões 🔴 de `06-decisoes.md`.

## Fase 1 — Núcleo do domínio (qua 25/11 manhã) · padrões sem dependências
Cada item = classes + teste `Rxx_` verde.
- [x] **R1 State**: `Livro`, `EstadoLivro` + 7 estados, `SituacaoLivro`, `TransicaoInvalidaException`, `Emprestimo.prorrogar` (branch `r01-state`, 19 testes).
- [x] **R2 Strategy**: `PoliticaDeMulta` + 3, `ServicoDeMultas` (parte de cálculo), `ConfiguracaoAdministrativa`, `Multa` (branch `r02-strategy`, 7 testes).
- [x] **R10 Composite**: `NoDoAcervo`, `GrupoDoAcervo`, `ItemDoAcervo`, `OrganizacaoDoAcervo` (branch `r10-composite`, 7 testes).
- [x] Entidades: `Pessoa`, `Papel`, `Estudante`, `Atendente`, `Devolucao` *(feitas na Fase 2, junto do R7/R4; `Transacao` virou `sealed`)*.

## Fase 2 — Persistência e circulação (qua 25/11 tarde) ✅ (branch `fase2/persistencia-circulacao`, 38 testes novos; total 81)
- [x] **R8 (memória)**: `FabricaDePersistencia`, `UnidadeDeTrabalho` (foto/restauração + trava única), repositórios em memória (guardam e devolvem cópias); `config.Aplicacao` + `PerfilDeExecucao` (8 testes).
- [x] Pontos de acesso `Acervo`, `Relacionamento`, `Movimentacao` sobre os repositórios.
- [x] **R7 Template Method**: `CadastroTemplate` + 4 cadastros; tratadores `TratadorDeAcervo/Pessoa/Estudante/Atendente` (11 testes).
- [x] **R4 Facade**: `FachadaDeCirculacao` + comprovantes; `TratadorDeEmprestimo`, `TratadorDeDevolucao` delegando (7 testes).
- [x] **R5 Observer**: `PublicadorDeDevolucao`, `EventoDevolucao`, 4 ouvintes; publicação pós-commit (6 testes).
- [x] **R6 Command**: `SolicitacaoDeCirculacao`, `SolicitacaoDeReserva/Renovacao`, `ExecutorDeSolicitacoes` (concorrência testada) (6 testes).
- [x] Commits em branches empilhadas por padrão: `r08-abstract-factory` → `r07-template-method` → `r04-facade` → `r05-observer` → `r06-command`. [x] Push + PRs (mesclados na `main`, PRs #6–#10).

## Fase 3 — Integrações e produção (qua 25/11 noite) ✅
- [x] **R3 Adapter**: `BancoAApi`, `BancoBApi` (simulados), `ServicoDeCobranca`, `AdaptadorBancoA/B`, quitação idempotente (branch `r03-adapter`, 8 testes; adiantado para depois do R2 por dependência).
- [x] **R9 Decorator**: `ConsultaAcervo`, `ConsultaAcervoPadrao`, `DecoradorDeConsulta`, `ConsultaComMedicao`, `ConsultaComCache`, `Medidor`, `MetadadosLivro` (branch `r09-decorator`, empilhada sobre `r08-relacional`; 8 testes; total 90).
- [x] **R8 (relacional)**: `FabricaRelacional`, `UnidadeDeTrabalhoJdbc` (conexão preguiçosa, transação única), repositórios JDBC (incl. multas), `schema.sql`, gravação/reconstituição do estado do Livro (branch `r08-relacional`; 1 teste no `mvnw test` + 4 de integração em `mvnw test -Pmysql`, rodados contra o MySQL local). *Feito antes do R9 porque a demo roda em MySQL (D-09).*
- [x] **CLI**: perfil padrão `producao` (D-09) + `--perfil=memoria`; dados de exemplo só com banco vazio + `--reiniciar-dados` (D-10); numeração do banco simulado (D-12); menu que chama **só tratadores** (+ `iAdministracao`); roteiro `docs/demo/roteiro-demo.txt` (`--roteiro=`); e-mails de boleto e quitação (B-13/B-16). Branch `cli/interface-cli`, empilhada sobre `r09-decorator`; `InterfaceCliTest` (6) + 3 em `R03`; total 99. Rodado contra o MySQL (roteiro completo + reinício sem perder dados). Itens originais: roteiro do fluxo da demo (reserva → empréstimo → renovação → devolução atrasada → multa), com relógio ajustável (`Clock` injetado) para simular atraso.
- [x] `./mvnw test` → todos os `Rxx_` verdes (99). Rodado contra o MySQL real (`-Pmysql`: 103 verdes; roteiro da demo pelo jar).
- [x] **CLI com acentos no console do Windows**: o console abre na página 850 e a CLI escreve em UTF-8 →
  `interfaceusuario.ConsoleDoWindows` troca o console para UTF-8 (`chcp 65001`, com entrada herdada para atingir o mesmo
  console) enquanto a CLI roda e restaura a página original na saída; teclado lido em UTF-8; símbolos sem suporte na fonte
  padrão (✓ ✗ ⚠ ●) trocados por texto. Conferido num `cmd` real (página 850) digitando "Coração Ação Útil".

## ~~Fase 4 — UML no Astah~~ — retirada do plano (decisão da equipe, 25/11, A-09)
A refatoração dos diagramas **não será feita**. Os `.asta` em `docs/` são entregues como estão; a lista do que divergia
continua em `03-revisao-uml.md` (útil para responder na arguição). O desenho dos padrões está no código, em
`04-arquitetura-alvo.md` e nos guias `docs/estudo/`. Itens que estavam aqui (não serão feitos): máquina de estados
E1–E9, atividades A1–A15, componentes C1–C8, implantação I1–I7, sequências S1–S8, casos de uso U1–U3, PDFs.

## Fase 5 — Documentação e ensaio (qui 26/11)
- [x] `README.md`: requisitos, comando único (`mvnw test`), como subir MySQL, como rodar a CLI, perfis, roteiro da demo,
  ferramentas/modelos LLM usados e onde. *(Seção "Modelos UML" ajustada: os diagramas não foram refatorados.)*
- [ ] Mesclar os PRs pendentes na `main` (#12 R9, PR da CLI) e conferir o README contra a `main`.
- [x] `docs/RASTREABILIDADE.md`: R1–R10 ✅ com classes e testes. [ ] Conferência final de que cada classe citada existe.
- [ ] `REGISTRO_LLM.md`: preencher a coluna "aceito/corrigido/descartado" (16 linhas com _a preencher_) — **só a equipe**.
- [ ] **Ensaio da demo cronometrado (≤ 5 min)** na máquina da apresentação: `mvnw test` →
  `java -jar target/biblioteca.jar --reiniciar-dados --roteiro=docs/demo/roteiro-demo.txt` (ou digitando o fluxo) →
  alteração ao vivo. Conferir antes: MySQL rodando, `BIBLIOTECA_DB_SENHA` definida, acentos no terminal usado.
- [ ] **Treino de arguição**: cada integrante explica os 10 padrões e as perguntas "Na arguição, saiba explicar" de `01-enunciado.md`.
- [ ] **Treino de alteração ao vivo** (cada um faz pelo menos uma vez, < 3 min):
  - nova política de multa (ex. `PoliticaDobroAposDezDias`) → só nova classe + registro na configuração;
  - novo ouvinte da devolução (ex. `OuvinteAuditoria`) → só nova classe + `registrar` no composition root.

## Sex 27/11 manhã
- [ ] `git clone` limpo em outra pasta → `mvnw test` verde → push final / tag `entrega` **antes das 09h40**.

---

## Divisão sugerida (Vicente, Gabriel, Clara, Cauã — todos revisam tudo; arguição não tem divisão)
| Pessoa | Estudar / revisar |
|---|---|
| A | R1, R4, R6 (circulação) — guias `docs/estudo/R01`, `R04`, `R06` |
| B | R2, R3, R5 (financeiro/devolução) — `R02`, `R03`, `R05` |
| C | R7, R8, R9, R10 (cadastro/persistência/acervo) — `R07`, `R08`, `R09`, `R10` |
| D | CLI, README, rastreabilidade, REGISTRO_LLM, roteiro da demo |

Regra: todo PR revisado por outra pessoa, que precisa conseguir explicar o código revisado.

## Riscos
| Risco | Mitigação |
|---|---|
| Prazo curto | Fases 1–2 primeiro (cobrem 7 padrões e a maior parte da nota); MySQL real por último. |
| Relevo verbal PR1 não reconhecido na correção (PDF exige diagramas de classes) | Citado no README (equipe optou por não pedir confirmação escrita). |
| MySQL indisponível na máquina da demo (demo em MySQL, D-09) | Ensaiar na máquina da demo com `--reiniciar-dados`; família relacional testada via tipos/UoW sem conexão (R08); plano B: `--perfil=memoria`. |
| **UML não refatorada (A-09)** — coerência código × UML vale 15% e o R1 pede "máquina de estados alinhada"; os diagramas atuais divergem do código (ex.: eventos da máquina de estados, "Interface Web", implantação web) | Decisão consciente da equipe. Na arguição: explicar as divergências com `03-revisao-uml.md` e mostrar que o código segue `04-arquitetura-alvo.md`; o README registra que os diagramas não foram atualizados. |
| Acentos no terminal da apresentação | `ConsoleDoWindows` põe o console em UTF-8; ensaiar no terminal que será usado (cmd, PowerShell ou VS Code). |
| Integrante não sabe explicar parte do código | Revisão cruzada obrigatória + sessão de treino na quinta à noite. |
