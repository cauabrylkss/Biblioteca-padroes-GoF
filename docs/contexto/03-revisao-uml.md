# 03 — Revisão dos diagramas (UML 2.5) e correções necessárias

Legenda de gravidade: 🔴 erro de notação/semântica UML ou contradição com o enunciado · 🟠 inconsistência entre
diagramas ou com o código-alvo · 🟡 qualidade/legibilidade.

> Regra geral: o critério "coerência código × UML" vale 15%. **Corrigir os diagramas para refletir o código que será
> escrito** (padrões GoF incluídos), não o contrário. Os diagramas são entregues em `.asta` **e PDF**.

---

## 1. Máquina de estados de Livro (`DiagramaDeEstados05`)

| # | Gravidade | Problema | Correção |
|---|---|---|---|
| E1 | 🔴 | **REPARANDO → INCINERADO sem evento** (só a guarda `[reparo inviável]`). Uma transição só com guarda vira transição de conclusão e dispara imediatamente ao entrar em Reparando. O enunciado exige o **evento de laudo irrecuperável**. | `Reparando --> Incinerado : registrarLaudoIrrecuperavel()` |
| E2 | 🔴 | **Pseudoestado choice inútil**: os dois ramos vão para o mesmo alvo (Devolvido), e o efeito `/ emitirBoletoDeMulta` coloca a multa **dentro da máquina de estados do Livro** — contradiz R5 (multa é reação **independente** via Observer, depois da devolução). | Remover o choice. `Emprestado --> Devolvido : registrarDevolucao()` e `Renovado --> Devolvido : registrarDevolucao()`, sem efeito de multa. |
| E3 | 🔴 | **Trigger na transição inicial** (nota "inserirLivro()" ligada à transição). Em UML a transição que sai do pseudoestado inicial **não tem evento nem guarda**. Nota não é trigger. | Transição inicial sem rótulo (no máximo efeito `/ cadastrar`). A criação é responsabilidade do cadastro (R7), não do estado. |
| E4 | 🟠 | Guarda inconsistente com o enunciado: `[numRenovacoes = 0]` em Emprestado→Renovado e `entry / numRenovacoes++` em Renovado. O enunciado fixa **`numRenovacoes < 2`** e exige que **guarda e ação estejam no método da classe do estado de origem** — ação em `entry` fica na classe de destino. Além disso a ação **"vencimento + 30 dias" não aparece**. | `Emprestado --> Renovado : renovar() [numRenovacoes < 2] / emprestimo.prorrogar(30 dias)` e o self-loop `Renovado --> Renovado : renovar() [numRenovacoes < 2] / emprestimo.prorrogar(30 dias)`. Remover o `entry`. |
| E5 | 🟠 | Nomes de eventos diferentes dos métodos que serão implementados (`renovarEmprestimo()` vs `renovar()`, `solicitarReserva()` etc.). R1 exige **1 transição = 1 método da classe do estado de origem**. | Usar exatamente os nomes de método de `EstadoLivro` (ver `04-arquitetura-alvo.md` §R1). |
| E6 | 🟠 | Guardas em linguagem natural (`[estudante cadastrado]`, `[dano identificado no acervo]`, `[reserva registrada para o mesmo estudante]`). Validação de estudante é da fachada/Relacionamento, não do estado do Livro. | Manter na máquina apenas guardas avaliáveis pelo estado (`[numRenovacoes < 2]`, `[emBomEstado]`, `[not emBomEstado]`, `[idEstudante = reserva.idEstudante]`). |
| E7 | 🟡 | Diagrama chamado "Statemachine Diagram0" e sem contexto. | Nomear **"Livro"** (`stm Livro`) e associar a máquina à classe `Livro`. |
| E8 | 🟡 | Dois estados finais (após `excluirLivro()` e após Incinerado) — permitido, mas Incinerado→[*] por transição de conclusão torna "Incinerado" efêmero. | Deixar **Incinerado como estado terminal estável** (sem saída) ou manter só o final de `excluir()`. Escolher um e refletir no código. |
| E9 | 🟡 | Falta cancelamento/expiração de reserva (Reservado → Livre). | Opcional: `Reservado --> Livre : cancelarReserva()`. Só incluir se for implementado. |

