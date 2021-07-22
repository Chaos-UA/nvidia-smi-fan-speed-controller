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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        if (nvidiaAttributes.isEmpty()) {
            return;
        }

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

        Map<Integer, GpuInfo> fanGpuMap = getFanGpuMap();

        String output = IOUtils.toString(process.getInputStream(), "UTF-8");

        List<NvidiaAttributesDTO> result = parseNvidiaSettingsOutput(fanGpuMap, output);
        return result;
    }

    private List<NvidiaAttributesDTO> parseNvidiaSettingsOutput(Map<Integer, GpuInfo> fanGpuMap, String output) {
        Map<Integer, NvidiaAttributesDTO> resultMap = new LinkedHashMap<>();

        String[] outputLines = output.split("\n");

        for (String line : outputLines) {
            Matcher matcher = READ_ATTRIBUTE_PATTERN.matcher(line);
            if (matcher.find()) {
                String attributeName = matcher.group(1);
                int index = Integer.valueOf(matcher.group(2));
                int value = Integer.valueOf(matcher.group(3));
                boolean isFanAttribute = "GPUTargetFanSpeed".equals(attributeName);
                GpuInfo gpuInfo = fanGpuMap.get(index);
                final int gpuIndex = isFanAttribute ? gpuInfo.getIndex() : index;

                NvidiaAttributesDTO attributes = resultMap.computeIfAbsent(gpuIndex, (key) -> {
                    NvidiaAttributesDTO nvidiaAttributes = new NvidiaAttributesDTO();
                    nvidiaAttributes.setGpuIndex(gpuIndex);
                    return nvidiaAttributes;
                });

                if (isFanAttribute) {
                    attributes.setFanIndex(index);
                }

                setAttributeValue(attributeName, value, attributes);
            }
        }

        List<NvidiaAttributesDTO> result = new ArrayList<>(resultMap.values());

        // multiple fans not supported yet, so remove
        result.removeIf(v -> fanGpuMap.get(v.getFanIndex()).getFanIndexes().size() > 1);

        if (!fanControllerConfig.isForceManualControl()) {
            result.removeIf(v -> v.getGpuFanControlState() != 1);
        } else {
            throw new RuntimeException("Not supported");
        }

        validateThatAllFieldsSet(result, output);

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

    private void validateThatAllFieldsSet(List<NvidiaAttributesDTO> attributes, String output) {
        for (NvidiaAttributesDTO attribute : attributes) {
            if (attribute.getGpuIndex() == null || attribute.getGpuCoreTemp() == null
                    || attribute.getGpuFanControlState() == null || attribute.getGpuTargetFanSpeed() == null) {
                throw new IllegalArgumentException(String.format("Some attributes are not set: %s, result: %s",
                        attribute, output));
            }
        }
    }

    /**
     * key is an fan index.
     */
    @SneakyThrows
    private Map<Integer, GpuInfo> getFanGpuMap() {
        String cmd = "nvidia-settings -q gpus --verbose";
        Process process = Runtime.getRuntime().exec(cmd);
        Map<Integer, GpuInfo> result = new LinkedHashMap<>();
        String output = IOUtils.toString(process.getInputStream(), "UTF-8");

        GpuInfo gpuInfo = null;
        for (String line : output.split("\n")) {
            Pattern pattern = Pattern.compile("gpu:(\\d)\\] \\(([\\w \\d-]+)");
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                gpuInfo = new GpuInfo();
                gpuInfo.setFanIndexes(new LinkedHashSet<>());
                gpuInfo.setIndex(Integer.parseInt(matcher.group(1)));
                gpuInfo.setName(matcher.group(2));
                continue;
            }

            pattern = Pattern.compile("\\[fan:(\\d)\\]");
            matcher = pattern.matcher(line);
            if (matcher.find()) {
                int fanIndex = Integer.parseInt(matcher.group(1));
                gpuInfo.getFanIndexes().add(fanIndex);
                result.put(fanIndex, gpuInfo);
            }
        }
        return result;
    }

    private static class GpuInfo {
        private String name;
        private Integer index;
        private Set<Integer> fanIndexes;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public Set<Integer> getFanIndexes() {
            return fanIndexes;
        }

        public void setFanIndexes(Set<Integer> fanIndexes) {
            this.fanIndexes = fanIndexes;
        }
    }
}
