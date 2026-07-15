package com.javarush;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

@Slf4j
public class HibernateUtil {

    // Единственный экземпляр фабрики сессий на всё приложение
    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            log.info("Инициализация Hibernate и чтение конфигурации...");

            // configure() автоматически ищет файл hibernate.cfg.xml в папке resources
            return new Configuration().configure().buildSessionFactory();

        } catch (Throwable ex) {
            log.error("Критическая ошибка при создании SessionFactory!", ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    // Метод, через который вы будете получать доступ к базе данных
    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    // Метод для корректного закрытия соединений при выходе из приложения
    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            log.info("Закрытие фабрики сессий Hibernate...");
            sessionFactory.close();
        }
    }
}