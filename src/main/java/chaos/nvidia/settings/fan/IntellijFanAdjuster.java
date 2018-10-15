package chaos.nvidia.settings.fan;

import chaos.nvidia.settings.fan.nvidia.NvidiaAttributesDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class IntellijFanAdjuster {

    private static final int TIME_CAPACITY_IN_MS = 60_000;

    private Map<Integer, GpuStatistic> gpuStatisticMap = new HashMap<>();

    public int calculateFanSpeed(int newFanSpeed, NvidiaAttributesDTO attributes) {
        GpuStatistic gpuStatistic = clearAndGetGpuStatistic(attributes.getGpuIndex());
        gpuStatistic.getSnapshots().add(new GpuInfo(attributes.getGpuCoreTemp(), newFanSpeed, System.currentTimeMillis()));

        final GpuFanCycle gpuFanCycle;

        if (gpuStatistic.getGpuFanCycle() != null &&
                (gpuStatistic.getGpuFanCycle().getLowerTemp().getTemp() == attributes.getGpuCoreTemp()
                        || gpuStatistic.getGpuFanCycle().getUpperTemp().getTemp() == attributes.getGpuCoreTemp())) {
            gpuFanCycle = gpuStatistic.getGpuFanCycle();
        } else {
            gpuFanCycle = detectFanCycle(gpuStatistic);
            gpuStatistic.setGpuFanCycle(gpuFanCycle);
        }

        if (gpuFanCycle == null) {
            return newFanSpeed;
        } else {
            return (gpuFanCycle.getLowerTemp().getFanSpeed() + gpuFanCycle.getUpperTemp().getFanSpeed()) / 2;
        }
    }

    private GpuStatistic clearAndGetGpuStatistic(int gpuIndex) {
        GpuStatistic result = gpuStatisticMap.computeIfAbsent(gpuIndex, (index) -> new GpuStatistic(gpuIndex));
        long currentTimestamp = System.currentTimeMillis();

        for (int i = 0; i < result.getSnapshots().size(); i++) {
            GpuInfo gpuInfo = result.getSnapshots().get(i);
            if (currentTimestamp - gpuInfo.getTimestamp() >= TIME_CAPACITY_IN_MS) {
                result.getSnapshots().remove(i--);
            }
        }

        return result;
    }

    private GpuFanCycle detectFanCycle(GpuStatistic gpuStatistic) {
        GpuInfo lowerTemp = gpuStatistic.getSnapshots().stream().min(Comparator.comparingInt(GpuInfo::getTemp)).orElse(null);
        GpuInfo upperTemp = gpuStatistic.getSnapshots().stream().max(Comparator.comparingInt(GpuInfo::getTemp)).orElse(null);

        if (lowerTemp != null && upperTemp != null && lowerTemp.getTemp() + 1 == upperTemp.getTemp()) {
            return new GpuFanCycle(lowerTemp, upperTemp);
        }

        return null;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    private static class GpuFanCycle {
        private final GpuInfo lowerTemp;
        private final GpuInfo upperTemp;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    private static class GpuStatistic {
        private final int gpuIndex;
        private GpuFanCycle gpuFanCycle;
        private final List<GpuInfo> snapshots = new ArrayList<>();

        public GpuStatistic(int gpuIndex) {
            this.gpuIndex = gpuIndex;
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    private static class GpuInfo {
        private final int temp;
        private final int fanSpeed;
        private final long timestamp;
    }
}
