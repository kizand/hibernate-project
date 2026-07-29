package com.javarush;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javarush.dao.CityDAO;
import com.javarush.dao.CountryDAO;
import com.javarush.domain.City;
import com.javarush.domain.Country;
import com.javarush.domain.CountryLanguage;
import com.javarush.redis.CityCountry;
import com.javarush.redis.Language;
import com.javarush.service.PerformanceBenchmark;
import com.javarush.dao.RedisDAO;
import com.javarush.service.DataTransformer;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisStringCommands;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;


import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class PerformanceTest {

    private static SessionFactory sessionFactory;
    private static RedisClient redisClient;
    private static StatefulRedisConnection<String, String> redisConnection;
    private static PerformanceBenchmark benchmark;

    private static final List<Integer> IDS = List.of(3, 30, 300, 5, 50, 500, 7, 70, 700, 900);
    private static final int WARM_UP_ITERATIONS = 5;
    private static final int TEST_ITERATIONS = 30;

    @BeforeAll
    static void setUp() {

        // Подтянет файл из src/test/resources автоматически
        sessionFactory = HibernateUtil.getSessionFactory();

        CityDAO cityDAO = new CityDAO(sessionFactory);
        CountryDAO countryDAO = new CountryDAO(sessionFactory);

        redisClient = RedisClient.create(RedisURI.create("redis://localhost:6379"));
        redisConnection = redisClient.connect();

        ObjectMapper objectMapper = new ObjectMapper();
        RedisDAO redisDAO = new RedisDAO(redisConnection, objectMapper);

        DataTransformer dataTransformer = new DataTransformer();

        // Инициализируем наш готовый сервис бенчмарков
        benchmark = new PerformanceBenchmark(sessionFactory, cityDAO, redisDAO, objectMapper);

        // Наполняем Redis
        List<City> cities;
        try (Session session = sessionFactory.getCurrentSession()) {
            session.beginTransaction();
            cities = cityDAO.getAllCities(0, 1000);
            // Загружаем страны, чтобы они были в сессии для трансформации
            countryDAO.getAllCountries();
            session.getTransaction().commit();
        }

        List<CityCountry> preparedData = dataTransformer.transformData(cities);
        redisDAO.pushToRedis(preparedData);
    }

    @BeforeEach
    void clearHibernateContext() {
        // Гарантируем полную изоляцию: перед КАЖДЫМ тестом очищаем кэш Hibernate
        try (Session session = sessionFactory.getCurrentSession()) {
            session.beginTransaction();
            session.clear();
            session.getTransaction().commit();
        } catch (Exception ignored) {}
    }

    @AfterAll
    static void tearDown() {
        if (redisConnection != null) {
            redisConnection.close();
        }
        if (redisClient != null) {
            redisClient.shutdown();
        }
        HibernateUtil.shutdown();
    }

    @Test
    @DisplayName("Тестирование производительности Redis")
    void testRedisPerformance() {
        // Разогрев строго для Redis
        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
            benchmark.testRedisData(IDS);
        }

        long startRedis = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            benchmark.testRedisData(IDS);
        }
        long stopRedis = System.nanoTime();

        double redisDuration = (stopRedis - startRedis) / 1_000_000.0 / TEST_ITERATIONS;
        System.out.printf("\n[REDIS ТЕСТ] Среднее время за прогон: %.3f ms\n", redisDuration);
        System.out.flush();
        assertTrue(redisDuration > 0);
    }

    @Test
    @DisplayName("Тестирование производительности MySQL")
    void testMySqlPerformance() {
        // Разогрев строго для MySQL
        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
            benchmark.testMysqlData(IDS);
        }

        long startMysql = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            // Очищаем кэш внутри цикла замера, чтобы MySQL не хитрил на повторных итерациях
            clearHibernateContext();
            benchmark.testMysqlData(IDS);
        }
        long stopMysql = System.nanoTime();

        double mysqlDuration = (stopMysql - startMysql) / 1_000_000.0 / TEST_ITERATIONS;
        System.out.printf("\n[MYSQL ТЕСТ] Среднее время за прогон: %.3f ms\n", mysqlDuration);
        System.out.flush();
        assertTrue(mysqlDuration > 0);
    }
}