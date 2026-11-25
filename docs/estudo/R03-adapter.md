# R3 — Adapter: guia de estudo para a arguição

Código: `src/main/java/br/pucpr/biblioteca/financeiro/cobranca/` (contrato e adaptadores), `.../banco/` (bancos
simulados), `.../financeiro/ServicoDeMultas.java` (cliente), `.../config/BancoDaInstalacao.java` (escolha do banco).
Testes: `src/test/java/.../financeiro/cobranca/R03AdapterTest.java`

## A ideia em uma frase
Os bancos A e B têm APIs que não "encaixam" no sistema; para cada um existe um **adaptador** que implementa o
**contrato institucional** de cobrança e traduz tudo (ida e volta), então o serviço de multas nunca fala "a língua"
de um banco.

## Papéis
| Papel GoF | Classe | O que faz |
|---|---|---|
| Target | `ServicoDeCobranca` | `registrar(SolicitacaoCobranca)`, `traduzirNotificacao(String)`, `banco()` |
| Adapter | `AdaptadorBancoA`, `AdaptadorBancoB` | Implementam o Target e chamam a API do seu banco, convertendo os dados |
| Adaptee | `BancoAApi`, `BancoBApi` | APIs externas simuladas, cada uma com seu formato; não sabem nada da biblioteca |
| Client | `ServicoDeMultas` | Emite boleto e quita multa usando só `ServicoDeCobranca` |

## As conversões (estão todas nos adaptadores)
| Dado | Institucional | Banco A | Banco B |
|---|---|---|---|
| Valor | `BigDecimal 7.00` | `700` (centavos, `long`) | `"7,00"` (texto com vírgula) |
| CPF | `12345678909` | `12345678909` | `123.456.789-09` |
| Identificador enviado | id da multa (`long`) | nosso número `long` | referência `"MULTA-15"` |
| Identificador devolvido | texto | código `long` → texto | `"B-000001"` |
| Situação | `EM_ABERTO`, `PAGO`, `CANCELADO` | `1`, `2`, `9` | `ABERTO`, `LIQUIDADO`, `BAIXADO` |
| Notificação | `NotificacaoPagamento` | `evento=EVT-A-1;titulo=1001;valorCentavos=700;status=2` | `LIQUIDADO\|B-000001\|7,00\|ev-b-1` |

## Perguntas do PDF
**Onde ficam as conversões de cada banco?**
Só nos adaptadores: `AdaptadorBancoA` (`paraCentavos`, `deCentavos`, `traduzirSituacao(int)`, parse da notificação)
e `AdaptadorBancoB` (`paraTextoComVirgula`, `deTextoComVirgula`, `formatarCpf`, `traduzirSituacao(String)`, parse da
notificação). As APIs dos bancos não foram alteradas, e o `ServicoDeMultas` não converte nada.

**Como o banco usado pela instalação é escolhido?**
Pela propriedade `banco=A|B` (`application.properties` ou `-Dbanco=B`). Na montagem da aplicação,
`BancoDaInstalacao.criar(banco)` cria a API e o adaptador certos e entrega ao `ServicoDeMultas` só a interface
`ServicoDeCobranca`. É o único `switch` por banco do sistema e roda uma vez. O teste
`R03_servicoDeMultasNaoDependeDeNenhumBanco` confere que o serviço não tem campo nem parâmetro de tipo de banco.

**Como a quitação é protegida contra notificações repetidas?**
Em `ServicoDeMultas.registrarPagamento`, em duas camadas:
1. o id do evento do banco é registrado (`RepositorioDeMultas.registrarNotificacaoProcessada`); a mesma notificação
   entregue de novo devolve `false` e é ignorada;
2. mesmo um evento **diferente** sobre uma multa já `QUITADA` é ignorado.

O método é `synchronized`, então duas notificações simultâneas não passam juntas pela verificação
(`R03_notificacoesSimultaneasQuitamUmaVezSo` dispara 16 ao mesmo tempo e só uma quita).

## Perguntas prováveis além do PDF
- **Por que não colocar um `if (banco == "A")` no serviço?** Cada banco novo obrigaria a mexer no serviço, e as
  regras de conversão de todos os bancos ficariam misturadas com as regras de multa.
- **Adapter × Strategy — não é a mesma coisa?** Strategy troca *algoritmos* que já seguem a mesma interface; Adapter
  faz uma interface *incompatível* parecer a esperada. Os bancos não foram feitos para o nosso contrato — o adaptador
  existe para "encaixar".
- **Por que a notificação chega como texto?** Porque cada banco manda no seu formato (como um webhook real); o
  adaptador é quem sabe ler.
- **E se o banco mandar uma notificação "em aberto" ou "cancelado"?** É traduzida para `EM_ABERTO`/`CANCELADO` e o
  serviço devolve `IGNORADA_SEM_PAGAMENTO` sem alterar nada.
- **Alteração ao vivo provável: "adicione o banco C".** Nova API simulada em `banco`, novo `AdaptadorBancoC` em
  `financeiro.cobranca` e um `case "C"` em `BancoDaInstalacao`. `ServicoDeMultas` não muda.
