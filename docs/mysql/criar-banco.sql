-- Prepara o MySQL local para o perfil "producao" (R8, D-07).
-- Executar UMA vez no MySQL Workbench conectado como root (ou outro usuário administrador).
-- As tabelas NÃO são criadas aqui: a aplicação cria o schema sozinha ao subir no perfil producao (D-06).
--
-- Antes de executar, troque 'troque-esta-senha' por uma senha sua e NÃO faça commit da senha real.
-- Depois, defina as variáveis de ambiente (PowerShell):
--   setx BIBLIOTECA_DB_USUARIO biblioteca
--   setx BIBLIOTECA_DB_SENHA   "<a senha escolhida>"
-- (abra um terminal novo para que elas valham)

CREATE DATABASE IF NOT EXISTS biblioteca
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'biblioteca'@'localhost' IDENTIFIED BY 'troque-esta-senha';

-- Somente o banco da aplicação; nenhum privilégio global.
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON biblioteca.* TO 'biblioteca'@'localhost';

FLUSH PRIVILEGES;
