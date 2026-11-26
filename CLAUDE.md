# CLAUDE.md — Sistema de Biblioteca com padrões GoF (PUCPR, MSC 2S-2026)

Trabalho acadêmico: implementar o sistema de reserva/empréstimo/renovação/devolução de livros aplicando **10 padrões GoF
obrigatórios**, com código **coerente com os modelos UML** (Astah). **Entrega: 27/11/2026 às 09h40.** Idioma do projeto:
**português (código, testes, docs)**.

## Leia antes de trabalhar
| Arquivo | Conteúdo |
|---|---|
| `docs/contexto/01-enunciado.md` | Resumo fiel do PDF: os 10 requisitos, cenários de verificação, perguntas da arguição, entregáveis, critérios |
| `docs/contexto/02-modelo-atual.md` | O que os `.asta` contêm hoje (classes, operações, máquina de estados, sequências, componentes, implantação) |
| `docs/contexto/03-revisao-uml.md` | Erros de UML por diagrama e a correção de cada um (inclui a **máquina de estados-alvo**) |
| `docs/contexto/04-arquitetura-alvo.md` | Pacotes, classes por papel GoF, decisões de desenho, nomes dos testes |
| `docs/contexto/05-plano-de-acao.md` | Fases, checklist, riscos |
| `docs/contexto/06-decisoes.md` | **Todas as decisões** (IDs A-xx…F-xx), decididas e pendentes, com recomendação. Seguir a recomendação enquanto a equipe não decidir outra coisa |
| `docs/RASTREABILIDADE.md` | Entregável: requisito → padrão → classes → teste (manter atualizado) |
| `REGISTRO_LLM.md` | Entregável: **registrar cada sessão de LLM relevante** (pedido + artefatos gerados) |

## ⚠️ Ponto a relevar (PR1) — orientação verbal do professor ≠ PDF
O PDF pede diagramas de classes por padrão e coerência código × UML; o professor disse verbalmente que **os diagramas
de classes UML não precisam estar corretamente modelados** e que o desenho com base nos padrões vem do desenvolvimento
(com apoio do LLM). Portanto: **não atualizar diagramas de classes**; o desenho de referência é `04-arquitetura-alvo.md`
+ o código. Mencionar o relevo no README.

**UML não será refatorada (decisão da equipe, 25/11 — `06-decisoes.md` A-09):** os demais diagramas (máquina de
estados, atividades, componentes, implantação, sequências, casos de uso) também ficam como estão. As divergências
entre UML e código ficam documentadas em `03-revisao-uml.md` (material para a arguição). Não propor a refatoração de
novo.

## Os 10 padrões (cada um na situação indicada — não trocar)
R1 **State** ciclo de vida do Livro · R2 **Strategy** políticas de multa · R3 **Adapter** bancos A/B · R4 **Facade**
circulação · R5 **Observer** reações à devolução · R6 **Command** solicitações de reserva/renovação · R7 **Template Method**
cadastros · R8 **Abstract Factory** famílias de persistência (memória/MySQL) · R9 **Decorator** consulta ao acervo
(medição/cache) · R10 **Composite** organização hierárquica do acervo.

## Precedência das fontes
**PDF > diagramação UML > recomendação técnica.** O que o PDF não fixa, a UML decide (ver `06-decisoes.md`, grupo B,
coluna Fonte). Se a UML contradiz o PDF, vale o PDF e o diagrama entra em `03-revisao-uml.md`. Desenho interno dos
padrões (grupo C) é do desenvolvimento (PR1).

## Regras de domínio invariáveis
- 7 estados do Livro: LIVRE, RESERVADO, EMPRESTADO, RENOVADO, DEVOLVIDO, REPARANDO, INCINERADO — **uma classe por estado**;
  evento inválido é **recusado sem alterar o livro**; cada transição = um método **da classe do estado de origem**, com
  guarda e ação no corpo.
