# R1 — State: guia de estudo para a arguição

Código: `src/main/java/br/pucpr/biblioteca/biblioteca/livro/` · Testes: `src/test/java/.../livro/R01StateTest.java`

## A ideia em uma frase
O `Livro` não sabe as regras do próprio ciclo de vida: ele **repassa cada evento ao objeto de estado atual**, e cada
um dos 7 estados é uma classe que **só implementa os eventos que aceita**.

## Papéis
| Papel GoF | Classe | O que faz |
|---|---|---|
| Context | `Livro` | Guarda `estado`; cada método público (`reservar()`, `renovar(emprestimo)`…) chama `estado.<evento>(this, …)` |
| State | `EstadoLivro` | Declara um método por evento. A implementação padrão de todos **lança `TransicaoInvalidaException` sem tocar no livro** |
| ConcreteState | `Livre`, `Reservado`, `Emprestado`, `Renovado`, `Devolvido`, `Reparando`, `Incinerado` | Sobrescrevem só os eventos aceitos; o corpo tem guarda + ação + `livro.mudarPara(próximo)` |

## Mapa transição → método (é a máquina de estados)
| Transição | Onde está |
|---|---|
| LIVRE → RESERVADO `reservar()` | `Livre.reservar` |
| LIVRE → REPARANDO `enviarParaReparo()` | `Livre.enviarParaReparo` |
| LIVRE → fim `excluir()` | `Livre.excluir` |
| RESERVADO → EMPRESTADO `emprestar [mesmo estudante]` | `Reservado.emprestar` |
| EMPRESTADO → RENOVADO `renovar [numRenovacoes < 2] / prorrogar` | `Emprestado.renovar` |
| RENOVADO → RENOVADO `renovar [numRenovacoes < 2] / prorrogar` | `Renovado.renovar` |
| EMPRESTADO/RENOVADO → DEVOLVIDO | `Emprestado.registrarDevolucao`, `Renovado.registrarDevolucao` |
| DEVOLVIDO → LIVRE / REPARANDO `inspecionar` | `Devolvido.inspecionar` |
| REPARANDO → LIVRE `retornarDoReparo()` | `Reparando.retornarDoReparo` |
| REPARANDO → INCINERADO `registrarLaudoIrrecuperavel()` | `Reparando.registrarLaudoIrrecuperavel` |
| INCINERADO | `Incinerado` não sobrescreve nada → tudo é recusado |

## Perguntas do PDF
**Onde está a guarda da renovação e por que é avaliada antes de alterar o vencimento?**
Em `Emprestado.renovar` e `Renovado.renovar`: `if (emprestimo.getNumRenovacoes() >= Emprestimo.LIMITE_RENOVACOES) throw …`.
Ela vem **antes** de `emprestimo.prorrogar()` porque a ação altera o vencimento; se a ordem fosse inversa, a 3ª
renovação somaria 30 dias e só depois seria recusada (o teste `R01_terceiraRenovacaoRecusada` pega isso).

**O que acontece quando um evento chega a um estado que não o aceita?**
O estado concreto não sobrescreveu aquele método, então roda a versão de `EstadoLivro`, que lança
`TransicaoInvalidaException` ("Operação 'reservar' não é permitida para um livro DEVOLVIDO."). Nada no livro foi
alterado, porque a exceção acontece antes de qualquer `mudarPara`. O teste parametrizado
`R01_cadaEstadoAceitaSoAsTransicoesDoDiagramaERecusaAsDemaisSemAlterarOLivro` tenta **todos os eventos em todos os
estados**.

**Como o estado é gravado e reconstituído na persistência?**
Grava-se o nome: `livro.getSituacao()` devolve um `SituacaoLivro` (enum `LIVRE`…`INCINERADO`), que vai para a coluna
`situacao`. Na leitura, `Livro.reconstituir(id, titulo, situacao)` chama `EstadoLivro.de(situacao)`, que devolve o
objeto de estado. Esse `switch` é o único do pacote e só converte dado em objeto — nenhuma regra é decidida ali.

## Perguntas prováveis além do PDF
- **Por que os tratadores não precisam de `if (livro.getSituacao() == …)`?** Porque basta chamar o evento; quem
  recusa é o estado. E `mudarPara` é package-private: fora do pacote ninguém consegue forçar um estado.
- **Por que `INSTANCIA` (uma só instância por estado)?** Os estados não têm dados (livro e empréstimo chegam por
  parâmetro), então não há por que criar um objeto novo a cada transição.
- **Por que as classes de estado não são públicas?** Para que o único jeito de mudar o estado seja disparar um evento
  no `Livro`.
- **Onde fica `numRenovacoes`?** No `Emprestimo` (como no modelo do Trabalho 02). O estado lê o número (guarda) e
  chama `prorrogar()` (ação).
- **E se quisessem um 8º estado (ex.: EXTRAVIADO)?** Nova classe que estende `EstadoLivro`, sobrescreve os eventos
  aceitos, nova constante em `SituacaoLivro` e um `case` em `EstadoLivro.de`; os estados que levam a ele passam a
  chamar `livro.mudarPara(Extraviado.INSTANCIA)`.
