package ru.job4j.grabber;

import java.time.LocalDateTime;
import java.util.Objects;

/*
  @author Alexey Kuzhelev (aleks2kv1977@gmail.com)
 * @version $Id$
 * @since 28.12.2022
 */

/**
 * Класс описывает модель данных:
 *  - id типа int - идентификатор вакансии (берется из нашей базы данных);
 *  - title типа String - название вакансии;
 *  - link типа String - ссылка на описание вакансии;
 *  - description типа String - описание вакансии;
 *  - created типа LocalDateTime - дата создания вакансии.
 *  Поле description исключено из equals() & hashCode(), потому что оно большое и только замедлит работу этих методов.
 *  Также убираем поля title и created, т.к. они могут дублироваться.
 *  Кроме того поле даты (created) типа LocalDateTime для хешей не рекомендуется использовать.
 */
public class Post {

    private int id;
    private String title;
    private String link;
    private String description;
    private LocalDateTime created;

    public Post(int id, String title, String link, String description, LocalDateTime created) {
        this.id = id;
        this.title = title;
        this.link = link;
        this.description = description;
        this.created = created;
    }

    public Post(String title, String link, String description, LocalDateTime created) {
        this.title = title;
        this.link = link;
        this.description = description;
        this.created = created;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Post post = (Post) o;
        return id == post.id && Objects.equals(link, post.link);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, link);
    }

    @Override
    public String toString() {
        return "Post{"
            + "id=" + id
            + ", title='" + title + '\''
            + ", link='" + link + '\''
            + ", description='" + description + '\''
            + ", created=" + created
            + '}';
    }
}
