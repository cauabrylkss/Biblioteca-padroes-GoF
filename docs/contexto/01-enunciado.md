# 01 — Enunciado do trabalho (resumo fiel do PDF)

Fonte: `docs/Trabalho_Implementacao_GoF_Biblioteca.pdf` — PUCPR, Modelagem de Sistemas Computacionais,
Prof. Edson Emílio Scalabrin, Turma A – 2S – 2026. Continuação do **Trabalho 02** e da lista
"Problemas de aplicação dos padrões GoF".

## Objetivo
Versão **executável** do sistema de cadastro, reserva, empréstimo, renovação e devolução de livros,
aplicando **no mínimo os 10 padrões GoF** abaixo, cada um **na situação para a qual foi proposto**.
O código deve corresponder aos modelos UML, e cada padrão é comprovado por um **cenário de verificação**
executado na demonstração.

## Contexto comum (premissas que não podem ser quebradas)
- Empresa iB2B, sistema desenvolvido pela BCC2022.
- Componentes **Acadêmico, RH, Biblioteca e Financeiro** comunicam-se **por interfaces**; o **Banco é externo**.
- Preservar os **tratadores**, a **delegação** e os **pontos internos de acesso `Acervo`, `Relacionamento` e `Movimentacao`** (Trabalho 02).
- Guarda **`numRenovacoes < 2`** (no máximo 2 renovações).
- Evento de **laudo irrecuperável** leva **REPARANDO → INCINERADO**.

## Os 10 requisitos obrigatórios

| Nº | Situação | Padrão |
|----|----------|--------|
| R1 | Ciclo de vida do livro | **State** (máquina de estados alinhada ao diagrama de classes) |
| R2 | Políticas de cálculo de multa | **Strategy** |
| R3 | Integração com bancos incompatíveis | **Adapter** |
| R4 | Entrada única para as operações de circulação | **Facade** |
| R5 | Reações independentes à devolução | **Observer** |
| R6 | Solicitações de circulação como objetos | **Command** |
| R7 | Rotina comum para cadastros | **Template Method** |
| R8 | Famílias compatíveis de acesso aos dados | **Abstract Factory** |
| R9 | Serviço de consulta com recursos opcionais | **Decorator** |
| R10 | Organização hierárquica do acervo | **Composite** |

Outros padrões podem ser usados se justificados, mas **não substituem** os da tabela.

### R1 — State (ciclo de vida do livro)
- `Livro` **delega cada evento a um objeto de estado**. **Uma classe por estado** (7): LIVRE, RESERVADO,
  EMPRESTADO, RENOVADO, DEVOLVIDO, REPARANDO, INCINERADO.
- Cada classe implementa **apenas as operações válidas**; as demais são **recusadas sem alterar o livro**.
- **Os tratadores não testam a situação do livro com condicionais.**
- Incluir o evento de **laudo irrecuperável** em REPARANDO.
- **Alinhamento com o modelo:** cada transição da máquina de estados = **um método da classe do estado de origem**;
  guardas e ações aparecem **no corpo desse método**.
- **Cenário:** 3ª renovação recusada **sem alterar o vencimento**; livro DEVOLVIDO ainda não inspecionado **não pode ser reservado**.
- **Arguição:** onde está a guarda da renovação e por que é avaliada antes de alterar o vencimento; o que acontece
  quando um evento chega a um estado que não o aceita; **como o estado é gravado e reconstituído na persistência**.

### R2 — Strategy (políticas de multa)
- Interface de política + 3 implementações: **original (R$ 1,00/dia)**, **com teto de R$ 20,00**, **com tolerância de 2 dias**.
- Política vigente definida pela **configuração administrativa**.
- A multa **grava o valor e a política aplicada na emissão**; a emissão de boletos **não muda**.
- **Cenário:** 5 dias de atraso → **R$ 5,00** (original) e **R$ 3,00** (tolerância). Sem atraso → **zero**.
- **Arguição:** nova política sem alterar o serviço de multas; por que trocar a política não recalcula multas já
  emitidas; como dias de atraso e valores monetários são calculados.

### R3 — Adapter (bancos incompatíveis)
- **Contrato institucional de cobrança** + **um adaptador por banco**, que converte **valores, documentos,
  identificadores e situações**. **Notificações de pagamento também são traduzidas pelo adaptador.**
