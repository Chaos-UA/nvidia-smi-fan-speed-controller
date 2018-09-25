package chaos.nvidia.settings.fan;

import lombok.SneakyThrows;

import java.lang.reflect.Field;

public class ReflectionUtil {

    @SneakyThrows
    public static <T> T getValue(Object object, String fieldName) {
        Field field = getField(object, fieldName);
        T result = (T) field.get(object);
        return result;
    }

    @SneakyThrows
    public static void setValue(Object object, String fieldName, Object value) {
        Field field = getField(object, fieldName);
        field.set(object, value);
    }

    @SneakyThrows
    public static Field getField(Object object, String fieldName) {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }
}
