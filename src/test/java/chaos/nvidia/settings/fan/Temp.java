package chaos.nvidia.settings.fan;

import chaos.nvidia.settings.fan.nvidia.AttributeHelper;
import chaos.nvidia.settings.fan.nvidia.NvidiaSettingsService;
import org.junit.Test;

public class Temp {

    @Test
    public void justRun() throws Exception {
        NvidiaSettingsService nvidiaSettingsService = new NvidiaSettingsService();
        nvidiaSettingsService.setAttributeHelper(new AttributeHelper());
        nvidiaSettingsService.getSettings();
    }
}
