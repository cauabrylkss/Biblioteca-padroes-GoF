# R5 — Observer: guia de estudo para a arguição

Código: `src/main/java/br/pucpr/biblioteca/biblioteca/circulacao/` (`PublicadorDeDevolucao`, `OuvinteDeDevolucao`,
`EventoDevolucao`, `OuvinteInspecao`, `OuvinteMulta`, `OuvinteIndicadores`, `OuvinteEmail`, `FilaDeInspecao`) ·
Registro: `config/Aplicacao.registrarOuvintes()` · Testes: `src/test/java/.../circulacao/R05ObserverTest.java`

## A ideia em uma frase
Quando uma devolução é confirmada, a fachada **avisa** o publicador; cada reação (inspeção, multa, indicadores, e-mail)
é um ouvinte independente — quem devolve não sabe quem reage.

## Papéis
| Papel GoF | Classe | O que faz |
|---|---|---|
| Subject | `PublicadorDeDevolucao` | `registrar`, `remover`, `publicar` (chama cada ouvinte com try/catch próprio) |
| Observer | `OuvinteDeDevolucao` | `aoDevolver(EventoDevolucao)` |
| ConcreteObserver | `OuvinteInspecao` | Põe o livro na `FilaDeInspecao` (não muda o estado) |
| | `OuvinteMulta` | `iMulta.emitirSeHouverAtraso(...)` → Financeiro calcula (R2) e emite boleto (R3) |
| | `OuvinteIndicadores` | Conta devoluções, atrasadas e dias de atraso |
| | `OuvinteEmail` | Envia o aviso de devolução por `ServicoDeEmail` |
| Evento | `EventoDevolucao` | idLivro, idEstudante, idEmprestimo, **vencimento**, **data de devolução** |

```
FachadaDeCirculacao.registrarDevolucao
  ├─ uow.executar { livro.registrarDevolucao(); grava Devolucao; grava livro }   ← commit
  └─ publicador.publicar(evento) → Inspecao → Multa → Indicadores → Email      ← depois do commit
```

## Perguntas do PDF
**Como incluir um novo ouvinte sem alterar o tratador?**
Uma classe nova que implementa `OuvinteDeDevolucao` + uma linha `publicador.registrar(new OuvinteAuditoria())` em
`Aplicacao.registrarOuvintes()`. Tratador, fachada e publicador não mudam
(`R05_novoOuvinteRegistradoSemAlterarTratadorNemFachada`). **Treino ao vivo**: criar `OuvinteAuditoria` que imprime o
evento.

**O que acontece quando o e-mail falha?**
`OuvinteEmail` lança exceção; `publicar` a captura **só para aquele ouvinte**, registra no log e devolve como aviso
(`ComprovanteDevolucao.avisos`). Os outros ouvintes rodam normalmente e a devolução não é desfeita, porque o commit
aconteceu antes (`R05_falhaNoEmailNaoAfetaDemais`, com `email.falhar=true`).

**Por que o evento leva os dados em vez de o ouvinte consultá-los depois?**
Porque os dados são os do **momento da devolução**: depois do commit o empréstimo já está encerrado e poderia até ser
alterado. Levando vencimento, data e estudante, cada ouvinte é independente, não precisa de repositório nem de
transação e o resultado não depende da ordem (`R05_publicacaoAconteceDepoisDoCommitComOsDadosDaDevolucao`).

## Perguntas prováveis além do PDF
- **O livro fica LIVRE depois de devolvido?** Não: fica DEVOLVIDO até o atendente chamar `inspecionarLivro(emBomEstado)`
  (B-17). Enquanto isso não pode ser reservado (`R05_livroPermaneceDevolvidoAteADecisaoDaInspecao`).
- **Devolução no prazo gera multa?** O `OuvinteMulta` chama o Financeiro do mesmo jeito; a política devolve zero e
  nenhuma multa é criada (`R05_devolucaoNoPrazoNaoGeraMulta`).
- **Por que síncrono?** Mais simples de explicar e testar (C-05); a independência vem do try/catch por ouvinte.
