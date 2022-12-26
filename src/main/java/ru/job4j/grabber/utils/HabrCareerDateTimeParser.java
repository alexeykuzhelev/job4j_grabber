package ru.job4j.grabber.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/*
  @author Alexey Kuzhelev (aleks2kv1977@gmail.com)
 * @version $Id$
 * @since 26.12.2022
 */

/**
 * Класс описывает преобразование строки в дату с указанием формата даты.
 */
public class HabrCareerDateTimeParser implements DateTimeParser {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssz");

    @Override
    public LocalDateTime parse(String parse) {
        return LocalDateTime.parse(parse, DATE_TIME_FORMATTER);
    }
}
