# Biblioteca-padroes-GoF

Sistema de cadastro, reserva, empréstimo, renovação e devolução de livros, implementado em Java com os **10 padrões
GoF** exigidos no trabalho de Modelagem de Sistemas Computacionais (PUCPR, Prof. Edson Emílio Scalabrin, 2S-2026).

**Equipe:** Vicente, Gabriel, Clara, Cauã.

> Projeto em desenvolvimento: entrega em 27/11/2026, 09h40. O andamento de cada requisito está em
> [docs/RASTREABILIDADE.md](docs/RASTREABILIDADE.md).

## Requisitos
- **JDK 21 ou superior** (desenvolvido com o JDK 25; o bytecode é Java 21) com `JAVA_HOME` configurado.
- Maven **não** precisa estar instalado: o projeto traz o Maven Wrapper (`mvnw` / `mvnw.cmd`).
- **MySQL 8** apenas para o perfil de produção (os testes não usam banco).

## Compilar e rodar os testes (comando único)
```bash
./mvnw test          # Git Bash / Linux / macOS
mvnw.cmd test        # PowerShell / cmd
```
Roda todos os cenários de verificação `R01_…` a `R10_…` no perfil em memória, sem configuração adicional.
Testes de integração com MySQL real (marcados `@Tag("mysql")`) só rodam com `./mvnw test -Pmysql`.

## Executar a interface (CLI)
```bash
./mvnw package                      # gera target/biblioteca.jar (também roda os testes)
java -jar target/biblioteca.jar     # perfil em memória, com dados de exemplo
java -Dperfil=producao -jar target/biblioteca.jar   # perfil de produção (MySQL)
```
Atalho sem empacotar: `./mvnw compile exec:java`.

## Perfil de produção (MySQL)
1. No MySQL Workbench, execute uma vez [docs/mysql/criar-banco.sql](docs/mysql/criar-banco.sql), trocando a senha.
2. Defina as variáveis de ambiente `BIBLIOTECA_DB_USUARIO` e `BIBLIOTECA_DB_SENHA` (opcional: `BIBLIOTECA_DB_URL`).
3. Rode com `-Dperfil=producao`. As tabelas são criadas automaticamente na primeira execução.

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
  persistencia/ (Abstract Factory) · memoria · jdbc     interfaceusuario/ (CLI)     config/ (composition root)
docs/
  *.asta                 modelos UML (Astah)        diagramas/  exportações
  contexto/              enunciado, revisão da UML, arquitetura, plano e decisões
```
Os pacotes correspondem aos componentes do diagrama de componentes (Biblioteca, RH, Acadêmico, Financeiro, Banco).

## Modelos UML
Fontes no Astah em `docs/*.asta`. **Observação:** por orientação verbal do professor, os **diagramas de classes não
foram atualizados** para os padrões; o desenho dos padrões está no código e em
[docs/contexto/04-arquitetura-alvo.md](docs/contexto/04-arquitetura-alvo.md). Máquina de estados de Livro, atividades,
componentes e implantação foram revisados conforme [docs/contexto/03-revisao-uml.md](docs/contexto/03-revisao-uml.md).

## Uso de LLMs
| Ferramenta / modelo | Onde foi usado |
|---|---|
| Claude Code (Claude Opus 5.5) | Leitura do enunciado e dos modelos `.asta`, revisão da UML, plano de ação, registro de decisões, estrutura do projeto (Fase 0). As próximas partes serão listadas aqui conforme forem feitas. |
| Claude Code (Claude Opus 5.5) | R1 State: pacote `biblioteca.livro` (Livro e os 7 estados), transações `Reserva`/`Emprestimo` e `R01StateTest`; guia `docs/estudo/R01-state.md`. |
| Claude Code (Claude Opus 5.5) | R2 Strategy: pacote `financeiro` (políticas de multa, configuração administrativa, serviço de multas, `Multa`) e `R02StrategyTest`; guia `docs/estudo/R02-strategy.md`. |
| Claude Code (Claude Opus 5.5) | R3 Adapter: bancos simulados (`banco`), contrato e adaptadores de cobrança (`financeiro.cobranca`), boleto e quitação idempotente no `ServicoDeMultas`, escolha do banco em `config`, `R03AdapterTest`; guia `docs/estudo/R03-adapter.md`. |
| Claude Code (Claude Opus 5.5) | R10 Composite: árvore do acervo em `biblioteca.acervo` (grupos, itens, organização) e `R10CompositeTest`; guia `docs/estudo/R10-composite.md`. |

Pedidos feitos e o que a equipe aceitou, corrigiu ou descartou: [REGISTRO_LLM.md](REGISTRO_LLM.md).
