# Biblioteca-padroes-GoF

Sistema de cadastro, reserva, empréstimo, renovação e devolução de livros, implementado em Java com os **10 padrões
GoF** exigidos no trabalho de Modelagem de Sistemas Computacionais (PUCPR, Prof. Edson Emílio Scalabrin, 2S-2026).

**Equipe:** Vicente, Gabriel, Clara, Cauã.

> Entrega em 27/11/2026, 09h40. Os 10 requisitos estão implementados e testados; o mapa requisito → padrão →
> classes → teste está em [docs/RASTREABILIDADE.md](docs/RASTREABILIDADE.md).

## Requisitos
- **JDK 21 ou superior** (desenvolvido com o JDK 25; o bytecode é Java 21) com `JAVA_HOME` configurado.
- Maven **não** precisa estar instalado: o projeto traz o Maven Wrapper (`mvnw` / `mvnw.cmd`).
- **MySQL 8** para a interface no perfil padrão (produção). Os testes não usam banco; sem MySQL, a interface roda
  com `--perfil=memoria`.

## Compilar e rodar os testes (comando único)
```bash
./mvnw test          # Git Bash / Linux / macOS
mvnw.cmd test        # PowerShell / cmd
```
Roda todos os cenários de verificação `R01_…` a `R10_…` no perfil em memória, sem configuração adicional.
Testes de integração com MySQL real (marcados `@Tag("mysql")`) só rodam com `./mvnw test -Pmysql`.

## Executar a interface (web)
```bash
./mvnw package                                   # gera target/biblioteca.jar (também roda os testes)
java -jar target/biblioteca.jar                  # interface web + perfil de produção (MySQL) — padrão
java -jar target/biblioteca.jar --reiniciar-dados   # apaga os dados do MySQL e recria os de exemplo
java -jar target/biblioteca.jar --perfil=memoria    # sem banco (plano B): dados só nesta execução
java -jar target/biblioteca.jar --porta=8081 --sem-navegador
java -jar target/biblioteca.jar --ajuda
```
O programa sobe um servidor HTTP em **http://localhost:8080** (o `HttpServer` que já vem no JDK — sem Spring nem
Tomcat) e abre o navegador. **Ctrl+C** no terminal encerra. Atalho sem empacotar:
`./mvnw compile exec:java -Dexec.args="--perfil=memoria"`.

A página pergunta **"operar como"** (Estudante, Atendente, Administrador ou Ferramentas da demonstração) — não há
login. Cada formulário chama **uma operação de um tratador**: `POST /api/iEmprestimo/solicitarReserva`,
`POST /api/iDevolucao/registrarDevolucao`, `GET /api/iLivro/consultarAcervo`… (o caminho aparece no histórico). A
página não tem regra de domínio: mostra **OK**, **Recusado** (HTTP 409, o livro não muda) ou **Erro**, e o painel
**Acervo agora** mostra a situação de cada livro, relida do repositório depois de cada operação.

Com o banco vazio são cadastrados dados de exemplo: estudantes **Ana (1)** e **Carla (2)**, atendente **Bruno (1)**,
livros **1 a 5** organizados em unidade/seções/subseção. As ferramentas da demonstração avançam o relógio (para
simular atraso), simulam o pagamento de um boleto no banco e mostram a caixa de saída de e-mails.

O fluxo da demonstração pela web (**reserva → empréstimo → renovação → devolução atrasada → multa** + inspeção e
pagamento do boleto) está em [docs/demo/APRESENTACAO.md](docs/demo/APRESENTACAO.md) e é executado por HTTP pelo
teste `InterfaceWebTest` (perfil em memória).

### CLI (reserva)
A interface de linha de comando continua disponível, com os mesmos tratadores e as mesmas mensagens:
```bash
java -jar target/biblioteca.jar --interface=cli
java -jar target/biblioteca.jar --reiniciar-dados --roteiro=docs/demo/roteiro-demo.txt   # --roteiro já escolhe a CLI
```
[docs/demo/roteiro-demo.txt](docs/demo/roteiro-demo.txt) responde as perguntas da CLI na ordem do fluxo da
demonstração; depois da última linha, a CLI volta para o teclado. O mesmo roteiro é executado pelo teste
`InterfaceCliTest`.

## Perfil de produção (MySQL)
1. No MySQL Workbench, execute uma vez [docs/mysql/criar-banco.sql](docs/mysql/criar-banco.sql), trocando a senha.
2. Defina as variáveis de ambiente `BIBLIOTECA_DB_USUARIO` e `BIBLIOTECA_DB_SENHA` (opcional: `BIBLIOTECA_DB_URL`)
   e abra um terminal novo.
3. Rode `java -jar target/biblioteca.jar`. As tabelas são criadas automaticamente na primeira execução.

Ficam só em memória (recomeçam a cada execução): a fila de pendências de inspeção, os indicadores de devolução, o
registro de solicitações já executadas (R6), a organização do acervo (recriada com os livros de exemplo) e os bancos
simulados (que, ao subir, retomam o que está gravado: continuam a numeração dos boletos e dos eventos de pagamento
e voltam a aceitar o pagamento dos boletos ainda em aberto — D-12, D-13).

