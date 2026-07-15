package com.javarush;

import com.javarush.dao.CityDAO;
import com.javarush.dao.CountryDAO;
import com.javarush.domain.City;
import com.javarush.domain.Country;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * JavaRush-University, Проект модуля 4
 */

public class App {

    private final SessionFactory sessionFactory;
    private final CountryDAO countryDAO;
    private final CityDAO cityDAO;

    public App() {
        this.sessionFactory = new Configuration().configure().buildSessionFactory();
        this.countryDAO = new CountryDAO(sessionFactory);
        this.cityDAO = new CityDAO(sessionFactory);
    }

    private List<City> fetchAllCities() {
        Session session = sessionFactory.getCurrentSession();
        List<City> allCities = new ArrayList<>();

        try {
            session.beginTransaction();

            List<Country> countries = countryDAO.getAllCountries();

            int totalCount = cityDAO.getTotalCount();
            int step = 500;

            for (int i = 0; i < totalCount; i += step) {
                List<City> citiesBatch = cityDAO.getCities(i, step);
                allCities.addAll(citiesBatch);
                session.clear();
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

    private void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }
    }

    public static void main(String[] args) {
        App app = new App();
        try {
            List<City> allCities = app.fetchAllCities();
            System.out.println("Успешно загружено городов: " + allCities.size());
        } finally {
            app.shutdown();
        }
    }
}