**Máquina-alvo** — ✅ **implementada no R1** (`biblioteca.livro`); desenhar no Astah exatamente assim. Cada linha é
um método da classe do estado de origem (`Livre`, `Reservado`, …), e o teste
`R01_cadaEstadoAceitaSoAsTransicoesDoDiagramaERecusaAsDemaisSemAlterarOLivro` confere a tabela inteira:
```
[*] --> Livre                                   (sem evento; livro novo nasce LIVRE)
Livre      --> Reservado  : reservar()
Livre      --> Reparando  : enviarParaReparo()
Livre      --> [*]        : excluir()
Reservado  --> Emprestado : emprestar(reserva, idEstudante) [reserva.idEstudante = idEstudante]
Emprestado --> Renovado   : renovar(emprestimo) [emprestimo.numRenovacoes < 2] / emprestimo.prorrogar()
Renovado   --> Renovado   : renovar(emprestimo) [emprestimo.numRenovacoes < 2] / emprestimo.prorrogar()
Emprestado --> Devolvido  : registrarDevolucao()
Renovado   --> Devolvido  : registrarDevolucao()
Devolvido  --> Livre      : inspecionar(emBomEstado) [emBomEstado]
Devolvido  --> Reparando  : inspecionar(emBomEstado) [not emBomEstado]
Reparando  --> Livre      : retornarDoReparo()
Reparando  --> Incinerado : registrarLaudoIrrecuperavel()
Incinerado                 (estado final estável: não aceita nenhum evento — decisão B-19, resolve E8)
```
`emprestimo.prorrogar()` = vencimento + 30 dias e `numRenovacoes + 1`. A validação "estudante cadastrado" (antiga
guarda de `solicitarReserva`) fica na fachada (R4), não na máquina do livro (E6).

---

## 2. Diagramas de atividades (`DiagramaDeAtividades05`)

### 2.1 AtividadesNaBiblioteca
| # | Gravidade | Problema | Correção |
|---|---|---|---|
| A1 | 🔴 | **Livro incinerado volta a LIVRE**: "incinerar livro" segue para o merge que leva a "alterar estado do livro para livre". | Após "incinerar livro" → **nó final de fluxo** (ou final da atividade). Nunca convergir com o ramo "livro em bom estado". |
| A2 | 🔴 | **"retornar livro" atualiza o objeto para `Livro [RENOVADO]`** — estado errado. | `Livro [LIVRE]`. |
| A3 | 🔴 | **Inspeção antes do registro**: "Inspecionar livro e devolver à biblioteca" (Atendente) acontece antes de "registrar devolução". Contradiz R1/R5 (o livro fica **DEVOLVIDO até a inspeção**). | Ordem: registrar devolução → `Livro [DEVOLVIDO]` → (publicar evento) → inspecionar → decisão `[emBomEstado]`/`[else]`. |
| A4 | 🔴 | **Multa sem decisão**: "verificar possíveis atrasos" vai **sempre** para "solicitar emissão de multa". Devolução no prazo também gera multa (contradiz R5). | Nó de decisão `[atrasado]` → emitir multa; `[no prazo]` → fim. |
| A5 | 🔴 | **Nó de decisão usado como merge** (losango com 3 entradas e 1 saída sem guarda, ex.: antes de "Informar recusa"). Decisão = 1 entrada/N saídas; merge = N entradas/1 saída. | Inserir **merge** explícito antes de "Informar recusa" (e no ramo de renovação). |
| A6 | 🔴 | **Fluxos de controle ligando ação a objeto** (`Criar Reserva → <<create>> Reserva`) e objetos "sumidouro" sem saída. Estereótipos `<<create>>/<<update>>` em nós de objeto não são UML padrão. | Usar **fluxo de objeto com pinos** (ação com *output pin* `reserva : Reserva`) ou nó de objeto entre duas ações. Estado do objeto como `Livro [RESERVADO]` está correto — manter, sem estereótipo. |
| A7 | 🟠 | **Uma atividade para 5 casos de uso**, com o fluxo de um caso "escorrendo" para o próximo (ex.: "Informar estudante sobre a reserva" → "Solicitar empréstimo"). Um processo não continua automaticamente do outro; reserva e empréstimo são dias diferentes. | Dividir em atividades por caso de uso (**Reservar**, **Efetivar empréstimo**, **Renovar**, **Registrar devolução + inspeção**, **Cobrar multa**), cada uma com nó inicial e finais próprios. Onde houver espera, usar **accept event action** (ex.: "pagamento notificado"). |
| A8 | 🟠 | Raias **Acadêmico** e **RH** vazias; falta raia para o validar estudante (Acadêmico/Relacionamento). | Remover raias vazias ou colocar "validar estudante" na raia Acadêmico. |
| A9 | 🟠 | Renovação: guarda `[0 < numRenovacoes < 2]` e testes de estado (`[estado = EMPRESTADO]`, `[estado = RENOVADO]`) — reproduz no fluxo o que deve estar no **State**. | Uma ação "renovar empréstimo" e decisão única `[numRenovacoes < 2]` / `[else] informar recusa`. |
| A10 | 🟠 | Devolução sem passar pelos ouvintes (inspeção, multa, indicadores, e-mail) — R5. | Após "registrar devolução": **fork** para "enfileirar inspeção", "calcular multa", "atualizar indicadores", "enviar e-mail" → join → fim (falha em um ramo não cancela os demais: modele com região de interrupção apenas no ramo de e-mail, ou nota). |
| A11 | 🟡 | Fork/join com barras desenhadas em sentidos opostos ("Criar empréstimo" e "Alterar estado" em paralelo e depois join em direção ao Atendente) — confuso. | Fork → [criar empréstimo ‖ atualizar livro] → join → "entregar livro". |

