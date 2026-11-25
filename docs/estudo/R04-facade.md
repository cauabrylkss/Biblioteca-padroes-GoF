# R4 — Facade: guia de estudo para a arguição

Código: `src/main/java/br/pucpr/biblioteca/biblioteca/circulacao/FachadaDeCirculacao.java` + `Comprovante*.java` ·
Clientes: `biblioteca/tratadores/TratadorDeEmprestimo.java`, `TratadorDeDevolucao.java` ·
Testes: `src/test/java/.../circulacao/R04FacadeTest.java`

## A ideia em uma frase
Toda operação de circulação (reservar, emprestar, renovar, devolver, inspecionar, reparo, laudo) tem **uma porta só**:
a fachada faz todos os passos na ordem certa, dentro de uma transação, e devolve um **comprovante** — a interface nunca
mexe em Movimentacao, Acervo ou Livro diretamente.

## Papéis
| Papel GoF | Classe | O que faz |
|---|---|---|
| Facade | `FachadaDeCirculacao` | Uma operação pública por caso de uso; cada uma em `unidadeDeTrabalho.executar(...)` |
| Subsistemas | `Relacionamento`, `Movimentacao`, `Acervo`, `Livro` | Validar pessoas, registrar transações, ler/gravar o livro, decidir a transição (State) |
| Clientes | `TratadorDeEmprestimo` (`iEmprestimo`), `TratadorDeDevolucao` (`iDevolucao`) | Só delegam |
| Resultado | `Comprovante` (sealed) e seus records | O que aconteceu, sem dar acesso aos objetos do domínio |

## Perguntas do PDF
**Quais classes a fachada coordena e em que ordem? (efetivar empréstimo)**
1. `Relacionamento.exigirEstudante` → 2. `Relacionamento.exigirAtendente` → 3. `Movimentacao.consultarReservaDoLivro`
(sem reserva → recusa, B-04) → 4. `Acervo.exigirLivro` (estado atual, lido do repositório) → 5. `livro.emprestar(reserva,
idEstudante)` — **o estado RESERVADO** confere se a reserva é do mesmo estudante → 6. `Movimentacao.removerTransacao(reserva)`
→ 7. `Movimentacao.inserirTransacao(new Emprestimo(...))` (vencimento = hoje + 30) → 8. `Acervo.atualizarLivro(livro)`.

**O que é desfeito se um passo falhar no meio?**
Tudo. A operação roda em `UnidadeDeTrabalho.executar`: `iniciar()` → passos → `confirmar()`; qualquer exceção chama
`desfazer()`, que volta os repositórios à foto tirada no início. O teste
`R04_falhaNoUltimoPassoDesfazRemocaoDaReservaECriacaoDoEmprestimo` faz o último passo (gravar o livro) falhar: a reserva
removida volta, o empréstimo criado some e o livro continua RESERVADO.

**Por que a interface não acessa Movimentacao diretamente?**
Porque aí a ordem dos passos e a transação ficariam espalhadas pela interface (e repetidas em cada tela). Com a
fachada, a interface só conhece tratadores, os tratadores só conhecem a fachada (`R04_tratadoresDeCirculacaoDependemSoDaFachada`)
e a fachada devolve comprovantes — records imutáveis, nunca coleções nem entidades
(`R04_fachadaDevolveComprovantesNuncaColecoes`). Sem a referência ao objeto, não há como alterar uma reserva "por fora".

## Perguntas prováveis além do PDF
- **A fachada tem `if` sobre o estado do livro?** Não. Ela dispara o evento (`livro.reservar()`, `livro.emprestar(...)`)
  e o estado aceita ou recusa (R1). Os únicos `orElseThrow` são para "não existe reserva/empréstimo".
- **Quem garante que só o estudante do empréstimo renova/devolve?** `Emprestimo.exigirEstudante` (domínio).
- **E a devolução?** Também transacional; os ouvintes (R5) só são avisados **depois** do commit.
- **Por que um `ComprovanteDeSituacao` único?** Inspeção, reparo e laudo só mudam a situação; um record com
  `operacao`, situação anterior e atual basta.
