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
            boolean isFanAttribute = "GPUTargetFanSpeed".equals(attribute.value());
            int index = isFanAttribute ? nvidiaSettings.getFanIndex() : nvidiaSettings.getGpuIndex();
            String settingAttribute = String.format(attribute.value(), index);
            field.setAccessible(true);
            Integer value = (Integer) field.get(nvidiaSettings);

            if (value == null) {
                continue;
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
