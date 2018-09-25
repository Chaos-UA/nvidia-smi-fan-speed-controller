package chaos.nvidia.settings.fan.nvidia;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Component
public class AttributeHelper {

    @SneakyThrows
    public String formatForUpdate(NvidiaAttributesDTO nvidiaSettings) {
        StringBuilder result = new StringBuilder();
        for (Field field : nvidiaSettings.getClass().getDeclaredFields()) {
            Attribute attribute = field.getDeclaredAnnotation(Attribute.class);

            if (attribute == null) {
                continue;
            }

            String settingAttribute = String.format(attribute.value(), nvidiaSettings.getGpuIndex());
            field.setAccessible(true);
            Integer value = (Integer) field.get(nvidiaSettings);

            if (value == null) {
                continue;
            }

            if (nvidiaSettings.getGpuIndex() == null) {
                throw new NullPointerException(String.format("gpu index %s is null", nvidiaSettings.getGpuIndex()));
            }

            result.append(String.format(" -a %s=%s ", settingAttribute, value));
        }

        return result.toString();
    }

    @SneakyThrows
    public List<String> getAttributesForQuery() {
        List<String> result = new ArrayList<>();
        for (Field field : NvidiaAttributesDTO.class.getDeclaredFields()) {
            Attribute attribute = field.getDeclaredAnnotation(Attribute.class);
            if (attribute != null) {
                result.add(attribute.value());
            }
        }
        return result;
    }

}
