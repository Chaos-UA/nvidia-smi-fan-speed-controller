package chaos.nvidia.settings.fan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FanSpeedControllerApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(FanSpeedControllerApplication.class);

	public static void main(String[] args) {
		try {
			SpringApplication.run(FanSpeedControllerApplication.class, args).getBean(FanController.class).start();
			System.exit(0);
		} catch (Exception e) {
			LOGGER.info("Error during startup", e);
			System.exit(1);
		}
	}
}
