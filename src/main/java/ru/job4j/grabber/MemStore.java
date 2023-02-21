package ru.job4j.grabber;

import java.util.ArrayList;
import java.util.List;

/*
  @author Alexey Kuzhelev (aleks2kv1977@gmail.com)
 * @version $Id$
 * @since 22.02.2023
 */

/**
 * Класс описывает хранение данных модели в памяти.
 */
public class MemStore implements Store {
    private final List<Post> posts = new ArrayList<>();
    private int id = 0;

    /**
     * Метод сохраняет элемент в списке
     */
    @Override
    public void save(Post post) {
        if (post.getId() == 0) {
            post.setId(id++);
        }
        posts.add(post);
    }

    /**
     * Метод позволяет извлечь все элементы списка
     */
    @Override
    public List<Post> getAll() {
        return posts;
    }

    /**
     * Метод позволяет извлечь элемент из списка по id
     */
    @Override
    public Post findById(int id) {
        return indexOf(id) == id ? posts.get(id) : null;
    }

    /**
     * Метод ищет индекс элемента в списке, удовлетворяющий условию.
     * Он должен вернуть индекс первого совпавшего с условием элемента,
     * или -1, если совпадений не найдено.
     */
    private int indexOf(int id) {
        int index = -1;
        for (int i = 0; i < posts.size(); i++) {
            if (posts.get(i).getId() == id) {
                index = i;
                break;
            }
        }
        return index;
    }
}
