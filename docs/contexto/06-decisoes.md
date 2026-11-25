# 06 — Registro de decisões

Cada decisão tem um ID estável (citar o ID em commits/PRs quando aplicável). Status: ✅ decidido · ❓ pendente
(vale a **recomendação** até a equipe decidir diferente). Coluna **Quando**: 🔴 bloqueia o início (Fase 0/1) ·
🟠 precisa até a fase indicada · 🟢 pode ficar com o padrão.

## Regra de precedência (definida pela equipe em 24/11)
1. **PDF do enunciado** (regras e cenários explícitos) — exceto onde houver relevo verbal registrado (PR1).
2. **Diagramação UML** (`docs/*.asta`) — **decide tudo o que o PDF não fixa**.
3. Recomendação técnica deste documento — só para o que nem o PDF nem a UML definem.

Quando a UML contradiz o PDF (ex.: multa como efeito na máquina de estados, inspeção dentro da sequência de devolução),
vale o PDF e o diagrama entra na lista de correção de `03-revisao-uml.md`.
Pela PR1, o **desenho interno dos padrões** (grupo C) é responsabilidade do desenvolvimento.

## Já decididas
| ID | Decisão | Valor |
|---|---|---|
| D-01 | Interface | ✅ **CLI** |
| D-02 | Build | ✅ **Maven** 3.10.2 (`C:\Program Files\Apache\maven\apache-maven-3.10.2`) |
| D-03 | JDK | ✅ **Microsoft OpenJDK 25.0.5** (`C:\Program Files\Microsoft\jdk-25.0.5.102-hotspot`) |
| D-04 | Diagramas de classes UML | ✅ **Relevados** pelo professor (PR1) |
| D-05 | Servidor MySQL | ✅ Instalado; serviço Windows `MySQL` rodando (automático) em `localhost:3306` |
| E-01 | Bytecode | ✅ `maven.compiler.release=21` |
| E-02 | `JAVA_HOME` | ✅ Definido (máquina) → JDK 25 |
| E-03 | Maven Wrapper | ✅ Sim |
| E-04 | Coordenadas | ✅ `br.pucpr.biblioteca` / `biblioteca-gof` |
| E-05 | Testes | ✅ JUnit 5 + AssertJ, sem Mockito |

---

## A. Escopo da UML (após o relevo PR1)
| ID | Decisão | Opções | Recomendação | Quando |
|---|---|---|---|---|
| A-01 | Pedir confirmação escrita do relevo PR1? | sim / não | ✅ **Não será pedida** (decisão da equipe, 25/11 — assunto encerrado) | — |
| A-02 | Quais diagramas corrigir | estados · atividades · componentes · implantação · sequências · casos de uso | **Estados (obrigatório, R1), atividades, componentes, implantação**. Casos de uso: limpeza rápida | 🟠 Fase 4 |
| A-03 | Diagramas de sequência dos cenários (entregável do PDF) | os 10 · só os do fluxo da demo · nenhum | **Só os 4 do fluxo da demo** — salvo se A-01 disser outra coisa | 🟠 Fase 4 |
| A-04 | Máquina de estados × código | alinhar ao **código** · ao diagrama antigo | **Ao código** (eventos = métodos de `EstadoLivro`) | 🟠 Fase 1 |
| A-05 | Arquivos `.asta` | um único · manter os 8 separados | **Manter separados** | 🟢 |
| A-06 | Quem edita a UML | equipe no Astah · LLM via API · misto | **Equipe no Astah**, guiada por `03-revisao-uml.md`; conferência com `extrair.sh` | 🟠 Fase 4 |
| A-07 | Implantação para CLI | `biblioteca.jar` em JVM · manter web | **`biblioteca.jar`** + MySQL + Banco externo + SMTP | 🟠 Fase 4 |
| A-08 | Componente "Interface Web" | renomear para **Interface CLI** · manter | **Renomear** | 🟠 Fase 4 |

## B. Regras de domínio que o PDF não fixa → resolvidas pela UML quando possível
Coluna **Fonte**: PDF · UML (diagrama) · ❓ (nenhum dos dois — vale a recomendação).

