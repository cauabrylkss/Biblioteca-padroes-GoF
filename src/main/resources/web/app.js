'use strict';
/*
 * Interface web do Sistema de Biblioteca.
 * Só faz três coisas: envia os formulários para /api/{interface}/{operacao}, mostra as mensagens devolvidas
 * e recarrega o painel do acervo. Nenhuma regra de domínio fica aqui — quem decide é o sistema (tratadores → fachada
 * → domínio); a página mostra "OK", "Recusado" ou "Erro" conforme a resposta.
 */

/** Quem está operando ("operar como", sem login): ids informados ao entrar como estudante ou atendente. */
const operador = { idEstudante: null, idAtendente: null };

/** Situação de cada livro na última atualização, para destacar o que mudou (ex.: LIVRE → RESERVADO). */
let situacaoAnterior = new Map();

// ---------------------------------------------------------------------------------------------------------------
// Comunicação com a API
// ---------------------------------------------------------------------------------------------------------------

/** Chama uma operação. GET leva os campos na URL; POST, no corpo (formulário). Devolve {codigo, ok, mensagens, dados}. */
async function chamar(metodo, rota, campos = {}) {
  const parametros = new URLSearchParams(campos);
  const url = '/api/' + rota + (metodo === 'GET' && parametros.size > 0 ? '?' + parametros : '');
  try {
    const resposta = await fetch(url, metodo === 'GET' ? {} : { method: 'POST', body: parametros });
    return { codigo: resposta.status, ...(await resposta.json()) };
  } catch (falha) {
    return { codigo: 0, ok: false, mensagens: ['Sem resposta do servidor (ele foi encerrado?)'], dados: null };
  }
}

// ---------------------------------------------------------------------------------------------------------------
// Formulários das operações
// ---------------------------------------------------------------------------------------------------------------

document.querySelectorAll('form[data-metodo]').forEach(formulario => {
  formulario.addEventListener('submit', async evento => {
    evento.preventDefault();
    const botao = evento.submitter;
    const rota = botao?.dataset.rota ?? formulario.dataset.rota;       // um formulário pode ter um botão por operação
    const campos = new FormData(formulario);
    if (formulario.dataset.com) {
      campos.set(formulario.dataset.com, operador[formulario.dataset.com]);
    }
    const titulo = formulario.dataset.titulo + (botao?.dataset.rota ? ' — ' + botao.textContent : '');
    const resultado = await chamar(formulario.dataset.metodo, rota, campos);
    registrarNoHistorico(titulo, formulario.dataset.metodo, rota, resultado);
    await atualizarTopoEAcervo();
  });
});

// ---------------------------------------------------------------------------------------------------------------
// Operar como
// ---------------------------------------------------------------------------------------------------------------

document.querySelectorAll('.atores button').forEach(botao => {
  botao.addEventListener('click', () => {
    document.querySelectorAll('.atores button').forEach(b => b.classList.toggle('ativo', b === botao));
    document.querySelectorAll('.ator').forEach(painel => {
      painel.hidden = painel.id !== 'ator-' + botao.dataset.ator;
    });
  });
});

const ENTRAR = {
  estudante: { rota: 'iEstudante/consultarEstudante', campo: 'idEstudante' },
  atendente: { rota: 'iAtendente/consultarAtendente', campo: 'idAtendente' },
};

document.querySelectorAll('form[data-entrar]').forEach(formulario => {
  const ator = formulario.dataset.entrar;
  formulario.addEventListener('submit', async evento => {
    evento.preventDefault();
    const id = formulario.elements.id.value;
    const { rota, campo } = ENTRAR[ator];
    const resultado = await chamar('GET', rota, { [campo]: id });
    operador[campo] = resultado.ok ? id : null;
    document.getElementById('quem-' + ator).textContent =
        resultado.ok ? resultado.dados.nomeCompleto + ' (id ' + id + ')' : 'Ninguém';
    document.querySelector('[data-precisa="' + ator + '"]').classList.toggle('liberado', resultado.ok);
    registrarNoHistorico('Operar como ' + ator, 'GET', rota, resultado);
  });
});

