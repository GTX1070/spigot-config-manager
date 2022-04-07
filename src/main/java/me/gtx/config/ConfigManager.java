package me.gtx.config;

import me.gtx.config.annotation.Comment;
import me.gtx.config.annotation.Config;
import me.gtx.config.annotation.Tree;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public enum ConfigManager {

    INSTANCE;

    public void load(Object instance, FileConfiguration yamlProvider, File save) {
        Map<String, String> comments = new HashMap<>();
        try {
            Class<?> clazz = instance.getClass();
            for(Field field : clazz.getDeclaredFields()) {
                if(field.isAnnotationPresent(Config.class)) {
                    field.setAccessible(true);
                    Class<?> fieldType = field.getType();
                    Config configAnnotation = field.getAnnotation(Config.class);
                    String key = configAnnotation.key();

                    if(field.isAnnotationPresent(Comment.class)) {
                        Comment comment = field.getAnnotation(Comment.class);
                        comments.put(key, comment.comment());
                    }

                    if(field.isAnnotationPresent(Tree.class)) {
                        Object innerInstance = field.get(instance);
                        for(Field f : fieldType.getDeclaredFields()) {
                            if(f.isAnnotationPresent(Config.class)) {
                                Config inner = f.getAnnotation(Config.class);
                                f.setAccessible(true);

                                if(f.isAnnotationPresent(Comment.class)) {
                                    Comment comment = f.getAnnotation(Comment.class);
                                    comments.put(inner.key(), comment.comment());
                                }

                                this.handleField(f, innerInstance, yamlProvider, key + "." + inner.key());
                            }
                        }
                    } else {
                        this.handleField(field, instance, yamlProvider, key);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        try {
            yamlProvider.save(save);

            handleComments(save, comments);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void handleField(Field field, Object instance, FileConfiguration yamlProvider, String key) {
        try {
            Class<?> fieldType = field.getType();
            if(fieldType == List.class) {
                Class<?> listType = Object.class;
                try {
                    ParameterizedType genericType = (ParameterizedType) field.getGenericType();
                    if(genericType.getActualTypeArguments().length > 0) {
                        try {
                            listType = Class.forName(genericType.getActualTypeArguments()[0].getTypeName());
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (ClassCastException e) {
                    // We can ignore this
                }

                if(listType.getSuperclass() != Enum.class) {
                    if(!yamlProvider.contains(key)) {
                        List<?> list = (List<?>) field.get(instance);
                        if(list == null)
                            list = Collections.emptyList();
                        yamlProvider.set(key, list);
                    } else {
                        List<?> list = (List<?>) yamlProvider.get(key);
                        if(list == null) list = Collections.emptyList();
                        if(listType == Object.class) {
                            field.set(instance, list);
                        } else {
                            Class<?> type = listType;
                            field.set(instance, list.stream().filter(o -> o.getClass().isAssignableFrom(type)).collect(Collectors.toList()));
                        }
                    }
                } else {
                    if(!yamlProvider.contains(key)) {
                        List<?> list = (List<?>) field.get(instance);
                        if(list == null) list = Collections.emptyList();
                        yamlProvider.set(key, list.stream().map(e -> ((Enum<?>) e).name()).collect(Collectors.toList()));
                    } else {
                        List<?> list = (List<?>) yamlProvider.get(key);
                        if(list == null) list = Collections.emptyList();
                        Class<?> type = listType;
                        field.set(instance, list.stream().map(o -> Enum.valueOf((Class<? extends Enum>) type, (String) o)).collect(Collectors.toList()));
                    }
                }
            } else if(fieldType.getSuperclass() == Enum.class) {
                if(!yamlProvider.contains(key)) {
                    Enum<?> value = (Enum<?>) field.get(instance);
                    yamlProvider.set(key, value.name());
                } else {
                    Object value = yamlProvider.get(key);
                    if(value instanceof String) {
                        try {
                            field.set(instance, Enum.valueOf((Class<? extends Enum>) fieldType, (String) value));
                        } catch (IllegalArgumentException e) {
                            // We can also ignore this one
                        }
                    }
                }
            } else {
                if(!yamlProvider.contains(key))
                    yamlProvider.set(key, field.get(instance));
                else
                    field.set(instance, yamlProvider.get(key));
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void handleComments(File file, Map<String, String> comments) {
        try {
            Path path = Paths.get(file.toURI());
            List<String> lines = Files.readAllLines(path);
            List<String> newLines = new ArrayList<>();
            for(String line : lines) {
                if(line.contains("#"))
                    continue;
                for(String key : comments.keySet()) {
                    String[] split = key.split("\\.");
                    if(split.length > 0) {
                        String last = split[split.length - 1];
                        if(line.contains(last + ":")) {
                            newLines.add("# " + comments.get(key));
                        }
                    }
                }
                newLines.add(line);
            }
            Files.write(path, newLines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