### 2.2 Cadastros
| # | Gravidade | Problema | Correção |
|---|---|---|---|
| A12 | 🟠 | **Condição de corrida modelada**: "Registrar Pessoa" corre **em paralelo** com "Verificar se a Pessoa do Estudante existe". A verificação pode falhar porque a Pessoa ainda está sendo criada. | Cadastros são operações independentes; não usar fork. Modelar **uma atividade genérica "Incluir cadastro"** que espelha o **Template Method** (R7): buscar existente → `[existe]` apresentar existente / `[novo]` validar → verificar pré-requisitos → `[falhou]` recusar (sem gravar) → criar → armazenar → apresentar. |
| A13 | 🟠 | Falta o passo **"buscar existente"** e o retorno do cadastro existente (cenário R7). | Incluído no fluxo acima. |
| A14 | 🟡 | Linhas de recusa cruzando todo o diagrama até merges do lado oposto. | Cada recusa → nó final de fluxo na própria raia. |
| A15 | 🟡 | Raia "Administrador" e ator "Administrador" ok, mas no componente o cadastro de Estudante é Acadêmico e de Atendente é RH — manter raias coerentes com o diagrama de componentes. | Ajustar após decidir onde fica `Relacionamento` (ver §4). |

---

## 3. Diagrama de implantação (`DiagramaDeImplantacao02`)

