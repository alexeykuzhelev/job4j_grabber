package ru.job4j.grabber;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.DateTimeParser;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

/*
  @author Alexey Kuzhelev (aleks2kv1977@gmail.com)
 * @version $Id$
 * @since 07.02.2023
 */

/**
 * Класс описывает парсинг HTML страницы, получаемую через запрос на сервер.
 * Запросы на сервер, получение и парсинг HTML делаем с помощью библиотеки jsoup.
 * Нужно получить данные по вакансиям с сайта вакансий career.habr.com:
 * извлечь со страницы название, дату создания и ссылку на вакансию
 */
public class HabrCareerParse implements Parse {

    private static final String SOURCE_LINK = "https://career.habr.com";

    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer", SOURCE_LINK);

    private final DateTimeParser dateTimeParser;

    public static final int PAGES = 5;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

    public static void main(String[] args) {
        HabrCareerParse habrCareerParse = new HabrCareerParse(new HabrCareerDateTimeParser());
        List<Post> posts = habrCareerParse.list(String.format("%s%s", PAGE_LINK, "?page="));
        posts.forEach(System.out::println);
    }

    /**
     * Метод реализует парсинг детального описания вакансии.
     * Нужно программно переходить по ссылкам на вакансии и извлекать данные описания.
     */
    private static String retrieveDescription(String link) {
        Connection connection = Jsoup.connect(link);
        String text = null;
        try {
            Document document = connection.get();
            Element descriptionElement = document.select(".style-ugc").first();
            text = Objects.requireNonNull(descriptionElement).text();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text;
    }

    private Element retrieveTitle(Element element) {
        return Objects.requireNonNull(element.select(".vacancy-card__title").first());
    }

    private String retrieveLink(Element element) {
        return retrieveTitle(element).child(0).attr("href");
    }

    private LocalDateTime retrieveDate(Element element) {
        Element dateElement = Objects.requireNonNull(element.select(".vacancy-card__date").first()).child(0);
        return dateTimeParser.parse(dateElement.attr("datetime"));
    }

    /**
     * Метод делает запрос на сервер, получает HTML страницу и создает из нее объект Document.
     * @return возвращает объект Document с данными HTML страницы
     */
    private Document getDocument(String pageLink) {
        Document document = null;
        try {
            Connection connection = Jsoup.connect(pageLink);
            document = connection.get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return document;
    }

    /**
     * Метод парсит все детали одного поста.
     * @return возвращает объект с данными поста
     */
    public Post parsePost(Element element) {
        String link = retrieveTitle(element).child(0).attr("href");
        return new Post(
            retrieveTitle(element).text(),
            retrieveLink(element),
            retrieveDescription(String.format("%s%s", SOURCE_LINK, link)),
            retrieveDate(element)
        );
    }

    /**
     * Метод загружает список всех постов.
     * В нем нужно спарсить 5 страниц.
     * @param link - ссылка, откуда берем все посты
     * @return возвращает список из всех постов в документе
     */
    @Override
    public List<Post> list(String link) {
        List<Post> posts = new ArrayList<>();
        for (int i = 1; i <= PAGES; i++) {
            Document document = getDocument(link);
            Elements rows = document.select(".vacancy-card__inner");
            rows.forEach(row -> posts.add(parsePost(row)));
        }
        return posts;
    }
}
