package ru.job4j.grabber;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.*;
import java.util.Properties;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/*
  @author Alexey Kuzhelev (aleks2kv1977@gmail.com)
 * @version $Id$
 * @since 08.04.2023
 */

/**
 * Класс соединяет отдельные части (Post, Store, Parse, Scheduller) в целое приложение.
 * Post - модель данных (содержит данные поста сайта career.habr.com)
 * Store - реализует хранение данных модели в базе данных
 * Parse - реализует извлечения данных с сайта career.habr.com
 * Scheduler - планировщик, управляющий всеми работами
 */
public class Grabber implements Grab {
    private static final String SOURCE_LINK = "https://career.habr.com";
    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer", SOURCE_LINK);
    private final Parse parse;
    private final Store store;
    private final Scheduler scheduler;
    private final int time;

    public Grabber(Parse parse, Store store, Scheduler scheduler, int time) {
        this.parse = parse;
        this.store = store;
        this.scheduler = scheduler;
        this.time = time;
    }

    /**
     * Метод реализует работу планировщика и запускает задачу на выполнение
     */
    @Override
    public void init() throws SchedulerException {
        JobDataMap data = new JobDataMap();
        data.put("store", store);
        data.put("parse", parse);
        JobDetail job = newJob(GrabJob.class)
            .usingJobData(data)
            .build();
        SimpleScheduleBuilder times = simpleSchedule()
            .withIntervalInSeconds(time)
            .repeatForever();
        Trigger trigger = newTrigger()
            .startNow()
            .withSchedule(times)
            .build();
        scheduler.scheduleJob(job, trigger);
        try {
            Thread.sleep(120000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        scheduler.shutdown();
    }

    /**
     * Класс описывает действия для загрузки всех постов.
     * Загруженные посты сохраняются в базе данных PostgreSQL.
     * Далее через методы базе данных посты выводятся на печать в консоль.
     */
    public static class GrabJob implements Job {
        /**
         * Метод реализует задачу, которую надо выполнять периодически (добавление данных в БД)
         * Задача загружает и выводит на печать посты.
         */
        @Override
        public void execute(JobExecutionContext context) {
            JobDataMap map = context.getJobDetail().getJobDataMap();
            Store store = (Store) map.get("store");
            Parse parse = (Parse) map.get("parse");
            try {
                parse.list(String.format("%s%s", PAGE_LINK, "?page="))
                    .forEach(store::save);
                System.out.println("\nВсе элементы модели данных из БД: \n");
                store.getAll().forEach(System.out::println);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) throws Exception {
        var cfg = new Properties();
        try (InputStream in = Grabber.class.getClassLoader()
            .getResourceAsStream("app.properties")) {
            cfg.load(in);
        }
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();
        var parse = new HabrCareerParse(new HabrCareerDateTimeParser());
        var store = new PsqlStore(cfg);
        var time = Integer.parseInt(cfg.getProperty("time"));
        new Grabber(parse, store, scheduler, time).init();
    }
}
