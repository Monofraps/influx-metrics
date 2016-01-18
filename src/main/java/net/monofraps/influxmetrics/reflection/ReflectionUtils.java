package net.monofraps.influxmetrics.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author monofraps
 */
public class ReflectionUtils {
    /**
     * Returns all fields including inherited fields of `klass`.
     * @param klass The class whose fields to return.
     * @return List of fields declared by klass and klass' superclasses.
     */
    public static <T> List<Field> getDeepFields(Class<T> klass) {
        final List<Field> fields = new LinkedList<>();

        Class<?> currentClass = klass;
        do {
            fields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
            currentClass = currentClass.getSuperclass();
        } while(currentClass != null);

        return fields;
    }

    /**
     * Returns all public getters including inherited methods of `klass`.
     * A getter is defined as a public method without parameters whose name starts with 'get'.
     * @param klass The class whose getters to return.
     * @return List of public getters declared by klass and klass' superclasses.
     */
    public static <T> List<Method> getPublicGetters(Class<T> klass) {
        final List<Method> getters = new LinkedList<>();

        Class<?> currentClass = klass;
        do {
            getters.addAll(Arrays.stream(currentClass.getDeclaredMethods()).filter(m ->Modifier.isPublic(m.getModifiers())).filter(m -> m.getName().startsWith("get")).filter(m -> m.getParameterCount() == 0).collect(Collectors.toList()));
            currentClass = currentClass.getSuperclass();
        } while(currentClass != null);

        return getters;
    }
}
