package com.example.nexus.core;

import com.example.nexus.controller.DownloadController;
import com.example.nexus.service.DownloadService;
import com.example.nexus.service.SettingsService;
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

                try {
                    var constructor = serviceClass.getDeclaredConstructor(DIContainer.class);
                    service = constructor.newInstance(this);
                } catch (NoSuchMethodException e1) {

                    try {
                        var constructor = serviceClass.getDeclaredConstructor(DatabaseManager.class);
                        DatabaseManager dbManager = get(DatabaseManager.class);
                        if (dbManager == null) {
                            throw new RuntimeException("DatabaseManager not registered in container");
                        }
                        service = constructor.newInstance(dbManager);
                    } catch (NoSuchMethodException e2) {

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

    public void registerDefaultControllers() {

        if (get(DownloadController.class) == null) {
            DownloadService downloadService = getOrCreate(DownloadService.class);
            SettingsService settingsService = getOrCreate(SettingsService.class);
            DownloadController downloadController = new DownloadController(downloadService, settingsService);
            register(DownloadController.class, downloadController);

        }
    }
}
