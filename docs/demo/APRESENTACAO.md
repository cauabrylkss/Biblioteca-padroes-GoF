# Apresentação (27/11) — roteiro de 5 minutos

Guia da equipe para a demonstração. Os comandos e as duas alterações ao vivo foram executados em 25/11 nesta máquina
(tempos medidos: `mvnw test` ≈ 10 s, `mvnw package -DskipTests` ≈ 10 s, roteiro da CLI no MySQL ≈ 5 s).

## Antes de começar (na máquina da apresentação)
- [ ] `git pull` na `main`; `java -version` mostra 25 (se mostrar 1.8, abrir um terminal novo).
- [ ] Serviço `MySQL` rodando; `BIBLIOTECA_DB_SENHA` definida no terminal que será usado.
- [ ] `./mvnw package` uma vez (gera `target/biblioteca.jar` e já confirma os testes).
- [ ] `java -jar target/biblioteca.jar --reiniciar-dados` e sair com `0` — deixa o banco com os dados de exemplo.
- [ ] Conferir os acentos no terminal escolhido (cmd, PowerShell ou VS Code): a primeira tela mostra "padrões".
- [ ] Editor aberto em `config/Aplicacao.java` (métodos `politicasDeMulta()` e `registrarOuvintes()`).
- [ ] Sem MySQL? Usar `--perfil=memoria` em todos os comandos da CLI (os ids e o boleto 1001 continuam iguais).

## Roteiro
| Tempo | O que fazer | O que dizer |
|---|---|---|
| 0:00–0:45 | `./mvnw test` | "Um teste por cenário de verificação, `R01_` a `R10_`, todos verdes, sem banco (perfil de memória)." |
| 0:45–2:45 | `java -jar target/biblioteca.jar --reiniciar-dados --roteiro=docs/demo/roteiro-demo.txt` | Narrar o fluxo enquanto ele aparece (abaixo). |
| 2:45–4:45 | Alteração ao vivo (abaixo) | "Uma classe nova + uma linha no composition root; nenhuma classe existente muda." |
| 4:45–5:00 | `./mvnw test` de novo (opcional) | "Nada quebrou." |

### O que aparece no roteiro (para narrar)
1. **Estudante Ana** consulta o livro 1: título pela **consulta ao acervo** (R9 — medida e com cache) e **situação atual
   LIVRE** lida do repositório.
2. Ana **solicita reserva** → solicitação vira objeto (**Command**, R6) → **fachada** (R4) → evento no livro (**State**,
   R1) → RESERVADO. Consulta de novo: título vem do **cache**, situação RESERVADO vem do repositório (cache nunca
   decide reserva).
3. **Atendente Bruno** realiza o empréstimo (só a partir de RESERVADO, mesmo estudante) → EMPRESTADO, vence em 30 dias.
4. Ana **renova** → RENOVADO, +30 dias (guarda `numRenovacoes < 2`).
5. **Ferramentas** avançam o relógio 65 dias (relógio injetado).
6. Bruno **registra a devolução** → DEVOLVIDO, 5 dias de atraso. Os **ouvintes** (Observer, R5) reagem depois do
   commit: inspeção pendente, **multa** R$ 5,00 pela política vigente (**Strategy**, R2) com **boleto** 1001 no Banco A
   (**Adapter**, R3), indicadores e e-mail.
7. Bruno **inspeciona** (bom estado) → LIVRE.
8. **Pagamento simulado** do boleto: o banco manda a notificação no formato dele, o adaptador traduz → boleto PAGO,
   multa QUITADA. Repetir o pagamento → **nada muda** (quitação idempotente).
9. Caixa de e-mails (boleto, devolução, quitação); medições da consulta (2 chamadas, a 2ª pelo cache); indicadores.

Tudo isso grava no **MySQL** (Abstract Factory, R8: perfil `producao` escolhe a família relacional uma única vez).
Cadastros dos dados de exemplo passaram pelo **Template Method** (R7) e os livros estão na árvore do acervo
(**Composite**, R10: Administrador → Acervo → Organização).

Se preferir digitar em vez de usar o roteiro, as respostas estão em `docs/demo/roteiro-demo.txt`, na ordem.

## Alteração ao vivo
O professor escolhe. As duas foram feitas e testadas; cada uma leva ~2 min. **Não commitar** depois (desfazer com
`git checkout -- .` e apagar a classe nova).

### A) Nova política de multa — `PoliticaDobroAposDezDias` (Strategy, R2)
1. Criar `src/main/java/br/pucpr/biblioteca/financeiro/PoliticaDobroAposDezDias.java`:
   ```java
   package br.pucpr.biblioteca.financeiro;

   import java.math.BigDecimal;

   /** Papel GoF: ConcreteStrategy (Strategy, R2) — R$ 1,00/dia até 10 dias; R$ 2,00/dia a partir do 11º. */
   public final class PoliticaDobroAposDezDias implements PoliticaDeMulta {

       @Override
       public BigDecimal calcular(long diasDeAtraso) {
           PoliticaDeMulta.exigirDiasNaoNegativos(diasDeAtraso);
           long ateDez = Math.min(diasDeAtraso, 10);
           long depoisDeDez = Math.max(diasDeAtraso - 10, 0);
           return UM_REAL_POR_DIA.multiply(BigDecimal.valueOf(ateDez + 2 * depoisDeDez));
       }

       @Override
       public String codigo() {
           return "dobro";
       }

       @Override
       public String descricao() {
           return "R$ 1,00 por dia até 10 dias; R$ 2,00 por dia depois";
       }
   }
   ```
