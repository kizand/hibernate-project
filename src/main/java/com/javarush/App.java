package com.javarush;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.javarush.service.AppService;

import java.util.List;

/**
 * Точка входа
 */

public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        try (AppService appService = new AppService()) {
            List<Integer> ids = List.of(3, 30, 300, 5, 50, 500, 7, 70, 700, 900);
            appService.migrateDataAndRunBenchmark(ids, 30);
        } catch (Exception e) {
            log.error("Произошла ошибка в процессе выполнения приложения!", e);
        }
    }
}
