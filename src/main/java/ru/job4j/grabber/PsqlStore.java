package ru.job4j.grabber;

import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

/*
  @author Alexey Kuzhelev (aleks2kv1977@gmail.com)
 * @version $Id$
 * @since 06.03.2023
 */

/**
 * Класс соединяет отдельные части (Post, Store, Parse) в целое приложение.
 * Post - модель данных (содержит данные поста сайта career.habr.com)
 * Store - реализует хранение и извлечение данных модели в БД
 * Parse - реализует извлечения данных с сайта career.habr.com
 */
public class PsqlStore implements Store, AutoCloseable {

    private final Connection cnn;

    /**
     * В конструкторе устанавливаем подключение к БД
     */
    public PsqlStore(Properties cfg) throws SQLException {
        try {
            Class.forName(cfg.getProperty("jdbc.driver"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        cnn = DriverManager.getConnection(
            cfg.getProperty("url"),
            cfg.getProperty("username"),
            cfg.getProperty("password")
        );
    }

    /**
     * Метод выполняет SQL-запрос в БД с параметрами для записи данных.
     * Чтобы при повторном парсинге не получать исключения из-за уникальности ссылки link,
     * использован INSERT ON CONFLICT для колонки link.
     */
    @Override
    public void save(Post post) {
        try (
            PreparedStatement ps = cnn.prepareStatement(
                "insert into post(name, text, link, created) values (?, ?, ?, ?) "
                    + "on conflict (link) "
                    + "do update set name = excluded.name, "
                    + "text = excluded.text, "
                    + "created = excluded.created",
                Statement.RETURN_GENERATED_KEYS
            )
        ) {
            ps.setString(1, post.getTitle());
            ps.setString(2, post.getDescription());
            ps.setString(3, post.getLink());
            ps.setTimestamp(4, Timestamp.valueOf(post.getCreated()));
            ps.execute();
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    post.setId(generatedKeys.getInt(1));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод реализует получение всех записей таблицы по запросу
     */
    @Override
    public List<Post> getAll() {
        List<Post> posts = new ArrayList<>();
        try (PreparedStatement ps = cnn.prepareStatement("select * from post")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    posts.add(getPostFromResultSet(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return posts;
    }

    /**
     * Метод реализует поиск записи по id
     */
    @Override
    public Post findById(int id) {
        try (PreparedStatement ps = cnn.prepareStatement("select * from post where id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return getPostFromResultSet(rs);
                }
            }
        } catch (SQLException se) {
            se.printStackTrace();
        }
        return null;
    }

    @Override
    public void close() throws Exception {
        if (cnn != null) {
            cnn.close();
        }
    }

    private Post getPostFromResultSet(ResultSet resultSet) throws SQLException {
        return new Post(
            resultSet.getInt("id"),
            resultSet.getString("name"),
            resultSet.getString("text"),
            resultSet.getString("link"),
            resultSet.getTimestamp("created").toLocalDateTime()
        );
    }

    public static void main(String[] args) throws Exception {
        PsqlStore psqlStore;
        try (InputStream in = PsqlStore.class.getClassLoader().getResourceAsStream("app.properties")) {
            Properties config = new Properties();
            config.load(in);
            psqlStore = new PsqlStore(config);
        }
        HabrCareerParse habrCareerParse = new HabrCareerParse(new HabrCareerDateTimeParser());
        Document document = Jsoup.connect("http://career.habr.com/vacancies/java_developer?page=1").get();
        Element element = document.select(".vacancy-card__inner").first();
        Post post = habrCareerParse.parsePost(element);
        psqlStore.save(post);
        System.out.println("Элемент модели данных, найденный в БД по id: \n" + psqlStore.findById(1));
        System.out.println("Все элементы модели данных из БД: \n" + psqlStore.getAll());
    }
}