| # | Gravidade | Problema | Correção |
|---|---|---|---|
| I1 | 🔴 | **JDBC desenhado como dependência (seta tracejada)** entre nós. Ligação entre nós é **caminho de comunicação** (associação sólida), como os outros dois. | Trocar por associação `<<JDBC>>` sólida. |
| I2 | 🔴 | **`<<external>>` em srvMySQL e srvOracle**. Pelo enunciado, o externo é o **Banco (financeiro)**; o MySQL é o banco de produção **da aplicação**. Falta o nó do Banco externo. | Remover `<<external>>` dos SGBDs; adicionar nó **`<<external>> Banco A / Banco B`** ligado ao srvApp por `<<HTTPS/API>>`. |
| I3 | 🟠 | Artefato **`source.jar` no Tomcat** — Tomcat implanta `.war`. Nenhum artefato **manifesta** os componentes. | Artefato `biblioteca.jar` (CLI) ou `biblioteca.war` (web) com dependências `<<manifest>>` para os componentes Biblioteca, Acadêmico, RH, Financeiro, Interface. Ajustar ao que for implementado (ver decisão D1). |
| I4 | 🟠 | Oracle: falta indicar que é **réplica somente leitura** (R8: não recebe escritas). | Rótulo `replicação {unidirecional, somente leitura}` com seta MySQL → Oracle; artefato "Banco de dados espelho" com `{readOnly}`. |
| I5 | 🟠 | Falta servidor de **e-mail** (ouvinte de e-mail, R5). | Nó `<<device>> srvSMTP` (ou `<<external>>`), caminho `<<SMTP>>`. |
| I6 | 🟡 | "JVM 1090", "Windows 10" em servidor, "Dell Edge 3600" (modelo inexistente). Moldura `pkg` em vez de `deployment`. | `JVM 17` (ou a versão usada), SO de servidor, `Dell PowerEdge`; nomear o diagrama. |
| I7 | 🟡 | "Banco de dados principal" como artefato — aceitável, mas melhor `schema.sql`/`biblioteca (schema)`. | Opcional. |

---

## 4. Diagrama de componentes (`DiagramaDeComponentes03`)

| # | Gravidade | Problema | Correção |
|---|---|---|---|
| C1 | 🔴 | **Portas com nomes repetidos** no mesmo componente (5× `tratadorWeb` em Interface Web, 3× `tratadorDeDevolucao` em Financeiro) e **com nome de classe** (`tratadorDeEmprestimo` em Acadêmico provendo iEstudante; `tratadorDeDevoluvao` [sic] em Biblioteca requerendo iAtendente). Porta é ponto de interação do componente, não a classe tratadora. | Uma porta por interface com nome do papel (`pEmprestimo`, `pDevolucao`, `pAcervo`, `pEstudante`, `pPessoa`, `pAtendente`, `pMulta`, `pCobranca`, `pQuitacao`), ou portas omitidas e lollipop/soquete direto no componente. |
| C2 | 🔴 | **Interfaces desenhadas como notas** (retângulos rosa com "operações" que são caixas de classe dentro de nota). Nota não é interface; as "caixas" são classes soltas (`inserirLivro(...)`, etc. viraram classes no modelo). | Interfaces como classificadores `<<interface>>` com compartimento de operações (ou só no diagrama de classes), ligadas por realização/uso. Apagar do modelo as classes com nome de operação. |
| C3 | 🔴 | **Conector sem interface** formando um retângulo entre Financeiro e Interface Web passando por iPessoa — semântica indefinida. | Remover; se Financeiro precisa de dados da pessoa, soquete `iPessoa` ligado ao lollipop do RH. |
| C4 | 🔴 | Portas do Banco `<<external>>` chamadas `tratadorDeCobranca` (nome de classe interna de um sistema externo) e com uma porta sem interface. | Banco **provê `iCobranca`** e **requer `iQuitacao`** (callback de notificação). No código, `iCobranca` é o *Target* do **Adapter** e cada banco tem seu adaptador (R3). |
| C5 | 🟠 | **`Relacionamento` duplicado** (dentro de Acadêmico e de RH) e **`TratadorDeDevolucao` duplicado** (Biblioteca e Financeiro). | Um único `Relacionamento` (proposta: no RH, exposto por `iPessoa`/`iAtendente`; Acadêmico usa `iPessoa`), um único `TratadorDeDevolucao` (Biblioteca). Financeiro contém `ServicoDeMultas`, `PoliticaDeMulta`, adaptadores de banco. |
| C6 | 🟠 | Faltam as peças dos padrões: `FachadaDeCirculacao`, publicador/ouvintes, executor de solicitações, fábrica de persistência, consulta decorada, árvore do acervo. | Adicionar componente **Persistência** (provê `FabricaDePersistencia`) e listar as classes principais dentro de cada componente. |
| C7 | 🟠 | iMulta, iQuitacao, iCobranca, iPessoa, iEstudante, iAtendente **sem operações definidas** em lugar nenhum. | Definir no diagrama de classes do Financeiro/RH (ver `04-arquitetura-alvo.md`). |
| C8 | 🟡 | Muitos cruzamentos; elementos fantasmas no modelo (`Component0`, `Artifact1`, `Classifier/Instance7/13/14`, `classe2`). | Reorganizar; apagar fantasmas (Model Browser → Delete from model). |

