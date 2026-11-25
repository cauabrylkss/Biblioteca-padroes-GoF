/**
 * Livro e seu ciclo de vida.
 * <p>Padrão <b>State (R1)</b>: {@code Livro} é o Context; {@code EstadoLivro} é o State; as sete classes
 * {@code Livre}, {@code Reservado}, {@code Emprestado}, {@code Renovado}, {@code Devolvido}, {@code Reparando} e
 * {@code Incinerado} são os ConcreteStates. Cada transição da máquina de estados é um método da classe do estado de origem.
 */
package br.pucpr.biblioteca.biblioteca.livro;