// ---------------------------------------------------------------------------------------------------------------
// Histórico de mensagens
// ---------------------------------------------------------------------------------------------------------------

function registrarNoHistorico(titulo, metodo, rota, resultado) {
  const item = elemento('li', resultado.ok ? 'ok' : resultado.codigo === 409 ? 'recusado' : 'erro');
  const cabecalho = elemento('div', 'cabecalho');
  cabecalho.append(elemento('strong', null, titulo), elemento('code', null, metodo + ' /api/' + rota));
  item.append(cabecalho);
  resultado.mensagens.forEach(mensagem => item.append(elemento('p', null, mensagem)));
  if (metodo === 'GET' && Array.isArray(resultado.dados) && resultado.dados.length > 0
      && typeof resultado.dados[0] === 'object') {
    item.append(tabela(resultado.dados));
  }
  document.getElementById('historico').prepend(item);
}

/** Tabela simples com as chaves do primeiro objeto como colunas (listagens: pessoas, multas, e-mails…). */
function tabela(linhas) {
  const colunas = Object.keys(linhas[0]);
  const t = elemento('table', 'dados');
  const cabecalho = elemento('tr');
  colunas.forEach(coluna => cabecalho.append(elemento('th', null, coluna)));
  t.append(cabecalho);
  linhas.forEach(linha => {
    const tr = elemento('tr');
    colunas.forEach(coluna => tr.append(elemento('td', null, texto(linha[coluna]))));
    t.append(tr);
  });
  return t;
}

document.getElementById('limpar').addEventListener('click', () => {
  document.getElementById('historico').replaceChildren();
});

// ---------------------------------------------------------------------------------------------------------------
// Topo (data de hoje, instalação) e painel do acervo
// ---------------------------------------------------------------------------------------------------------------

async function atualizarTopoEAcervo() {
  const [instalacao, acervo] = await Promise.all([
    chamar('GET', 'iAdministracao/descricaoDaInstalacao'),
    chamar('GET', 'iLivro/consultarAcervo'),
  ]);
  if (instalacao.ok) {
    document.getElementById('hoje').textContent = instalacao.dados.hoje;
    document.getElementById('instalacao').textContent = instalacao.dados.descricao;
  }
  if (acervo.ok) {
    mostrarAcervo(acervo.dados);
  }
}

function mostrarAcervo(livros) {
  const corpo = document.getElementById('acervo');
  corpo.replaceChildren();
  if (livros.length === 0) {
    const tr = elemento('tr');
    const td = elemento('td', null, 'Acervo vazio.');
    td.colSpan = 4;
    tr.append(td);
    corpo.append(tr);
  }
  const atual = new Map();
  livros.forEach(livro => {
    atual.set(livro.id, livro.situacao);
    const antes = situacaoAnterior.get(livro.id);
    const mudou = antes !== undefined && antes !== livro.situacao;
    const tr = elemento('tr', mudou ? 'mudou' : null);
    const situacao = elemento('td');
    situacao.append(elemento('span', 'situacao situacao-' + livro.situacao,
        mudou ? antes + ' → ' + livro.situacao : livro.situacao));
    tr.append(elemento('td', null, livro.id), elemento('td', null, livro.titulo), situacao,
        elemento('td', 'grupo', livro.grupo ?? '—'));
    corpo.append(tr);
  });
  situacaoAnterior = atual;
}

// ---------------------------------------------------------------------------------------------------------------
// Utilitários (textContent: o que o usuário digitou nunca vira HTML)
// ---------------------------------------------------------------------------------------------------------------

function elemento(tag, classe, conteudo) {
  const e = document.createElement(tag);
  if (classe) e.className = classe;
  if (conteudo !== undefined) e.textContent = conteudo;
  return e;
}

function texto(valor) {
  if (valor === null || valor === undefined) return '—';
  return typeof valor === 'object' ? JSON.stringify(valor) : String(valor);
}

atualizarTopoEAcervo();
