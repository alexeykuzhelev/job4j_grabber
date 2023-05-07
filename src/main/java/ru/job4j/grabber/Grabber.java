package ru.job4j.grabber;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Properties;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/*
  @author Alexey Kuzhelev (aleks2kv1977@gmail.com)
 * @version $Id$
 * @since 08.05.2023
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
    private final Properties cfg = new Properties();

    @Override
    public void init() {
    }

    /**
     * Метод реализует работу планировщика и запускает задачу на выполнение
     */
    @Override
    public void init(Parse parse, Store store, Scheduler scheduler) throws SchedulerException {
        JobDataMap data = new JobDataMap();
        data.put("store", store);
        data.put("parse", parse);
        JobDetail job = newJob(GrabJob.class)
            .usingJobData(data)
            .build();
        SimpleScheduleBuilder times = simpleSchedule()
            .withIntervalInSeconds(Integer.parseInt(cfg.getProperty("time")))
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

    /**
     * Метод реализует получение данных через браузер от нашего граббера.
     * Класс ServerSocket создает сервер.
     * Ответ от сервера будет в виде списка вакансий.
     */
    public void web(Store store) {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(Integer.parseInt(cfg.getProperty("port")))) {
                while (!server.isClosed()) {
                    Socket socket = server.accept();
                    try (OutputStream out = socket.getOutputStream()) {
                        out.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
                        for (Post post : store.getAll()) {
                            out.write(post.toString().getBytes(Charset.forName("Windows-1251")));
                            out.write(System.lineSeparator().getBytes());
                        }
                    } catch (IOException io) {
                        io.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void cfg() throws IOException {
        try (InputStream in = Grabber.class.getClassLoader()
            .getResourceAsStream("app.properties")) {
            cfg.load(in);
        }
    }

    public Scheduler scheduler() throws SchedulerException {
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();
        return scheduler;
    }

    public Store store() throws SQLException {
        return new PsqlStore(cfg);
    }

    public static void main(String[] args) throws Exception {
        Grabber grab = new Grabber();
        grab.cfg();
        Scheduler scheduler = grab.scheduler();
        Store store = grab.store();
        grab.init(new HabrCareerParse(new HabrCareerDateTimeParser()), store, scheduler);
        grab.web(store);
    }
}