- Serviço de multas **sem condicionais por banco**; **notificação repetida não gera segunda quitação**.
- Bancos A e B podem ser **simulados** por classes que imitam APIs incompatíveis.
- **Cenário:** a mesma solicitação de R$ 7,00 produz **700 centavos no banco A** e **7,00 no banco B**.
- **Arguição:** onde ficam as conversões; como o banco da instalação é escolhido; como a quitação é protegida contra
  notificações repetidas.

### R4 — Facade (circulação)
- Fachada de circulação que coordena **Relacionamento, Movimentacao, Acervo e Livro** em operações **completas e
  transacionais**, devolvendo **comprovantes em vez de coleções**.
- **Tratadores delegam à fachada**; a **interface depende apenas dos tratadores**.
- **Cenário:** efetivar empréstimo deve **validar a reserva, remover a reserva ativa, criar o empréstimo e atualizar
  Livro**, sem expor coleções à interface.
- **Arguição:** quais classes a fachada coordena e em que ordem; o que é desfeito se um passo falhar no meio;
  por que a interface não acessa Movimentacao diretamente.

### R5 — Observer (reações à devolução)
- **Publicador de devolução** com ouvintes para **inspeção, multa, indicadores e e-mail**.
- O evento contém os dados necessários: **vencimento, data de devolução e estudante**.
- Publicação **após a confirmação** da devolução; **falha de um ouvinte não afeta os demais nem desfaz a devolução**.
- **Cenário:** devolução atrasada alimenta **inspeção, multa e indicadores**; devolução no prazo **não gera multa**;
  o livro **permanece DEVOLVIDO até a decisão da inspeção**.
- **Arguição:** novo ouvinte sem alterar o tratador; o que acontece quando o e-mail falha; por que o evento leva os
  dados em vez de o ouvinte consultá-los depois.

### R6 — Command (solicitações de circulação)
- Solicitações de **reserva e renovação** como objetos com **identificador, autor, instante de criação e operação de
  execução**. As telas criam a solicitação e a encaminham a um **executor**, que garante que **o mesmo identificador
  não seja executado duas vezes**. As regras continuam no domínio.
- **Cenário:** renovação executada 2× com o mesmo id acrescenta **apenas 30 dias**; solicitações diferentes continuam
  sujeitas ao limite de 2 renovações.
- **Arguição:** quem cria, quem invoca e quem executa; como a repetição é detectada **mesmo com duas submissões
  simultâneas**; por que não há operação de desfazer.

### R7 — Template Method (cadastros)
- Classe abstrata com o **método-modelo de inclusão, que não pode ser sobrescrito (`final`)**, fixando a ordem:
  **buscar existente → validar → verificar pré-requisitos → criar → armazenar → apresentar**.
- **Livro, Pessoa, Estudante e Atendente** implementam os passos variáveis, **preservando as interfaces `iLivro`,
  `iPessoa`, `iEstudante` e `iAtendente`**.
- **Cenário:** incluir estudante duplicado **retorna o cadastro existente**; incluir estudante associado a **CPF não
  cadastrado falha antes de qualquer gravação**.
- **Arguição:** passos fixos × variáveis; o que é um **gancho** e onde é usado; por que falha de pré-requisito não
  permite gravação parcial.

### R8 — Abstract Factory (acesso a dados)
- Fábrica abstrata que cria a **unidade de trabalho** e os **repositórios de Livro, Movimentacao e Relacionamento**;
  uma fábrica **em memória** e outra **relacional** (MySQL ou equivalente aprovado).
- A fábrica é **escolhida uma única vez pelo perfil de execução**. O **Oracle continua réplica e não recebe escritas**.
- **Cenário:** configuração de teste fornece **apenas objetos em memória**; configuração de produção fornece
  **repositórios MySQL que compartilham a mesma unidade de trabalho**.
- **Arguição:** onde a família é escolhida; como os repositórios compartilham a transação; o que impede misturar
  repositório em memória com relacional.

### R9 — Decorator (consulta ao acervo)
- Contrato de consulta, **implementação original sem alterações** e decoradores de **medição** e **cache**,
  combináveis conforme a instalação. **O cache armazena apenas metadados**; a **reserva consulta sempre o estado
  atual do Livro**.
