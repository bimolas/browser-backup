package com.example.nexus.core;


import com.example.nexus.util.DatabaseManager;

import java.util.HashMap;
import java.util.Map;

public class DIContainer {
    private final Map<Class<?>, Object> services = new HashMap<>();

    public <T> void register(Class<T> serviceClass, T implementation) {
        services.put(serviceClass, implementation);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> serviceClass) {
        return (T) services.get(serviceClass);
    }

    public <T> T getOrCreate(Class<T> serviceClass) {
        T service = get(serviceClass);
        if (service == null) {
            try {
                // Try constructor with DIContainer parameter first
                try {
                    var constructor = serviceClass.getDeclaredConstructor(DIContainer.class);
                    service = constructor.newInstance(this);
                } catch (NoSuchMethodException e1) {
                    // Try constructor with DatabaseManager parameter
                    try {
                        var constructor = serviceClass.getDeclaredConstructor(DatabaseManager.class);
                        DatabaseManager dbManager = get(DatabaseManager.class);
                        if (dbManager == null) {
                            throw new RuntimeException("DatabaseManager not registered in container");
                        }
                        service = constructor.newInstance(dbManager);
                    } catch (NoSuchMethodException e2) {
                        // Fall back to no-arg constructor
                        service = serviceClass.getDeclaredConstructor().newInstance();
                    }
                }
                register(serviceClass, service);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create instance of " + serviceClass.getName(), e);
            }
        }
        return service;
    }
}