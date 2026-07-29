package com.javarush.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javarush.HibernateUtil;
import com.javarush.dao.CityDAO;
import com.javarush.dao.CountryDAO;
import com.javarush.dao.RedisDAO;
import com.javarush.domain.City;
import com.javarush.redis.CityCountry;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;

public class AppService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(AppService.class);

    private final SessionFactory sessionFactory;
    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, String> redisConnection;

    private final CityDAO cityDAO;
    private final CountryDAO countryDAO;
    private final RedisDAO redisDAO;

    private final DataTransformer dataTransformer;
    private final PerformanceBenchmark benchmark;


    public AppService() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
        this.redisClient = RedisClient.create(RedisURI.create("localhost", 6379));
        this.redisConnection = redisClient.connect();

        checkRedisConnection();

        this.cityDAO = new CityDAO(sessionFactory);
        this.countryDAO = new CountryDAO(sessionFactory);

        ObjectMapper objectMapper = new ObjectMapper();
        this.redisDAO = new RedisDAO(redisConnection, objectMapper);

        this.dataTransformer = new DataTransformer();
        this.benchmark = new PerformanceBenchmark(sessionFactory, cityDAO, redisDAO, objectMapper);
    }

    private void checkRedisConnection() {
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            System.out.println("\nConnected to Redis\n");
        }
    }

    public void migrateDataAndRunBenchmark(List<Integer> benchmarkIds, int iterations) {
        List<City> allCities = null;

        try (Session session = sessionFactory.getCurrentSession()) {
            session.beginTransaction();
            allCities = cityDAO.getAllCities(1, 1000);
            session.getTransaction().commit();
        } catch (Exception e) {
            log.error("Ошибка при чтении данных из MySQL:", e);
        }
        List<CityCountry> preparedData = dataTransformer.transformData(allCities);

        redisDAO.pushToRedis(preparedData);
        benchmark.runTests(benchmarkIds, iterations);
    }

    private List<City> fetchAllCities() {
        Session session = sessionFactory.getCurrentSession();
        List<City> allCities = new ArrayList<>();

        try {
            session.beginTransaction();

            int totalCount = cityDAO.getTotalCount();
            int step = 500;

            for (int i = 0; i < totalCount; i += step) {
                allCities.addAll(cityDAO.getAllCities(i, step));
            }

            session.getTransaction().commit();
            return allCities;

        } catch (Exception e) {
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
            throw e;
        }
    }

    @Override
    public void close() {
        try {
            HibernateUtil.shutdown();
        } catch (Exception e) {
            log.error("Ошибка при закрытии фабрики сессий Hibernate", e);
        }

        if (redisConnection != null) {
            try {
                redisConnection.close();
            } catch (Exception e) {
                log.error("Ошибка при закрытии соединения Redis (redisConnection)", e);
            }
        }

        if (redisClient != null) {
            try {
                redisClient.shutdown();
            } catch (Exception e) {
                log.error("Ошибка при остановке клиента Redis (redisClient)", e);
            }
        }
    }

}
