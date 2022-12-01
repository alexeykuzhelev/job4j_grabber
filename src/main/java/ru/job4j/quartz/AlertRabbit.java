package ru.job4j.quartz;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/*
  @author Alexey Kuzhelev (aleks2kv1977@gmail.com)
 * @version $Id$
 * @since 01.12.2022
 */

/**
 * Класс описывает планировщик для вывода в консоль сообщений с периодичностью 10 секунд.
 * Планировщик реализован с помощью библиотеки Quartz.
 */
public class AlertRabbit {
    /**
     * Метод реализует работу планировщика
     * scheduler - создание класса, управляющего всеми работами
     * job - ссоздание задачи с передачей в нее класса, в котором описаны требуемые действия
     * times - создание расписания, запускать задачу через 10 секунд в бесконечном цикле
     * trigger - создание триггера - когда начинать запуск (сразу), с какой периодичностью
     * загрузка задачи и триггера в планировщик
     */
    public static void main(String[] args) {
        try {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            JobDetail job = newJob(Rabbit.class).build();
            SimpleScheduleBuilder times = simpleSchedule()
                .withIntervalInSeconds(getIntervalFromProperties())
                .repeatForever();
            Trigger trigger = newTrigger()
                .startNow()
                .withSchedule(times)
                .build();
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод реализует чтение файла с настройками
     * @return возвращает значение интервала времени для запуска задачи
     */
    private static int getIntervalFromProperties() throws IOException {
        Properties properties;
        try (InputStream in = AlertRabbit.class.getClassLoader().getResourceAsStream("rabbit.properties")) {
            properties = new Properties();
            properties.load(in);
        }
        return Integer.parseInt(properties.getProperty("rabbit.interval"));
    }

    /**
     * Класс описывает действия для вывода на консоль текста
     */
    public static class Rabbit implements Job {
        /**
         * Метод реализует задачу, которую надо выполнять периодически (вывод на консоль текста)
         */
        @Override
        public void execute(JobExecutionContext context) {
            System.out.println("Rabbit runs here ...");
        }
    }
}
