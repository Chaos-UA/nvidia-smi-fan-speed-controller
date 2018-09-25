package chaos.nvidia.settings.fan.nvidia;

import chaos.nvidia.settings.fan.FanControllerConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Getter
@Setter
public class NvidiaSettingsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NvidiaSettingsService.class);

    private static final Pattern READ_ATTRIBUTE_PATTERN = Pattern.compile("Attribute '(\\w+)' .+\\[\\w+:(\\d+)\\]\\): (\\d+).");

    @Autowired
    private FanControllerConfig fanControllerConfig;

    @Autowired
    private AttributeHelper attributeHelper;

    @SneakyThrows
    public void updateNvidiaSettings(List<NvidiaAttributesDTO> nvidiaAttributes) {
        String cmd = "nvidia-settings ";

        for (NvidiaAttributesDTO attributes : nvidiaAttributes) {
            cmd += attributeHelper.formatForUpdate(attributes);
        }

        if (fanControllerConfig.isEmulate()) {
            LOGGER.info("Emulating the following cmd: " + cmd);
        } else {
            Process process = Runtime.getRuntime().exec(cmd);
            LOGGER.info("Executed cmd: " + cmd);
        }
    }

    @SneakyThrows
    public List<NvidiaAttributesDTO> getSettings() {
        String cmd = "nvidia-settings ";

        for (String attribute : attributeHelper.getAttributesForQuery()) {
            String allGpusAttribute = attribute.replaceAll(":%s", "");
            cmd += String.format(" -q=%s ", allGpusAttribute);
        }

        Process process = Runtime.getRuntime().exec(cmd);

        String output = IOUtils.toString(process.getInputStream(), "UTF-8");

        List<NvidiaAttributesDTO> result = parseNvidiaSettingsOutput(output);
        return result;
    }

    private List<NvidiaAttributesDTO> parseNvidiaSettingsOutput(String output) {

        Map<Integer, NvidiaAttributesDTO> resultMap = new LinkedHashMap<>();

        String[] outputLines = output.split("\n");

        for (String line : outputLines) {
            Matcher matcher = READ_ATTRIBUTE_PATTERN.matcher(line);
            if (matcher.find()) {
                String attributeName = matcher.group(1);
                int gpuIndex = Integer.valueOf(matcher.group(2));
                int value = Integer.valueOf(matcher.group(3));

                NvidiaAttributesDTO attributes = resultMap.computeIfAbsent(gpuIndex, (key) -> {
                    NvidiaAttributesDTO nvidiaAttributes = new NvidiaAttributesDTO();
                    nvidiaAttributes.setGpuIndex(gpuIndex);
                    return nvidiaAttributes;
                });

                setAttributeValue(attributeName, value, attributes);
            }
        }

        List<NvidiaAttributesDTO> result =  new ArrayList<>(resultMap.values());

        validateThatAllFieldsSet(result);

        return result;
    }

    @SneakyThrows
    private void setAttributeValue(String attributeName, Object value, NvidiaAttributesDTO attributes) {
        for (Field field : attributes.getClass().getDeclaredFields()) {
            Attribute attribute = field.getDeclaredAnnotation(Attribute.class);
            if (attribute != null && attribute.value().contains(attributeName)) {
                field.setAccessible(true);
                field.set(attributes, value);
                return;
            }
        }
        throw new RuntimeException(String.format("Failed to find the attribute: %s", attributeName));
    }

    private void validateThatAllFieldsSet(List<NvidiaAttributesDTO> attributes) {
        for (NvidiaAttributesDTO attribute : attributes) {
            if (attribute.getGpuIndex() == null || attribute.getGpuCoreTemp() == null
                    || attribute.getGpuFanControlState() == null || attribute.getGpuTargetFanSpeed() == null) {
                throw new IllegalArgumentException("Some attributes are not set: " + attribute);
            }
        }
    }
}
