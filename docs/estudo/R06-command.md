# R6 — Command: guia de estudo para a arguição

Código: `src/main/java/br/pucpr/biblioteca/biblioteca/circulacao/` (`SolicitacaoDeCirculacao`, `SolicitacaoDeReserva`,
`SolicitacaoDeRenovacao`, `ExecutorDeSolicitacoes`) · Entrada: `biblioteca/tratadores/TratadorDeEmprestimo.java` ·
Testes: `src/test/java/.../circulacao/R06CommandTest.java`

## A ideia em uma frase
Reserva e renovação viram **objetos** (com id, autor e instante); um executor guarda os ids já recebidos e garante que
o mesmo pedido, enviado duas vezes (clique duplo, reenvio), só tenha efeito uma vez.

## Papéis
| Papel GoF | Classe | O que faz |
|---|---|---|
| Command | `SolicitacaoDeCirculacao` | `UUID id`, `autor`, `criadaEm`, `executar(fachada)` |
| ConcreteCommand | `SolicitacaoDeReserva`, `SolicitacaoDeRenovacao` | Sabem qual operação da fachada chamar |
| Invoker | `ExecutorDeSolicitacoes` | Executa no máximo uma vez por id |
| Receiver | `FachadaDeCirculacao` | Executa de fato (e o domínio decide as regras) |
| Client | interface (CLI) | Cria a solicitação (`SolicitacaoDeRenovacao.nova(autor, relogio, idLivro, idEstudante)`) |

## Perguntas do PDF
**Quem cria, quem invoca e quem executa?**
A interface **cria** e entrega a `iEmprestimo.renovarEmprestimo(solicitacao)`; o tratador repassa ao
`ExecutorDeSolicitacoes`, que **invoca** `solicitacao.executar(fachada)`; a `FachadaDeCirculacao` **executa**
(`renovar`) e o estado do livro aplica a guarda `numRenovacoes < 2`.

**Como a repetição é detectada mesmo com duas submissões simultâneas?**
```java
CompletableFuture<Comprovante> minha = new CompletableFuture<>();
CompletableFuture<Comprovante> anterior = execucoes.putIfAbsent(id, minha);   // atômico (ConcurrentHashMap)
if (anterior != null) return aguardar(anterior);                             // repetido: só espera
minha.complete(solicitacao.executar(receptor));                              // só a 1ª thread chega aqui
```
`putIfAbsent` é atômico: entre duas threads simultâneas, só uma registra o id; a outra recebe o mesmo future e espera
o mesmo comprovante. `R06_submissoesSimultaneasComMesmoIdExecutamUmaVez` dispara 8 threads ao mesmo tempo com a
mesma renovação: vencimento +30 dias, uma renovação.

**Por que não há operação de desfazer?**
Reserva e renovação são fatos registrados que podem ter efeitos fora do sistema (e-mail, boleto, prazo informado ao
estudante). Corrigir é uma **nova** operação de domínio, não apagar a anterior.

## Perguntas prováveis além do PDF
- **Solicitações diferentes passam do limite?** Não: cada uma é executada, mas a 3ª esbarra na guarda do estado
  (`R06_solicitacoesDiferentesRespeitamLimiteDeDuas`). O executor só impede repetição; regra continua no domínio.
- **E se a 1ª execução falhar?** A repetição devolve a **mesma** falha, sem executar de novo (C-03,
  `R06_repeticaoDeSolicitacaoRecusadaDevolveAMesmaRecusaSemExecutarDeNovo`).
- **Por que não `computeIfAbsent` executando dentro?** Executar dentro do mapa travaria outras chaves do mesmo
  "balde" durante a operação; com o future, a execução acontece fora do mapa.
- **Limitação:** os ids ficam em memória e se perdem ao reiniciar (C-04).
