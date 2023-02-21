package ru.job4j.grabber;

import java.util.List;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/*
  @author Alexey Kuzhelev (aleks2kv1977@gmail.com)
 * @version $Id$
 * @since 22.02.2023
 */

/**
 * Класс соединяет отдельные части (Post, Store, Parse, Scheduller) в целое приложение.
 * Post - модель данных (содержит данные поста сайта career.habr.com)
 * Store - реализует хранение данных модели в памяти (списке)
 * Parse - реализует извлечения данных с сайта career.habr.com
 * Scheduler - планировщик, управляющий всеми работами
 */
public class MemGrab implements Grab {

    private static final String SOURCE_LINK = "https://career.habr.com";

    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer", SOURCE_LINK);

    private final Parse parse;

    private final Store store;

    private final Scheduler scheduler;

    public MemGrab(Parse parse, Store store, Scheduler scheduler) {
        this.parse = parse;
        this.store = store;
        this.scheduler = scheduler;
    }

    /**
     * Метод реализует работу планировщика и запускает задачу на выполнение
     */
    @Override
    public void init() {
        try {
            scheduler.start();
            JobDataMap data = new JobDataMap();
            data.put("parse", parse);
            data.put("store", store);
            JobDetail job = newJob(HabrCareer.class)
                .usingJobData(data)
                .build();
            SimpleScheduleBuilder times = simpleSchedule()
                .withIntervalInMinutes(3)
                .repeatForever();
            Trigger trigger = newTrigger()
                .startNow()
                .withSchedule(times)
                .build();
            scheduler.scheduleJob(job, trigger);
            Thread.sleep(360000);
            scheduler.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Класс описывает действия для загрузки всех постов.
     * Загруженные посты сохраняются в модели в памяти.
     * Далее через методы модели памяти посты выводятся на печать в консоль.
     */
    public static class HabrCareer implements Job {
        /**
         * Метод реализует задачу, которую надо выполнять периодически (добавление данных в БД)
         * Задача загружает и выводит на печать посты.
         */
        @Override
        public void execute(JobExecutionContext context) {
            HabrCareerParse habrCareerParse = (HabrCareerParse) context
                .getJobDetail()
                .getJobDataMap()
                .get("parse");
            MemStore memStore = (MemStore) context
                .getJobDetail()
                .getJobDataMap()
                .get("store");
            List<Post> posts = habrCareerParse.list(String.format("%s%s", PAGE_LINK, "?page="));
            posts.forEach(memStore::save);
            memStore.getAll().forEach(System.out::println);
            System.out.println("Элемент модели данных, найденный в списке по id: \n" + memStore.findById(5));
        }
    }

    public static void main(String[] args) throws Exception {
        Parse habrCareerParse = new HabrCareerParse(new HabrCareerDateTimeParser());
        Store memStore = new MemStore();
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        new MemGrab(habrCareerParse, memStore, scheduler).init();
    }
}