## Configuração
Valores padrão em [src/main/resources/application.properties](src/main/resources/application.properties); qualquer chave
pode ser sobrescrita com `-Dchave=valor` (ex.: `-Dmulta.politica=tolerancia`, `-Dbanco=B`).

## Os 10 padrões
| Req | Situação | Padrão |
|---|---|---|
| R1 | Ciclo de vida do livro | State |
| R2 | Políticas de cálculo de multa | Strategy |
| R3 | Integração com bancos incompatíveis | Adapter |
| R4 | Entrada única para as operações de circulação | Facade |
| R5 | Reações independentes à devolução | Observer |
| R6 | Solicitações de circulação como objetos | Command |
| R7 | Rotina comum para cadastros | Template Method |
| R8 | Famílias compatíveis de acesso aos dados | Abstract Factory |
| R9 | Serviço de consulta com recursos opcionais | Decorator |
| R10 | Organização hierárquica do acervo | Composite |

Classes por papel e teste de cada cenário: [docs/RASTREABILIDADE.md](docs/RASTREABILIDADE.md).

## Estrutura
```
src/main/java/br/pucpr/biblioteca/
  biblioteca/   livro (State) · acervo (Decorator, Composite) · movimentacao · circulacao (Facade, Observer, Command) · tratadores
  rh/  academico/  cadastro (Template Method)  financeiro (Strategy) · financeiro/cobranca (Adapter)  banco (bancos simulados)
  persistencia/ (Abstract Factory) · memoria · jdbc     interfaceusuario/ (web · CLI)     config/ (composition root)
src/main/resources/web/   index.html · app.js · estilo.css (página da interface web)
docs/
  *.asta                 modelos UML (Astah)        diagramas/  exportações
  contexto/              enunciado, revisão da UML, arquitetura, plano e decisões
```
Os pacotes correspondem aos componentes do diagrama de componentes (Biblioteca, RH, Acadêmico, Financeiro, Banco).

## Modelos UML
Fontes no Astah em `docs/*.asta`, entregues **como estavam no início do trabalho**. **Observação:** por orientação
verbal do professor, os **diagramas de classes não foram atualizados** para os padrões; o desenho dos padrões está no
código e em [docs/contexto/04-arquitetura-alvo.md](docs/contexto/04-arquitetura-alvo.md). Os demais diagramas também
não foram refatorados (decisão da equipe); as diferenças entre eles e o código — e como cada uma seria corrigida —
estão em [docs/contexto/03-revisao-uml.md](docs/contexto/03-revisao-uml.md).

## Uso de LLMs
| Ferramenta / modelo | Onde foi usado |
|---|---|
| Claude Code (Claude Opus 5.5) | Leitura do enunciado e dos modelos `.asta`, revisão da UML, plano de ação, registro de decisões, estrutura do projeto (Fase 0). As próximas partes serão listadas aqui conforme forem feitas. |
| Claude Code (Claude Opus 5.5) | R1 State: pacote `biblioteca.livro` (Livro e os 7 estados), transações `Reserva`/`Emprestimo` e `R01StateTest`; guia `docs/estudo/R01-state.md`. |
| Claude Code (Claude Opus 5.5) | R2 Strategy: pacote `financeiro` (políticas de multa, configuração administrativa, serviço de multas, `Multa`) e `R02StrategyTest`; guia `docs/estudo/R02-strategy.md`. |
| Claude Code (Claude Opus 5.5) | R3 Adapter: bancos simulados (`banco`), contrato e adaptadores de cobrança (`financeiro.cobranca`), boleto e quitação idempotente no `ServicoDeMultas`, escolha do banco em `config`, `R03AdapterTest`; guia `docs/estudo/R03-adapter.md`. |
| Claude Code (Claude Opus 5.5) | R10 Composite: árvore do acervo em `biblioteca.acervo` (grupos, itens, organização) e `R10CompositeTest`; guia `docs/estudo/R10-composite.md`. |
| Claude Code (Claude Opus 5.5) | R7 Template Method, R4 Facade, R5 Observer, R6 Command e R8 (memória): cadastros, fachada de circulação, ouvintes da devolução, solicitações e persistência em memória, com os testes `R04`–`R08` e os guias `docs/estudo/`. |
| Claude Code (Claude Opus 5.5) | R8 relacional: pacote `persistencia.jdbc` (MySQL), `schema.sql`, `R08MySqlIntegracaoTest`. R9 Decorator: consulta ao acervo com medição e cache, `R09DecoratorTest`. |
| Claude Code (Claude Opus 5.5) | Interface CLI (`interfaceusuario`), `iAdministracao`, dados de exemplo, roteiro da demonstração, e-mails de boleto/quitação e numeração do banco simulado; `InterfaceCliTest`. |
| Claude Code (Claude Opus 5.5) | Interface web (`interfaceusuario.web`: `ServidorWeb`, `InterfaceWeb`, `Json`, `Requisicao`; página `src/main/resources/web/`), textos compartilhados com a CLI (`interfaceusuario.Mensagens`), opções `--interface`/`--porta`; `InterfaceWebTest`. |

Pedidos feitos e o que a equipe aceitou, corrigiu ou descartou: [REGISTRO_LLM.md](REGISTRO_LLM.md).
