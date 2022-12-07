package ru.job4j.quartz;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/*
  @author Alexey Kuzhelev (aleks2kv1977@gmail.com)
 * @version $Id$
 * @since 08.12.2022
 */

/**
 * Класс описывает планировщик для добавления данных в БД с периодичностью 10 секунд.
 * Планировщик реализован с помощью библиотеки Quartz.
 */
public class AlertRabbit {
    /**
     * Метод реализует работу планировщика
     * properties - получение обекта класса Properties с загрузкой в него данных из файла с настройками
     * connection - создание соединения с БД
     * scheduler - создание класса, управляющего всеми работами
     * data - создание объекта со ссылкой на connect к базе и передачей его в Job
     * job - создание задачи с передачей в нее класса, в котором описаны требуемые действия и параметра data
     * times - создание расписания, запускать задачу через 10 секунд в бесконечном цикле
     * trigger - создание триггера - когда начинать запуск (сразу), с какой периодичностью
     * загрузка задачи и триггера в планировщик
     * метод main работает 10 секунд и затем планировщик завершает работу через scheduler.shutdown()
     */
    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        Properties properties = getProperties();
        try (Connection connection = init(properties)) {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            JobDataMap data = new JobDataMap();
            data.put("connection", connection);
            JobDetail job = newJob(Rabbit.class).usingJobData(data).build();
            SimpleScheduleBuilder times = simpleSchedule()
                .withIntervalInSeconds(Integer.parseInt(properties.getProperty("rabbit.interval")))
                .repeatForever();
            Trigger trigger = newTrigger()
                .startNow()
                .withSchedule(times)
                .build();
            scheduler.scheduleJob(job, trigger);
            Thread.sleep(10000);
            scheduler.shutdown();
            System.out.println("scheduler completed");
        } catch (SchedulerException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод реализует соединение с БД
     */
    public static Connection init(Properties properties) throws ClassNotFoundException, SQLException {
        Class.forName(properties.getProperty("driver-class-name"));
        return DriverManager.getConnection(
            properties.getProperty("url"),
            properties.getProperty("username"),
            properties.getProperty("password")
        );
    }

    /**
     * Метод реализует чтение файла с настройками
     */
    private static Properties getProperties() throws IOException {
        Properties properties;
        try (InputStream in = AlertRabbit.class.getClassLoader().getResourceAsStream("rabbit.properties")) {
            properties = new Properties();
            properties.load(in);
        }
        return properties;
    }

    /**
     * Класс описывает действия для получения соединения из context и добавления данных в БД
     */
    public static class Rabbit implements Job {

        public Rabbit() {
            System.out.println(hashCode());
        }

        /**
         * Метод реализует задачу, которую надо выполнять периодически (добавление данных в БД)
         * statement - соединяемся с БД и делаем запрос
         */
        @Override
        public void execute(JobExecutionContext context) {
            System.out.println("Rabbit runs here ...");
            Connection connection = (Connection) context.getJobDetail().getJobDataMap().get("connection");
            try (PreparedStatement statement = connection.prepareStatement(
                "insert into rabbit(created_date) values (?)"
            )) {
                statement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                statement.execute();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
}