- **Cenário:** a 2ª consulta de metadados pode usar cache **e ainda assim gerar medição**; resultado antigo do cache
  **nunca autoriza a reserva** de um livro que deixou de estar LIVRE.
- **Arguição:** como a cadeia é montada e por que a ordem importa; o que acontece se o cache falhar; por que o cache
  não participa da decisão de reserva.

### R10 — Composite (organização do acervo)
- **Unidades, seções e subseções** como grupos de uma árvore de profundidade variável, com **itens que referenciam
  livros pelo identificador**. Grupos e itens respondem às **mesmas operações de contagem e listagem**.
- O grupo **recusa ciclos** e **livros que já pertencem a outro grupo**.
- **Cenário:** unidade com 2 seções (3 e 2 livros) informa **total 5**; **mover um livro entre seções não altera id,
  estado nem status de empréstimo**.
- **Arguição:** como a contagem percorre a árvore; regras contra ciclos e duplicidade; por que mover não toca em
  Livro nem em Movimentacao.

## Requisitos técnicos
- **Java 17+**, com **Maven ou Gradle**.
- Interface **web ou linha de comando**; **a interface chama apenas os tratadores** e não contém regras de domínio.
- Persistência: **duas famílias** (memória para testes; relacional MySQL para produção), **ambas criadas pela fábrica do R8**.
- **Um teste automatizado (JUnit) por cenário**, com nome que identifique o requisito, ex.: `R01_terceiraRenovacaoRecusada`.
- Projeto compila e testes rodam com **um único comando descrito no README**, sem configuração manual além do banco.

## Uso de LLMs (obrigações)
- README informa **ferramentas e modelos usados** e **em quais partes**.
- **`REGISTRO_LLM.md`** resume os principais pedidos e o que foi **aceito, corrigido ou descartado**.
- Código que nenhum integrante consegue explicar = **não implementado**. **Todos** respondem sobre **qualquer** parte.

## Entregáveis
1. Repositório com código, testes e README (compilar, executar, testar).
2. **Modelos UML atualizados no Astah (.asta) e exportados em PDF**: diagrama de classes **de cada padrão**,
   **máquina de estados de Livro** e **diagramas de sequência dos cenários**.
3. **Tabela de rastreabilidade**: requisito → padrão → classes por papel → teste.
4. `REGISTRO_LLM.md`.

## ⚠️ Pontos a relevar — orientação verbal do professor que difere do PDF
| # | O PDF diz | O professor disse verbalmente | Como o projeto trata |
|---|---|---|---|
| PR1 | Entregar "diagrama de classes **de cada padrão**" atualizado no Astah; coerência código × UML vale 15%; R1 pede "máquina de estados **alinhada ao diagrama de classes**". | **Não é preciso deixar os diagramas de classes UML corretamente modelados**; o desenho com base nos padrões fica a cargo do desenvolvimento (feito com apoio do LLM). | Diagramas de classes **não** serão atualizados para os padrões. O desenho de referência é `04-arquitetura-alvo.md` + o código. Os demais diagramas seguem o plano de correção (ver `06-decisoes.md`, grupo A). **Risco:** como o PDF continua dizendo o contrário, pedir confirmação por escrito (e-mail/Teams) e citar na apresentação/README. |

## Demonstração (5 min por equipe, 27/11/2026)
1. Executar os testes dos 10 cenários — **todos aprovados**.
2. Pela interface, **um fluxo completo**: reserva → empréstimo → renovação → **devolução atrasada** → **multa**.
3. Arguição individual sobre um trecho de código (o que faz, papel no padrão, o que aconteceria se alterado).
4. **Alteração ao vivo**: ex. **nova política de multa** ou **novo ouvinte da devolução**, sem alterar classes existentes.

## Avaliação
| Critério | Peso |
|---|---|
| Aplicação correta dos 10 padrões, papéis identificáveis no código | 40% |
| Cenários de verificação automatizados e aprovados | 20% |
| Coerência código × UML (incluindo máquina de estados) | 15% |
| Separação de responsabilidades, tratamento de falhas, regras de domínio | 15% |
| Documentação: README, rastreabilidade, registro de LLM | 10% |

Nota individual = nota da equipe × fator de arguição (1,0 / 0,7 / 0,4 / 0).

## Prazos
- Equipes de até 4. **Entrega do repositório: 27/11/2026 às 09h40.** Demonstração no mesmo dia.
