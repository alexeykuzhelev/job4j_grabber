package ru.job4j.grabber;

import java.io.IOException;
import java.util.Objects;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/*
  @author Alexey Kuzhelev (aleks2kv1977@gmail.com)
 * @version $Id$
 * @since 17.12.2022
 */

/**
 * Класс описывает парсинг HTML страницы, получаемую через запрос на сервер.
 * Запросы на сервер, получение и парсинг HTML делаем с помощью библиотеки jsoup.
 * Нужно получить данные по вакансиям с сайта вакансий career.habr.com:
 * извлечь со страницы название, дату создания и ссылку на вакансию
 */
public class HabrCareerParse {

    private static final String SOURCE_LINK = "https://career.habr.com";

    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer", SOURCE_LINK);

    public static void main(String[] args) throws IOException {
        Connection connection = Jsoup.connect(PAGE_LINK);
        Document document = connection.get();
        Elements rows = document.select(".vacancy-card__inner");
        rows.forEach(row -> {
            Element titleElement = row.select(".vacancy-card__title").first();
            Element linkElement = Objects.requireNonNull(titleElement).child(0);
            String vacancyName = titleElement.text();
            String link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
            System.out.printf("%s %s%n", vacancyName, link);
            Element dateElement = row.select(".vacancy-card__date").first();
            Element dateTimeElement = Objects.requireNonNull(dateElement).child(0);
            String dateTime = dateTimeElement.attr("datetime");
            System.out.println("Дата вакансии: " + dateTime);
        });
    }
}
