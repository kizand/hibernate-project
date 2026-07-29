package com.javarush;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

@Slf4j
public class HibernateUtil {

    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            ch.qos.logback.classic.LoggerContext loggerContext =
                    (ch.qos.logback.classic.LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
            java.util.List.of("org.hibernate", "io.lettuce", "io.netty")
                    .forEach(loggerName -> loggerContext.getLogger(loggerName).setLevel(ch.qos.logback.classic.Level.WARN));

            log.info("Инициализация Hibernate и чтение конфигурации...");

            return new Configuration().configure().buildSessionFactory();

        } catch (Throwable ex) {
            log.error("Критическая ошибка при создании SessionFactory!", ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            log.info("Закрытие фабрики сессий Hibernate...");
            sessionFactory.close();
        }
    }
}