---

## 5. Diagrama de classes / domínio (`DiagramaDeClasses19`, `DiagramaDeDominio8`)

> **RELEVADO (PR1, ver `01-enunciado.md`)**: o professor disse verbalmente que os diagramas de classes não precisam
> estar corretamente modelados; o desenho dos padrões vem do desenvolvimento. A tabela abaixo fica **como registro**
> das diferenças entre o modelo antigo e o código — **não é trabalho obrigatório**. Só limpar os elementos fantasmas
> (K3) se outros diagramas forem corrigidos no mesmo arquivo, para não vazarem para eles.

| # | Gravidade | Problema | Correção |
|---|---|---|---|
| K1 | 🔴 | **Nenhum padrão GoF modelado** (sem classes de estado, políticas, adaptadores, fachada, ouvintes, comandos, template, fábricas, decoradores, composite). O entregável pede **um diagrama de classes por padrão**. | Criar 10 diagramas (um por padrão) no mesmo projeto, mais o diagrama geral atualizado. |
| K2 | 🔴 | `EstadoLivro` é enumeração e `Livro.setEstado(EstadoLivro)` é público — permite que tratadores mudem o estado com `if`, o que R1 **proíbe**. | `EstadoLivro` vira interface/classe abstrata com 7 subclasses; `Livro` expõe **eventos** (`reservar`, `emprestar`, `renovar`, ...) e guarda `estado` privado. Persistir pelo **código** do estado (`SituacaoLivro` enum só para gravação). |
| K3 | 🔴 | **Modelo poluído com elementos fantasmas**: `Papel extends Pessoa` (no diagrama é associação "Tem"), `Reserva/Emprestimo/Devolucao extends Movimentacao` **e** `Transacao` (herança dupla), interface `CRUD` realizada por Pessoa/Livro/papéis, `Exemplar`, `GrupoDe*`, `Class4`, classes minúsculas, ~50 associações anônimas. A exportação/arguição pode expor isso. | Model Browser do Astah → apagar do **modelo** (não só do diagrama). Rodar `tools/astah/extrair.sh` e conferir que só sobra o que está nos diagramas. |
| K4 | 🟠 | `Multa.valor : float` e `calcularValor(...) : float`. Dinheiro em ponto flutuante gera erro de arredondamento (R2 pergunta "como os valores monetários são calculados"). | `BigDecimal` (escala 2, `RoundingMode.HALF_EVEN`) ou `long centavos`; `Multa` ganha `politicaAplicada : String` (R2). |
| K5 | 🟠 | `Multa.calcularValor` dentro da Multa → a política fica acoplada; R2 quer política plugável. | `ServicoDeMultas` + `PoliticaDeMulta` (Strategy). `Multa` só guarda valor e política. |
| K6 | 🟠 | `Devolucao.dataVencimento` duplicado do Empréstimo; Devolução não tem `dataDevolucao`. | `Devolucao` com `dataDevolucao`; vencimento vem do Empréstimo (e viaja no `EventoDevolucao`). |
| K7 | 🟠 | Tratadores dependem diretamente de Movimentacao/Acervo/Relacionamento/Livro (setas `<<create>>`, `<<use>>`). R4 exige que deleguem à **fachada**. | `TratadorDeEmprestimo`/`TratadorDeDevolucao` → `FachadaDeCirculacao` → Relacionamento/Movimentacao/Acervo/Livro. |
| K8 | 🟠 | Operações retornam entidades/`void` (`consultarReservaLivro(): Reserva`, `realizarEmprestimo(): void`). R4 pede **comprovantes**. | Fachada devolve `ComprovanteReserva`, `ComprovanteEmprestimo`, `ComprovanteRenovacao`, `ComprovanteDevolucao`. |
| K9 | 🟡 | Associações "utiliza" de atores-classe (Administrador/Estudante/Atendente) para tratadores misturam ator de caso de uso com classe de domínio. | Retirar do diagrama de classes de projeto (fica no de casos de uso). |
| K10 | 🟡 | `iDevolucao` mistura operações concretas e `{abstract}`; interfaces com nomes `iX` minúsculos mantidos por exigência do enunciado (R7 cita `iLivro`, `iPessoa`, `iEstudante`, `iAtendente`). | Manter os nomes `iX` (exigidos). Todas as operações de interface abstratas. |

