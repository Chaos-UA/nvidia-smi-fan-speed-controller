package chaos.nvidia.settings.fan;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;

@Configuration
public class AppConfiguration {

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper result = new ObjectMapper();
        result.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return result;
    }

    @Bean
    public FanControllerConfig fanControllerConfig() {
        FanControllerConfig result = new FanControllerConfig();
        result.setSteps(new ArrayList<>());

        for (int i = 0; ; i++) {

            Integer temp = applicationContext.getEnvironment().getProperty(String.format("fanController.steps[%s].temp", i), Integer.class);
            Integer fanSpeed = applicationContext.getEnvironment().getProperty(String.format("fanController.steps[%s].fanSpeed", i), Integer.class);

            if (temp == null || fanSpeed == null) {
                break;
            }

            FanControllerConfig.Step step = new FanControllerConfig.Step();
            step.setTemp(temp);
            step.setFanSpeed(fanSpeed);
            result.getSteps().add(step);
        }

        return result;
    }
}