| ID | Regra | Valor adotado | Fonte | Status |
|---|---|---|---|---|
| B-01 | Prazo do empréstimo inicial | **30 dias** | Equipe (25/11) | ✅ |
| B-02 | Base da renovação | Vencimento atual + 30 dias ("estender data de vencimento e aumentar numRenovacoes") | PDF (30 dias) + UML atividade | ✅ |
| B-03 | Renovar estando atrasado | **Permitido** — a guarda da máquina é só `numRenovacoes < 2`; a atividade só testa estado e número de renovações | UML estados/atividade | ✅ |
| B-04 | Empréstimo sem reserva prévia | **Proibido** — só RESERVADO → EMPRESTADO; atividade exige `[estado = RESERVADO]` | UML estados/atividade + PDF R4 | ✅ |
| B-05 | Quem pode emprestar | Só o estudante da reserva (`[reserva registrada para o mesmo estudante]`); reserva exige `[estudante cadastrado]` | UML estados | ✅ |
| B-06 | Cancelamento/expiração de reserva | **Não existe** | UML (ausente) | ✅ |
| B-07 | Contagem de dias de atraso | **Dias corridos por data** (`LocalDate`); devolver no dia do vencimento = 0 de atraso | Equipe (25/11) | ✅ |
| B-08 | Política com tolerância | (dias − 2) × R$1 → 5 dias = R$3,00 | PDF (cenário) | ✅ |
| B-09 | Política com teto | mín(dias × R$1, R$20) | PDF | ✅ |
| B-10 | Política vigente padrão | Recomendação: original | ❓ | 🟢 |
| B-11 | Como o admin troca a política | Recomendação: padrão no `.properties` + comando de administração na CLI | ❓ | 🟠 Fase 1 |
| B-12 | Quando a multa é emitida | **Automaticamente na devolução atrasada** (efeito `/emitirBoletoDeMulta` na máquina; sequência de devolução chama `emitirBoletoDeMulta` se atrasado) → implementado pelo **ouvinte de multa** (R5). `iDevolucao.emitirBoletoDeMulta` continua disponível ao atendente (caso de uso "Emitir multa") para consultar/reemitir | UML + PDF R5 | ✅ |
| B-13 | Boleto | **Entidade `Boleto`** com situação **EMITIDO → PAGO**, emitido junto com a multa via adaptador do banco e **enviado ao estudante por e-mail** | UML atividade | ✅ |
| B-14 | Formato das APIs simuladas dos bancos A/B | A = centavos `long`, CPF só dígitos, status `int` (1/2/9), id `long`; B = texto "7,00", CPF formatado, status `String` (ABERTO/LIQUIDADO/BAIXADO), id `B-000001`; cada banco com formato próprio de notificação | Implementação R3 | ✅ |
| B-15 | Banco da instalação | A por padrão, via `banco=` no `.properties` (`config.BancoDaInstalacao`) | Implementação R3 | ✅ |
| B-16 | Pagamento e quitação | Fluxo: estudante paga → **Banco recebe → informa quitação ao Financeiro → registrar multa como quitada (Boleto PAGO, Multa QUITADA) → notificar usuário**. Na CLI: comando "simular pagamento" que gera a notificação bruta do banco | UML atividade + PDF R3 | ✅ |
| B-17 | Inspeção | Evento `inspecionarLivro(emBomEstado)` disparado pelo **Atendente** (raia Atendente; `iDevolucao.inspecionarLivro`). O livro fica DEVOLVIDO até lá; o ouvinte de inspeção só registra a pendência | UML + PDF R5 | ✅ |
| B-18 | Exclusão de livro | Só em LIVRE (`excluirLivro()` sai de Livre para o estado final) | UML estados | ✅ |
| B-19 | Livro incinerado | **Estado final**: não aceita mais nenhum evento; o registro permanece para histórico e sai da árvore do acervo | UML estados | ✅ |
| B-20 | E-mail | Usado para **enviar boleto** e **notificar quitação**. Implementação simulada (log/arquivo), com falha configurável para demonstrar R5 | UML (uso) + ❓ (implementação) | ✅ |
| B-21 | Indicadores | Recomendação: total de devoluções, devoluções atrasadas, dias de atraso acumulados, multas emitidas | ❓ | 🟢 |
| B-22 | Chave de duplicidade | **Livro: `id` informado no cadastro** (sequência "Manter": `consultarLivro(livro.idLivro)` antes de inserir) — sem ISBN. **Pessoa: CPF** (PDF R7). **Estudante/Atendente:** pessoa que já tem o papel | UML sequência + PDF R7 | ✅ |
| B-23 | Validação de CPF | Recomendação: formato (11 dígitos) + dígitos verificadores | ❓ | 🟠 Fase 2 |
| B-24 | Geração de IDs | **Livro: informado pelo administrador** (B-22). Pessoa, papéis e transações: `long` sequencial. Solicitações (R6): UUID | UML + recomendação | ✅ |
| B-25 | Autenticação | **Sem login** — a UML só tem os atores Estudante, Atendente e Administrador; a CLI pergunta "operar como" e o operador vira o `autor` das solicitações (R6) | UML casos de uso | ✅ |
| B-26 | Atendente obrigatório | Sim: `realizarEmprestimo(idLivro, idEstudante, idAtendente)`, `registrarDevolucao(idLivro, idEstudante, idAtendente)` | UML (interfaces) | ✅ |
| B-27 | Papel Administrador | Mantido: cadastros e configuração | UML | ✅ |
| B-28 | Limites extras (máx. livros por estudante, bloqueio por multa) | **Não existem** | UML (ausente) | ✅ |
| B-29 | Operações oferecidas na CLI | As das interfaces UML: `iEmprestimo` (consultarReservaLivro, solicitarReserva, realizarEmprestimo, renovarEmprestimo), `iDevolucao` (registrarDevolucao, emitirBoletoDeMulta, inspecionarLivro, enviarParaReparo, retornarDoReparo + **registrarLaudoIrrecuperavel**, exigido pelo PDF), `iLivro`, `iPessoa`, `iEstudante`, `iAtendente` (CRUD) | UML + PDF | ✅ |