- Renovação: guarda **`numRenovacoes < 2`**, avaliada **antes** de alterar o vencimento; cada renovação = **+30 dias**.
- **DEVOLVIDO** permanece até a **inspeção** (`inspecionar(emBomEstado)`); DEVOLVIDO não pode ser reservado.
- **REPARANDO → INCINERADO** só pelo evento **`registrarLaudoIrrecuperavel()`**.
- Multa: original R$1,00/dia; teto R$20,00; tolerância 2 dias. Grava **valor e política** na emissão. Dinheiro em
  **`BigDecimal`**, nunca `float`/`double`.
- Notificação de pagamento repetida **não** quita duas vezes. Mesmo id de solicitação **não** executa duas vezes.
- Oracle é **réplica somente leitura** — a aplicação nunca escreve nele.
- Vindas da UML: empréstimo **só a partir de RESERVADO** e pelo mesmo estudante; renovar atrasado é permitido;
  multa emitida **automaticamente** na devolução atrasada (ouvinte) gera um **`Boleto` (EMITIDO → PAGO)** enviado por
  e-mail; pagamento → Banco informa quitação → Multa QUITADA → notificar usuário; **inspeção é comando do Atendente**;
  **id do Livro é informado no cadastro** (duplicidade por id); Pessoa duplicada por **CPF**; sem login (CLI pergunta
  "operar como"); `idAtendente` obrigatório em empréstimo e devolução; Incinerado é estado final.

## Regras de arquitetura (verificadas na arguição)
- Camadas: **Interface (CLI) → Tratadores → Fachada/serviços → Acervo · Relacionamento · Movimentacao → domínio**.
- A interface chama **apenas tratadores** e não tem regra de domínio.
- **Tratadores não testam o estado do livro com `if`/`switch`** e não chamam `setEstado` — disparam eventos no `Livro`.
- Preservar nomes do Trabalho 02: `TratadorDeEmprestimo` (`iEmprestimo`), `TratadorDeDevolucao` (`iDevolucao`),
  `TratadorDeAcervo` (`iLivro`), `TratadorDePessoa` (`iPessoa`), `TratadorDeEstudante` (`iEstudante`),
  `TratadorDeAtendente` (`iAtendente`), `Acervo`, `Relacionamento`, `Movimentacao`. Os nomes `iX` minúsculos são exigidos.
- Fachada devolve **comprovantes (records)**, nunca coleções; operações transacionais via `UnidadeDeTrabalho`
  (falha no meio → desfaz tudo).
- Observer publica **após o commit**; falha de um ouvinte não afeta os outros nem desfaz a devolução.
- Fábrica de persistência escolhida **uma única vez** pelo perfil (`teste` = memória, `producao` = MySQL) no composition root.
- Cache (R9) guarda **só metadados**; reserva sempre lê o estado atual do Livro do repositório.
- Extensões ao vivo (nova política de multa, novo ouvinte) devem exigir **só uma classe nova + registro no composition
  root**, sem editar classes existentes. Preservar isso.

## Stack e comandos
- Interface **web** (padrão; `HttpServer` do JDK + `src/main/resources/web/index.html`/`app.js`, sem framework, E-14) e
  **CLI** como reserva. JDK 25 compilando com `release 21` (mínimo 17), **Maven com wrapper**, JUnit 5 + AssertJ,
  MySQL Connector/J. Sem Spring.
- Pacote raiz `br.pucpr.biblioteca`; subpacotes = componentes (ver `04-arquitetura-alvo.md`).
- Comando único (meta): `./mvnw test` (Git Bash) / `mvnw.cmd test` (PowerShell).
- **Ambiente (24/11):** JDK em `C:\Program Files\Microsoft\jdk-25.0.5.102-hotspot`, Maven em
  `C:\Program Files\Apache\maven\apache-maven-3.10.2`. `JAVA_HOME` definido (máquina). Shells abertos antes da
  instalação ainda resolvem `java` para o JRE 8 — se `java -version` mostrar 1.8, pôr `$JAVA_HOME/bin` e o `bin` do
  Maven no início do PATH. **MySQL**: serviço Windows `MySQL` em `localhost:3306` (sem Docker), banco e usuário
  `biblioteca` criados, `BIBLIOTECA_DB_USUARIO`/`BIBLIOTECA_DB_SENHA` definidas (usuário do Windows). Não usar o JDK
  interno do Astah para o projeto.
