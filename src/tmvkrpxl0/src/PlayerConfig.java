package tmvkrpxl0.src;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SerializableAs("PlayerConfig")
public class PlayerConfig implements ConfigurationSerializable {
    protected boolean avoidArrow;
    protected Set<LivingEntity> targets;
    protected boolean multiShot;
    protected boolean homing;
    protected int selectMode;
    protected long skullCoolDown;
    protected long multiShotCoolDown;
    protected long tntCoolDown;
    protected int fishingMode;
    protected boolean isFishingRodThrown;
    protected boolean hasCoolDown;
    protected boolean doubleJump;
    protected boolean fallImpact;
    protected boolean burst;
    protected boolean burstWithMultishot;
    protected PlayerConfig(){
        avoidArrow = true;
        targets = new HashSet<>();
        multiShot = false;
        homing = false;
        selectMode = CombatUtil.RANDOM;
        skullCoolDown = 0;
        multiShotCoolDown = 0;
        tntCoolDown = 0;
        fishingMode = CombatUtil.DEFAULT;
        isFishingRodThrown = false;
        hasCoolDown = true;
        doubleJump = true;
        fallImpact = false;
        burst = false;
        burstWithMultishot = false;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("avoidArrow", avoidArrow);
        map.put("multiShot", multiShot);
        map.put("homing", homing);
        map.put("selectMode", selectMode);
        map.put("fishingMode", fishingMode);
        map.put("hasCoolDown", hasCoolDown);
        map.put("doubleJump", doubleJump);
        map.put("fallImpact", fallImpact);
        map.put("burst", burst);
        return map;
    }

    public static PlayerConfig deserialize(Map<String, Object> map){
        PlayerConfig config = new PlayerConfig();
        config.avoidArrow = (boolean) map.get("avoidArrow");
        config.multiShot = (boolean) map.get("multiShot");
        config.homing = (boolean) map.get("homing");
        config.selectMode = (int) map.get("selectMode");
        config.fishingMode = (int)map.get("fishingMode");
        config.hasCoolDown = (boolean) map.get("hasCoolDown");
        config.doubleJump = (boolean) map.get("doubleJump");
        config.fallImpact = (boolean) map.get("fallImpact");
        config.burst = (boolean) map.get("burst");
        return config;
    }
}
