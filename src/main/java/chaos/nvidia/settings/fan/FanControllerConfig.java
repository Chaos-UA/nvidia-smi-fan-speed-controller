package chaos.nvidia.settings.fan;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.util.List;

@ToString
public class FanControllerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(FanControllerConfig.class);

    private int maxFanSpeed;
    private int minTempThreshold;
    private int intervalSec;
    private boolean restoreDefaultOnExit;
    private boolean emulate;
    private boolean forceManualControl;
    private List<Step> steps;

    @PostConstruct
    public void init() {
        validateSettings();
        LOGGER.info("Config: " + this);
    }

    public int getMaxFanSpeed() {
        return maxFanSpeed;
    }

    @Value("${fanController.maxFanSpeed}")
    public void setMaxFanSpeed(int maxFanSpeed) {
        this.maxFanSpeed = maxFanSpeed;
    }

    public int getMinTempThreshold() {
        return minTempThreshold;
    }

    @Value("${fanController.minTempThreshold}")
    public void setMinTempThreshold(int minTempThreshold) {
        this.minTempThreshold = minTempThreshold;
    }

    public int getIntervalSec() {
        return intervalSec;
    }

    @Value("${fanController.intervalSec}")
    public void setIntervalSec(int intervalSec) {
        this.intervalSec = intervalSec;
    }

    public boolean isRestoreDefaultOnExit() {
        return restoreDefaultOnExit;
    }

    @Value("${fanController.restoreDefaultOnExit}")
    public void setRestoreDefaultOnExit(boolean restoreDefaultOnExit) {
        this.restoreDefaultOnExit = restoreDefaultOnExit;
    }

    public boolean isEmulate() {
        return emulate;
    }

    @Value("${fanController.emulate}")
    public void setEmulate(boolean emulate) {
        this.emulate = emulate;
    }

    public boolean isForceManualControl() {
        return forceManualControl;
    }

    @Value("${fanController.forceManualControl}")
    public void setForceManualControl(boolean forceManualControl) {
        this.forceManualControl = forceManualControl;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }

    private void validateSettings() {
        validateSteps();
    }

    private void validateSteps() {
        if (steps.isEmpty()) {
            throw new RuntimeException("Temperature steps is empty");
        }

        for (int i = 0; i < steps.size() - 1; i++) {
            Step step1 = steps.get(i);
            Step step2 = steps.get(i + 1);

            if (step1.getTemp() >= step2.getTemp() || step1.getFanSpeed() > step2.getFanSpeed()) {
                throw new IllegalArgumentException(
                        String.format("Validation failed for the steps: %s and %s. Seems that incorrect config", step1, step2)
                );
            }
        }
    }

    @Getter
    @Setter
    @ToString
    @ConfigurationProperties("fanController.steps")
    public static class Step {
        private Integer temp;
        private Integer fanSpeed;
    }
}
