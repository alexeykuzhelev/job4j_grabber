-- sql schema для проекта агрегатор
-- id - первичный ключ
-- name - имя вакансии
-- text - текст вакансии
-- link - текст, ссылка на вакансию (уникальное)
-- created - дата публикации
drop table if exists post;
create table if not exists post (
id serial primary key,
name varchar(255),
text text,
link varchar(255) unique,
created timestamp
);