## C. Arquitetura e padrões — responsabilidade do desenvolvimento (PR1)
Aplicadas conforme a recomendação; a equipe só precisa se manifestar se discordar.
| ID | Decisão | Valor adotado |
|---|---|---|
| C-01 | Operações na fachada | Todas as de circulação (reserva, empréstimo, renovação, devolução, inspeção, reparo, laudo) |
| C-02 | Operações como Command | Só reserva e renovação (PDF R6) |
| C-03 | Repetição do mesmo id | Devolve o mesmo comprovante da 1ª execução |
| C-04 | Registro de ids executados | Em memória (`ConcurrentHashMap`); limitação citada na arguição |
| C-05 | Observer | Síncrono, sequencial, `try/catch` por ouvinte, publicado após o commit |
| C-06 | Ordem dos ouvintes | inspeção → multa → indicadores → e-mail (resultado não depende da ordem) |
| C-07 | UoW em memória | Rollback real (alterações aplicadas só no commit) |
| C-08 | Árvore do acervo (R10) | Só em memória (montada no seed) |
| C-09 | Cadeia de decoradores | Configurável por `.properties`; ordem fixa Medição(Cache(Padrão)) |
| C-10 | Cache | Sem expiração; invalidado em `atualizarLivro`/`excluirLivro` |
| C-11 | Relógio | `java.time.Clock` injetado + comando de demo "avançar N dias" |
| C-12 | Datas | `LocalDate` (vencimento/devolução), `Instant` (criação) |
| C-13 | Exceções | Hierarquia própria a partir de `RegraDeNegocioException` |
| C-14 | Logging | `System.Logger` |
| C-15 | Interfaces de componente | `iMulta` (Financeiro → Biblioteca); `ServicoDeCobranca` = iCobranca (Target do Adapter); iQuitacao = recebimento de `NotificacaoPagamento` |
| C-16 | `Relacionamento` | Único, no RH |

## D. Persistência / MySQL
| ID | Decisão | Recomendação | Quando |
|---|---|---|---|
| D-06 | Criação do schema | A aplicação executa `schema.sql` (`CREATE TABLE IF NOT EXISTS`) ao subir no perfil produção | 🟠 Fase 3 |
| D-07 | Conexão | `application.properties` + variáveis de ambiente `BIBLIOTECA_DB_URL/USUARIO/SENHA`; senha fora do Git. Banco `biblioteca` e usuário próprio criados por um script SQL que a equipe executa uma vez no Workbench | 🟠 Fase 3 |
| D-08 | Teste do perfil produção (R08) | Sem banco (tipos + UoW compartilhada, conexão preguiçosa) + teste de integração `@Tag("mysql")` fora do `mvn test` padrão | 🟠 Fase 3 |
| D-09 | Perfil padrão da CLI | Memória com dados de exemplo; `--perfil=producao` para MySQL. *Se quiserem mostrar MySQL na demo, escolher produção como padrão* | 🟠 Fase 3 |
| D-10 | Dados de exemplo (seed) | Sim | 🟠 Fase 3 |
| D-11 | Estado no banco | Coluna `situacao VARCHAR` com o nome do estado | 🟠 Fase 3 |

## E. Build, repositório e código
| ID | Decisão | Recomendação | Quando |
|---|---|---|---|
| E-06 | Organização dos testes | ✅ Uma classe por requisito (`R01StateTest`…) com métodos `R01_…` | — |
| E-07 | Como rodar a CLI | Jar executável (`java -jar target/biblioteca.jar`) + `mvnw exec:java` | 🟠 Fase 3 |
| E-08 | Identificadores | ✅ Português sem acento | — |
| E-09 | Javadoc com papel GoF | Sim | 🟢 |
| E-10 | Fluxo Git | ✅ **Uma branch por padrão/fase** (`fase0/fundacao`, `r01-state`, …) + PR revisado por outro integrante | — |

## F. Equipe e processo
| ID | Decisão | Recomendação | Quando |
|---|---|---|---|
| F-01 | Integrantes | ✅ **Vicente, Gabriel, Clara, Cauã** (no README) | — |
| F-02 | Quanto do código o LLM escreve | ✅ **LLM escreve um padrão por vez; a equipe revisa e explica cada um antes do próximo** | — |
| F-03 | Preenchimento do `REGISTRO_LLM.md` | A cada entrega | 🟢 |
| F-04 | Ensaio e treino de arguição | Quinta à noite | 🟢 |