2. Registrar em `config/Aplicacao.java`, método `politicasDeMulta()`:
   ```java
   return List.of(new PoliticaOriginal(), new PoliticaComTeto(), new PoliticaComTolerancia(),
           new PoliticaDobroAposDezDias());
   ```
   (+ `import br.pucpr.biblioteca.financeiro.PoliticaDobroAposDezDias;`)
3. `./mvnw -q package -DskipTests` e rodar a CLI (`java -jar target/biblioteca.jar --reiniciar-dados`).
4. Mostrar: **3** Administrador → **3** Política de multa → digitar `dobro`. Depois um atraso de 12 dias:
   Estudante 1 reserva o livro 1 → Atendente 1 empresta (livro 1, estudante 1) → Ferramentas → avançar **42** dias →
   Atendente 1 registra a devolução (livro 1, estudante 1) → **multa R$ 14,00, política dobro**.
   Respostas, na ordem: `3 3 dobro 0 1 1 3 1 0 2 1 1 1 1 0 4 1 42 0 2 1 2 1 1 0`.
5. Pontos para falar: `ServicoDeMultas` (Context) não mudou e não tem `if` por política; a multa grava valor **e**
   código da política; multas antigas continuam com a política antiga.

### B) Novo ouvinte da devolução — `OuvinteAuditoria` (Observer, R5)
1. Criar `src/main/java/br/pucpr/biblioteca/biblioteca/circulacao/OuvinteAuditoria.java`:
   ```java
   package br.pucpr.biblioteca.biblioteca.circulacao;

   /** Papel GoF: ConcreteObserver (Observer, R5) — registra cada devolução numa trilha de auditoria. */
   public class OuvinteAuditoria implements OuvinteDeDevolucao {

       @Override
       public void aoDevolver(EventoDevolucao evento) {
           System.out.println("[Auditoria] livro " + evento.idLivro() + " devolvido pelo estudante "
                   + evento.idEstudante() + " (atraso: " + evento.diasDeAtraso() + " dia(s))");
       }
   }
   ```
2. Registrar em `config/Aplicacao.java`, método `registrarOuvintes()`, como última linha:
   ```java
   publicador.registrar(new OuvinteAuditoria());
   ```
   (+ `import br.pucpr.biblioteca.biblioteca.circulacao.OuvinteAuditoria;`)
3. `./mvnw -q package -DskipTests`, rodar a CLI e fazer uma devolução (pode ser o roteiro inteiro:
   `--reiniciar-dados --roteiro=docs/demo/roteiro-demo.txt`) → aparece `[Auditoria] livro 1 devolvido ...`.
4. Pontos para falar: fachada e tratadores não mudaram; o publicador chama cada ouvinte com `try/catch` (falha de um
   não afeta os outros nem desfaz a devolução — dá para mostrar em Ferramentas → **5** falha do e-mail); publicação
   acontece **depois do commit**.

`./mvnw test` continua verde com qualquer uma das duas alterações (conferido).

## Se algo der errado
| Problema | O que fazer |
|---|---|
| "Não foi possível iniciar: Senha do MySQL não definida" | Definir `BIBLIOTECA_DB_SENHA` no terminal ou usar `--perfil=memoria`. |
| Erro de conexão com o MySQL | `--perfil=memoria` (o fluxo é idêntico; só não grava). |
| Boleto não é 1001 / ids diferentes | Faltou `--reiniciar-dados` (com dados de execuções anteriores os ids continuam a contagem). |
| Acentos estranhos | Usar outro terminal (cmd ou VS Code) — a CLI troca o console para UTF-8 sozinha. |
| `java -version` mostra 1.8 | Abrir terminal novo (ou pôr `%JAVA_HOME%\bin` no início do PATH). |

## Arguição (individual)
- Cada padrão tem um guia em `docs/estudo/Rxx-*.md` com papéis, perguntas do PDF e perguntas prováveis.
- Mapa requisito → classes → testes: `docs/RASTREABILIDADE.md`.
- UML: os diagramas não foram refatorados (A-09). Se perguntarem por divergências (ex.: nomes de eventos na máquina
  de estados, "Interface Web", implantação), a lista e a correção de cada uma estão em `docs/contexto/03-revisao-uml.md`.
- Limitações conhecidas (e por quê): `docs/contexto/06-decisoes.md` E-12 (estado em memória) e C-04 (ids de
  solicitações em memória).
