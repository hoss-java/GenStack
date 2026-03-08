package com.GenStack.kvhandler;

import org.bson.Document;

import org.json.JSONObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Field;

public class SerializationUtil {
    
    // Generic serialize method
    public static <T> String serialize(T object) {
        JSONObject jsonObject = new JSONObject();
        for (Field field : object.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                jsonObject.put(field.getName(), field.get(object));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return jsonObject.toString();
    }

    // Generic deserialize method
    public static <T> T deserialize(String jsonString, Class<T> clazz) {
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            JSONObject jsonObject = new JSONObject(jsonString);
            for (String key : jsonObject.keySet()) {
                Field field = clazz.getDeclaredField(key);
                field.setAccessible(true);
                field.set(instance, jsonObject.get(key));
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Generic serialize method for Map
    public static <T> String serializeMap(Map<String, T> map) {
        JSONObject jsonObject = new JSONObject();
        for (Map.Entry<String, T> entry : map.entrySet()) {
            T value = entry.getValue();
            JSONObject valueObj = new JSONObject();
            for (Field field : value.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    valueObj.put(field.getName(), field.get(value));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            jsonObject.put(entry.getKey(), valueObj);
        }
        return jsonObject.toString();
    }

    // Generic deserialize method for Map
    public static <T> Map<String, T> deserializeMap(String jsonString, Class<T> clazz) {
        Map<String, T> map = new HashMap<>();
        JSONObject jsonObject = new JSONObject(jsonString);
        for (String key : jsonObject.keySet()) {
            JSONObject valueObj = jsonObject.getJSONObject(key);
            try {
                T instance = clazz.getDeclaredConstructor().newInstance();
                for (String fieldName : valueObj.keySet()) {
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(instance, valueObj.get(fieldName));
                }
                map.put(key, instance);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return map;
    }
}
