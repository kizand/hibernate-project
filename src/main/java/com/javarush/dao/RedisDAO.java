package com.javarush.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javarush.redis.CityCountry;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisStringCommands;

import java.util.List;

public class RedisDAO {

    private final StatefulRedisConnection<String, String> connection;
    private final ObjectMapper objectMapper;

    public RedisDAO(StatefulRedisConnection<String, String> connection, ObjectMapper objectMapper) {
        this.connection = connection;
        this.objectMapper = objectMapper;
    }

    public void pushToRedis(List<CityCountry> data) {
        RedisStringCommands<String, String> sync = connection.sync();
        for (CityCountry city : data) {
            try {
                sync.set(String.valueOf(city.getId()), objectMapper.writeValueAsString(city));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }

    public String get(String key) {
        return connection.sync().get(key);
    }
}
