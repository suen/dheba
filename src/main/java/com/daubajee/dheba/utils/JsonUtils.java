package com.daubajee.dheba.utils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import com.daubajee.dheba.peer.RemotePeerVerticle;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class JsonUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemotePeerVerticle.class);

    private static final List<Class<?>> KNOWN_TYPES = Arrays.asList(Integer.class, int.class, Boolean.class,
            boolean.class, Long.class, long.class, String.class);

    /**
     * works only for simple POJO with primitive types
     * 
     * @param object
     * @return
     */
    public static JsonObject toJson(Object object) {
        try {
            Method[] methods = object.getClass().getMethods();
            JsonObject json = new JsonObject();
            for (Method method : methods) {
                String name = method.getName();
                Class<?> returnType = method.getReturnType();
                if ((name.startsWith("get") || name.startsWith("is")) && !returnType.equals(Class.class)) {
                    Object getterValue = method.invoke(object);
                    String attrName = extractAttrNameFromGetterSetterName(name);

                    if (KNOWN_TYPES.contains(returnType)) {
                        json.put(attrName, getterValue);
                    } else {
                        LOGGER.warn("Unknown return type in getter " + returnType.getName());
                    }
                }
            }
            return json;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * works only for simple POJO with primitive types
     * 
     * @param json
     * @param objectType
     * @return
     */
    public static <T> T fromJson(JsonObject json, Class<T> objectType) {
        try {
            Method[] methods = objectType.getMethods();

            T object = objectType.newInstance();
            for (Method method : methods) {
                String name = method.getName();
                if (name.startsWith("set")) {
                    Class<?> paramType = method.getParameterTypes()[0];
                    String attrName = extractAttrNameFromGetterSetterName(name);

                    Object valstr = null;
                    if (paramType.equals(String.class)) {
                        valstr = json.getString(attrName);
                    } else if (paramType.equals(Integer.class) || paramType.equals(int.class)) {
                        valstr = json.getInteger(attrName);
                    } else if (paramType.equals(Long.class) || paramType.equals(long.class)) {
                        valstr = json.getLong(attrName);
                    } else if (paramType.equals(Boolean.class) || paramType.equals(boolean.class)) {
                        valstr = json.getBoolean(attrName);
                    } else {
                        LOGGER.warn("Unknown param type in setter " + paramType.getName());
                    }

                    if (valstr != null) {
                        method.invoke(object, valstr);
                    }
                }
            }
            return object;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

    }

    private static String extractAttrNameFromGetterSetterName(String methodname) {
        int prefix = methodname.startsWith("is") ? 2 : 3;
        String attrName = methodname.substring(prefix, methodname.length());
        return makeFirstCharLower(attrName);
    }

    private static String makeFirstCharLower(String str) {
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

}
