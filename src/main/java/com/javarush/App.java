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
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisStringCommands;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JavaRush-University, Проект модуля 4
 */

public class App {

    private final SessionFactory sessionFactory;

    private final CountryDAO countryDAO;
    private final CityDAO cityDAO;

    private final ObjectMapper objectMapper;
    private final RedisClient redisClient;

    public App() {
        this.sessionFactory = new Configuration().configure().buildSessionFactory();
        this.countryDAO = new CountryDAO(sessionFactory);
        this.cityDAO = new CityDAO(sessionFactory);
        this.objectMapper = new ObjectMapper();
        this.redisClient = getRedisClient();
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

    private RedisClient getRedisClient() {
        RedisClient redisClient = RedisClient.create(RedisURI.create("localhost", 6379));
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            System.out.println("\nConnected to Redis\n");
        }
        return redisClient;
    }

    private void pushToRedis(List<CityCountry> data) {
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisStringCommands<String, String> sync = connection.sync();
            for (CityCountry city : data) {
                try {
                    sync.set(String.valueOf(city.getId()), objectMapper.writeValueAsString(city));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private long getCountFromRedis() {
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            return connection.sync().dbsize();
        }
    }

    private void testRedisData(List<Integer> ids) {
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisStringCommands<String, String> sync = connection.sync();
            for (Integer id : ids) {
                String value = sync.get(String.valueOf(id));
                try {
                    objectMapper.readValue(value, CityCountry.class);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void testMysqlData(List<Integer> ids) {
        try (Session session = sessionFactory.getCurrentSession()) {
            session.beginTransaction();

            for (Integer id : ids) {
                City city = cityDAO.getById(id);
                city.getCountry().getLanguages().size();
            }
            session.getTransaction().commit();
        }
    }

    private List<CityCountry> transformData(List<City> cities) {
        return cities.stream().map(city -> {
            CityCountry res = new CityCountry();
            res.setId(city.getId());
            res.setName(city.getName());
            res.setPopulation(city.getPopulation());
            res.setDistrict(city.getDistrict());

            Country country = city.getCountry();
            res.setAlternativeCountryCode(country.getAlternativeCode());
            res.setContinent(country.getContinent());
            res.setCountryCode(country.getCode());
            res.setCountryName(country.getName());
            res.setCountryPopulation(country.getPopulation());
            res.setCountryRegion(country.getRegion());
            res.setCountrySurfaceArea(country.getSurfaceArea());

            Set<CountryLanguage> countryLanguages = country.getLanguages();
            Set<Language> languages = countryLanguages.stream().map(cl -> {
                Language language = new Language();
                language.setLanguage(cl.getLanguage());
                language.setIsOfficial(cl.getIsOfficial());
                language.setPercentage(cl.getPercentage());

                return language;
            }).collect(Collectors.toSet());
            res.setLanguages(languages);

            return res;
        }).collect(Collectors.toList());
    }


    private void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }
    }

    public static void main(String[] args) {
        App app = new App();
        try {
            List<City> allCities = app.fetchAllCities();
            List<CityCountry> preparedData = app.transformData(allCities);
            app.pushToRedis(preparedData);

            System.out.println("\n==================================================");
            System.out.println(" РЕЗУЛЬТАТЫ ВЫПОЛНЕНИЯ ПРИЛОЖЕНИЯ:");
            System.out.println("==================================================");
            System.out.println("1. Успешно загружено городов из MySQL: " + allCities.size());
            System.out.println("2. Успешно преобразовано сущностей: " + preparedData.size());
            System.out.println("3. Статус синхронизации: УСПЕШНО ЗАПИСАНО В REDIS");
            System.out.println("4. Количество ключей в Redis: " + app.getCountFromRedis());
            System.out.println("==================================================\n");

        } catch (Exception e) {
            System.err.println("Произошла ошибка в процессе выполнения!");
            e.printStackTrace();
        } finally {
            app.shutdown();
        }
    }
}
