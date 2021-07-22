package chaos.nvidia.settings.fan.nvidia;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class NvidiaAttributesDTO {

    private Integer gpuIndex;
    private Integer fanIndex;
    // [gpu:0]/GPUFanControlState=1
    @Attribute("[gpu:%s]/GPUFanControlState")
    private Integer gpuFanControlState;
    // [gpu:0]/GPUCoreTemp
    @Attribute("[gpu:%s]/GPUCoreTemp")
    private Integer gpuCoreTemp;
    // [fan:0]/GPUTargetFanSpeed=50
    @Attribute("[fan:%s]/GPUTargetFanSpeed")
    private Integer gpuTargetFanSpeed;
}