- Rodar: `java -jar target/biblioteca.jar` (interface web em http://localhost:8080, perfil padrão `producao` = MySQL,
  D-09) · `--perfil=memoria` (plano B) · `--reiniciar-dados` · `--porta=NNNN` · `--sem-navegador` · `--interface=cli`
  · `--roteiro=docs/demo/roteiro-demo.txt` (implica a CLI; executado também pelo `InterfaceCliTest` — mudar a CLI exige
  manter o roteiro funcionando).
- Web: `interfaceusuario.web.InterfaceWeb` tem **uma rota por operação de tratador** (`GET|POST /api/{iInterface}/{operacao}`)
  e só campos de tratadores; o `InterfaceWebTest` roda o fluxo da demo por HTTP e confere que todo formulário do
  `index.html` aponta para uma rota existente — ao criar/renomear uma operação, mudar os dois. A página não tem regra
  de domínio. Textos de comprovantes ficam em `interfaceusuario.Mensagens` (os mesmos na CLI e na web). Testes de integração com MySQL: `./mvnw test -Pmysql` (**apagam os
  dados** do banco; depois rodar com `--reiniciar-dados`).

## Testes
- **Um teste por cenário de verificação**, nome iniciando pelo requisito: `R01_terceiraRenovacaoRecusada`,
  `R07_cpfNaoCadastradoFalhaAntesDeGravar` etc. (lista completa em `04-arquitetura-alvo.md`).
- Datas via `java.time.Clock` injetado (atrasos determinísticos). Testes rodam no perfil de memória, sem MySQL.

## UML (Astah)
- Fontes em `docs/*.asta`; PNG do estado original em `docs/diagramas/png-original/`.
- Os `.asta` são ZIP com objetos Java serializados — **não editar à mão**. Para ler: `bash tools/astah/extrair.sh`
  (usa o Astah em `C:\Program Files\astah-UML`; gera PNGs + dump textual em `build/astah/`).
- **Não editar os diagramas** (A-09). Ao mudar nomes de eventos do Livro, componentes ou nós de implantação no código,
  registrar a nova divergência em `03-revisao-uml.md` (coerência código × UML = 15% da nota; a equipe explica as
  divergências na arguição).
- Os modelos têm **elementos fantasmas** (apagados do diagrama, não do modelo) — não usá-los como referência; a
  verdade é o que está desenhado + `04-arquitetura-alvo.md`.

## Convenções de trabalho
- Equipe: **Vicente, Gabriel, Clara, Cauã**. Remoto: `origin` = github.com/cauabrylkss/Biblioteca-padroes-GoF.
- **Uma branch por padrão/fase** (`fase0/fundacao`, `r01-state`, `r02-strategy`, …) + PR revisado por outro integrante.
  Fluxo com LLM: **um padrão por vez**; a equipe revisa e explica antes do próximo.
- Commits/push só quando pedido. Não commitar credenciais do MySQL (usar variáveis `BIBLIOTECA_DB_*`).
- A confirmação escrita do relevo PR1 **não será pedida** — não voltar a sugerir.
- `Main` força saída UTF-8 (console do Windows embaralha acentos); manter isso ao mexer na CLI. O console do Windows
  abre na página 850: `interfaceusuario.ConsoleDoWindows` o coloca em UTF-8 durante a execução (e restaura ao sair) e o
  teclado é lido em UTF-8. Não usar na CLI símbolos ausentes da fonte padrão do console (✓ ✗ ⚠ ●).
- Toda sessão com LLM que gere código ou documentação → nova linha em `REGISTRO_LLM.md`.
- Ao concluir um padrão: teste verde → atualizar `docs/RASTREABILIDADE.md` (status ✅) → marcar checklist em `05-plano-de-acao.md`.
- Código precisa ser explicável por qualquer integrante: preferir clareza a abstração extra; comentar o **papel GoF** no
  Javadoc da classe (ex.: `/** Papel GoF: ConcreteState (State) — ... */`).
