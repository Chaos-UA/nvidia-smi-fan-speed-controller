package chaos.nvidia.settings.fan;

import chaos.nvidia.settings.fan.nvidia.NvidiaAttributesDTO;
import chaos.nvidia.settings.fan.nvidia.NvidiaSettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

@Component
public class FanController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FanController.class);

    @Autowired
    private FanControllerConfig fanControllerConfig;

    @Autowired
    private NvidiaSettingsService nvidiaSettingsService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::restoreDefault));
    }

    public void start() throws InterruptedException {
        while (true) {
            try {
                List<NvidiaAttributesDTO> readAttributes = nvidiaSettingsService.getSettings();
                LOGGER.info("Read attributes: " + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(readAttributes));

                List<NvidiaAttributesDTO> attributesToUpdate = new ArrayList<>();

                for (NvidiaAttributesDTO attributes : readAttributes) {
                    int resultFanSpeed = calculateFanSpeed(attributes);
                    if (resultFanSpeed == attributes.getGpuTargetFanSpeed()) {
                        continue;
                    }

                    NvidiaAttributesDTO nvidiaAttributes = new NvidiaAttributesDTO();
                    nvidiaAttributes.setGpuIndex(attributes.getGpuIndex());
                    nvidiaAttributes.setGpuFanControlState(1); // unlock fan control
                    nvidiaAttributes.setGpuTargetFanSpeed(resultFanSpeed);
                    attributesToUpdate.add(nvidiaAttributes);
                }

                LOGGER.info("Modified attributes: " + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(attributesToUpdate));
                nvidiaSettingsService.updateNvidiaSettings(attributesToUpdate);

                Thread.sleep(fanControllerConfig.getIntervalSec() * 1000);
            } catch (Exception e) {
                LOGGER.error("Error", e);
                Thread.sleep(2000);
            }
        }
    }

    @PreDestroy
    public void stop() {
        restoreDefault();
    }

    private int calculateFanSpeed(NvidiaAttributesDTO attributes) {
        int currentTemp = attributes.getGpuCoreTemp();

        if (attributes.getGpuTargetFanSpeed() == 0 && fanControllerConfig.getMinTempThreshold() > currentTemp) {
            LOGGER.info("Threshold didn't pass, threshold temp: {}, current temp: {}", fanControllerConfig.getMinTempThreshold(), currentTemp);
            return 0;
        }

        StepRange stepRange = getStepRange(currentTemp, fanControllerConfig.getSteps());

        double result = (currentTemp - stepRange.getLowerStep().getTemp())
                * (double) (stepRange.getUpperStep().getFanSpeed() - stepRange.getLowerStep().getFanSpeed())
                / (stepRange.getUpperStep().getTemp() - stepRange.getLowerStep().getTemp())
                + stepRange.getLowerStep().getFanSpeed();

        int resultFanSpeed = (int) Math.round(result);
        if (resultFanSpeed > fanControllerConfig.getMaxFanSpeed()) {
            resultFanSpeed = fanControllerConfig.getMaxFanSpeed();
        }
        return resultFanSpeed;
    }

    private void restoreDefault() {
        if (fanControllerConfig.isRestoreDefaultOnExit()) {
            try {
                List<NvidiaAttributesDTO> readAttributes = nvidiaSettingsService.getSettings();

                List<NvidiaAttributesDTO> attributesToUpdate = new ArrayList<>();

                for (NvidiaAttributesDTO attributes : readAttributes) {
                    NvidiaAttributesDTO nvidiaAttributes = new NvidiaAttributesDTO();
                    nvidiaAttributes.setGpuIndex(attributes.getGpuIndex());
                    nvidiaAttributes.setGpuFanControlState(0); // restore default driver fan control
                    attributesToUpdate.add(nvidiaAttributes);
                }

                nvidiaSettingsService.updateNvidiaSettings(attributesToUpdate);
                LOGGER.info("Restored default behavior");
            } catch (Exception e) {
                LOGGER.error("Failed to restore default fan control behavior", e);
            }
        } else {
            LOGGER.info("Restored default behavior is disabled");
        }
    }

    private StepRange getStepRange(int currentTemp, List<FanControllerConfig.Step> steps) {
        FanControllerConfig.Step lowerStep = null;
        FanControllerConfig.Step upperStep = null;

        for (int i= 0; i < steps.size(); i++) {
            FanControllerConfig.Step step = steps.get(i);
            if (lowerStep == null) {
                lowerStep = step;
                upperStep = step;
            } else {
                if (currentTemp >= step.getTemp()) {
                    lowerStep = step;
                    if (i + 1 < steps.size()) {
                        upperStep = steps.get(i + 1);
                    } else {
                        upperStep = step;
                    }
                }
            }
        }

        if (lowerStep.getTemp() > upperStep.getTemp()) {
            upperStep = lowerStep;
        }

        return new StepRange(lowerStep, upperStep);
    }

    @Getter
    private static class StepRange {
        @NonNull
        private final FanControllerConfig.Step lowerStep;
        @NonNull
        private final FanControllerConfig.Step upperStep;

        public StepRange(FanControllerConfig.Step lowerStep, FanControllerConfig.Step upperStep) {
            this.lowerStep = lowerStep;
            this.upperStep = upperStep;
        }
    }
}
