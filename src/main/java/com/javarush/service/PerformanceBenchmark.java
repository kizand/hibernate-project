package com.javarush.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javarush.dao.CityDAO;
import com.javarush.dao.RedisDAO;
import com.javarush.domain.City;
import com.javarush.redis.CityCountry;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class PerformanceBenchmark {

    private final SessionFactory sessionFactory;
    private final CityDAO cityDAO;
    private final RedisDAO redisDAO;
    private final ObjectMapper objectMapper;

    public PerformanceBenchmark(SessionFactory sessionFactory, CityDAO cityDAO, RedisDAO redisDAO, ObjectMapper objectMapper) {
        this.sessionFactory = sessionFactory;
        this.cityDAO = cityDAO;
        this.redisDAO = redisDAO;
        this.objectMapper = objectMapper;
    }

    public void runTests(List<Integer> ids, int iterations) {
        // 1. РАЗОГРЕВ JVM
        for (int i = 0; i < 5; i++) {
            testRedisData(ids);
            testMysqlData(ids);
        }

        // 2. ТЕСТИРОВАНИЕ REDIS
        long startRedis = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            testRedisData(ids);
        }
        long stopRedis = System.nanoTime();

        // 3. ТЕСТИРОВАНИЕ MYSQL
        long startMysql = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            try (Session session = sessionFactory.getCurrentSession()) {
                session.beginTransaction();
                session.clear();
                session.getTransaction().commit();
            }
            testMysqlData(ids);
        }
        long stopMysql = System.nanoTime();

        // 4. ВЫВОД РЕЗУЛЬТАТОВ
        double redisDuration = (stopRedis - startRedis) / 1_000_000.0 / iterations;
        double mysqlDuration = (stopMysql - startMysql) / 1_000_000.0 / iterations;

        System.out.printf("Среднее время за прогон (%d итераций):\n", iterations);
        System.out.printf("%s:\t%.3f ms\n", "Redis", redisDuration);
        System.out.printf("%s:\t%.3f ms\n", "MySQL", mysqlDuration);
    }

    public void testRedisData(List<Integer> ids) {
        for (Integer id : ids) {
            String value = redisDAO.get(String.valueOf(id));
            try {
                objectMapper.readValue(value, CityCountry.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }

    public void testMysqlData(List<Integer> ids) {
        try (Session session = sessionFactory.getCurrentSession()) {
            session.beginTransaction();
            for (Integer id : ids) {
                City city = cityDAO.getById(id);
                if (city != null) {
                    city.getCountry().getLanguages().size();
                }
            }
            session.getTransaction().commit();
        }
    }
}