---

## 6. Diagramas de sequência (`DiagramaDeSequencia19`)

| # | Gravidade | Problema | Correção |
|---|---|---|---|
| S1 | 🔴 | **Linhas de vida sem tipo** (`base = ?`) e **mensagens não vinculadas a operações**: há mensagens que não existem nas classes (`delete()`, `Stop0()`, `create(...)` sem construtor, `emprestimo1.estaAtrasado()` sem parâmetro). | Linhas de vida `nome : Classe` vinculadas às classes do modelo; cada mensagem escolhida da lista de operações. |
| S2 | 🔴 | **`alt` com um único operando** (vários). Isso é `opt`. | Trocar por `opt [cond]` ou adicionar `[else]`. |
| S3 | 🔴 | **Registrar devolução já faz a inspeção** e chama `livro.setEstado(LIVRE)` na mesma interação — contradiz R1/R5 (o livro fica DEVOLVIDO até a inspeção, que é outro evento). | Separar: "Registrar devolução" (fachada → livro.registrarDevolucao() → commit → publicador.publicar(evento) → ouvintes) e "Inspecionar livro" (fachada → livro.inspecionar(ok)). |
| S4 | 🟠 | **Tratador chama `livro.setEstado(X)` e `livro.getEstado()`** — exatamente o que R1 proíbe. | Tratador → Fachada → `livro.reservar(...)` → `estado.reservar(livro, ...)` (mostrar delegação ao objeto-estado). |
| S5 | 🟠 | Numeração fora de ordem (1.7 antes de 1.4; 3.5 antes de 3.4) e mensagem duplicada (4.5 e 4.6 `renovar()`). Return do tratador ao ator antes do return da Multa. | Reordenar; remover duplicata. |
| S6 | 🟠 | "Manter Livro, Pessoa, Estudante e Atendente" tem **4 interações num diagrama** com linhas de vida repetidas (4× Administrador, 3× Relacionamento). | Um diagrama por cenário, e para o R7 um único diagrama genérico do método-modelo `incluir()`. |
| S7 | 🟠 | Faltam sequências dos **cenários de verificação** (entregável): 10 cenários R1–R10. | Criar um diagrama de sequência por cenário (ver `05-plano-de-acao.md`). |
| S8 | 🟡 | Mensagens com prefixo do objeto no nome (`livro1.setEstado(...)`). | O destino já é a linha de vida; nome = operação. |

---

## 7. Casos de uso (`DiagramaDeCasosDeUso03`) — não citado pelo professor, mas há ajustes

| # | Gravidade | Problema | Correção |
|---|---|---|---|
| U1 | 🟠 | Casos de uso duplicados no modelo com grafias diferentes (`Inserir Atendente`/`inserir Atendente`, `Inserir Estudante`/`inserir Estudante`) e `extend` duplicado (Efetivar empréstimo → Registrar reserva, 2×); dependência simples entre casos de uso. | Limpar fantasmas; usar só `<<include>>`/`<<extend>>`. |
| U2 | 🟠 | Faltam: **Inspecionar livro**, **Enviar para reparo / Retornar do reparo**, **Registrar laudo irrecuperável**, **Quitar multa (notificação do Banco)**, **Configurar política de multa**; ator **Banco** (sistema externo). | Adicionar. |
| U3 | 🟡 | Sem fronteira do sistema (subject). | Adicionar retângulo "Sistema de Biblioteca". |
