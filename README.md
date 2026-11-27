# Кэширование реляционных данных в Redis (Hibernate + MySQL + Redis)

Java-приложение для миграции и кэширования географических данных из MySQL в хранилище Redis с последующим бенчмарком производительности чтения.

## 🛠 Технологический стек
* **Язык:** Java 17
* **ORM и реляционная БД:** Hibernate ORM 6.6, MySQL 8
* **Мониторинг SQL:** P6Spy
* **NoSQL / Кэширование:** Redis, Lettuce
* **Сериализация:** Jackson
* **Инфраструктура:** Docker (MySQL 8, Redis)
* **Тесты и утилиты:** JUnit 5, Lombok, Logback (SLF4J), Maven

## ✨ Основной функционал
* **Чтение из MySQL (`CityDAO`, `CountryDAO`):** постраничная выгрузка данных через HQL-запросы и Hibernate `SessionFactory`.
* **Миграция в Redis (`DataTransformer`, `RedisDAO`):** объединение сущностей (`City`, `Country`, `CountryLanguage`) в DTO-модель `CityCountry` и сохранение в Redis в формате JSON (`id` -> JSON).
* **Бенчмарк производительности (`PerformanceBenchmark`):** сравнение времени чтения из кэша Redis (с десериализацией) и напрямую из MySQL через Hibernate.

## 🏗 Структура проекта
* `App.java` — точка входа (запуск миграции и бенчмарка в `try-with-resources`).
* `HibernateUtil.java` — инициализация и закрытие `SessionFactory`, фильтрация логов.
* `domain` — JPA-сущности реляционной схемы (`City`, `Country`, `CountryLanguage`, перечисление `Continent`).
* `redis` — денормализованные DTO-модели (`CityCountry`, `Language`) для Redis.
* `dao` — слой доступа к данным (`CityDAO`, `CountryDAO`, `RedisDAO`).
* `service` — бизнес-логика (`AppService`, `DataTransformer`, `PerformanceBenchmark`).
* `resources/` — конфигурационные файлы Hibernate (`hibernate.cfg.xml`) и P6Spy (`spy.properties`).
* `test` — интеграционные тесты производительности (`PerformanceTest`).

## 🚀 Запуск проекта локально

### Предварительные требования
* JDK 17+
* Maven 3.8+
* Docker (для запуска контейнеров MySQL 8 со схемой `world` и Redis)

1. **Клонируйте репозиторий:**
   ```bash
   git clone https://github.com/kizand/hibernate-project.git
   cd hibernate-project
   ```
2. **Запустите MySQL 8 и Redis** (через Docker или локально на портах `3306` и `6379`).
3. **Соберите и запустите приложение:**
   ```bash
   mvn clean compile exec:java -Dexec.mainClass="com.javarush.App"
   ```
   *(Либо запустите метод `main` в классе `App.java` напрямую через IntelliJ IDEA — в консоль будет выведено среднее время выполнения запросов для Redis и MySQL).*
4. **Запуск тестов:**
   ```bash
   mvn test
   ```

## 💡 Ключевые технические решения
* **Борьба с N+1:** использование `join fetch` в HQL-запросах и контроль запросов через P6Spy.
* **Денормализация:** хранение агрегированных данных `CityCountry` в формате JSON для быстрого доступа по ключу (`GET id`).
* **Корректность бенчмарка:** учет разогрева JVM, использование `System.nanoTime()` и очистка кэша первого уровня Hibernate (`session.clear()`).
* **Управление ресурсами:** реализация `AutoCloseable` в `AppService` для гарантированного закрытия соединений с БД и Redis.
