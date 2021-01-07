package tmvkrpxl0.src;

import net.minecraft.server.v1_16_R3.DataWatcher;
import net.minecraft.server.v1_16_R3.EntityLiving;
import net.minecraft.server.v1_16_R3.PacketPlayOutEntityMetadata;
import org.apache.commons.lang.reflect.FieldUtils;
import org.bukkit.Location;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.Random;

public class CombatUtil {

    protected static final int RANDOM = 0;
    protected static final int CLOSEST = 1;
    protected static final int LOOKING = 2;

    protected static final int DEFAULT = 0;
    protected static final int ALL = 1;
    protected static final int GRAPPLINGHOOK = 2;

    protected static tmvkrpxl0Combat main = null;

    public static LivingEntity pickTarget(final Player player){
        return pickTarget(player, player.getLocation());
    }

    public static LivingEntity pickTarget(final Player player, final Location location){
        PlayerConfig config = main.playerConfigs.get(player.getUniqueId());
        switch(config.selectMode){
            case RANDOM:
                Random random = new Random();
                int size = config.targets.size();
                int idx = Math.abs(random.nextInt())%size;
                int i = 0;
                for(LivingEntity livingEntity : config.targets){
                    if(i==idx)return livingEntity;
                    i++;
                }
                return null;
            case CLOSEST:
                double distanceSquared = Integer.MAX_VALUE;
                LivingEntity closest = null;
                for(LivingEntity livingEntity : config.targets){
                    if(!player.hasLineOfSight(livingEntity))continue;
                    if(livingEntity.getLocation().distanceSquared(location) < distanceSquared){
                        distanceSquared = livingEntity.getLocation().distanceSquared(location);
                        closest = livingEntity;
                    }
                }
                return closest;
            case LOOKING:
                LivingEntity lookingAt = null;
                double angle = Math.PI * 2;
                Vector eyeVector = player.getEyeLocation().toVector();
                Vector direction = player.getEyeLocation().getDirection();
                for(LivingEntity entity : config.targets){
                    Vector difference = entity.getLocation().toVector().subtract(eyeVector);
                    double tempAngle = direction.angle(difference);
                    if(angle>tempAngle){
                        angle = tempAngle;
                        lookingAt = entity;
                    }
                }
                return lookingAt;
            default:
                throw new IllegalStateException("Player " + player.getName() + " has impossible selection mode");
        }
    }

    public static void selectTargets(final Player player){
        PlayerConfig config = main.playerConfigs.get(player.getUniqueId());
        config.targets.clear();
        final int degree = 60;
        Vector playerDirection = player.getEyeLocation().getDirection();
        Vector playerEyeVector = player.getEyeLocation().toVector();
        LivingEntity toReturn = null;
        double lastAngleDifference = 360;
        int size = 0;
        for(Entity e : player.getNearbyEntities(100, 100, 100)){
            if(!(e instanceof LivingEntity))continue;
            if(!player.hasLineOfSight(e))continue;
            LivingEntity entity = (LivingEntity)e;
            Vector entityVector = entity.getLocation().toVector();
            Vector difference = entityVector.subtract(playerEyeVector);
            double angleDifference = Math.toDegrees(playerDirection.angle(difference));
            if(angleDifference<=degree){
                config.targets.add(entity);
                size++;
                if(size==100)return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    public static void setGlowing(LivingEntity glowingEntity, Player sendPacketPlayer, boolean glow) {
        try {
            EntityLiving entityLiving = ((CraftLivingEntity) glowingEntity).getHandle();

            DataWatcher toCloneDataWatcher = entityLiving.getDataWatcher();
            DataWatcher newDataWatcher = new DataWatcher(entityLiving);

            // The map that stores the DataWatcherItems is private within the DataWatcher Object.
            // We need to use Reflection to access it from Apache Commons and change it.
            Int2ObjectOpenHashMap<DataWatcher.Item<?>> currentMap = (Int2ObjectOpenHashMap<DataWatcher.Item<?>>) FieldUtils.readDeclaredField(toCloneDataWatcher, "entries", true);
            Int2ObjectOpenHashMap<DataWatcher.Item<?>> newMap = new Int2ObjectOpenHashMap<>();

            // We need to clone the DataWatcher.Items because we don't want to point to those values anymore.
            for (Integer integer : currentMap.keySet()) {
                newMap.put(integer, currentMap.get(integer).d()); // Puts a copy of the DataWatcher.Item in newMap
            }

            // Get the 0th index for the BitMask value. http://wiki.vg/Entities#Entity
            DataWatcher.Item item = newMap.get(0);
            byte initialBitMask = (Byte) item.b(); // Gets the initial bitmask/byte value so we don't overwrite anything.
            byte bitMaskIndex = (byte) 6; // The index as specified in wiki.vg/Entities
            if (glow) {
                item.a((byte) (initialBitMask | 1 << bitMaskIndex));
            } else {
                item.a((byte) (initialBitMask & ~(1 << bitMaskIndex))); // Inverts the specified bit from the index.
            }

            // Set the newDataWatcher's (unlinked) map data
            FieldUtils.writeDeclaredField(newDataWatcher, "entries", newMap, true);

            PacketPlayOutEntityMetadata metadataPacket = new PacketPlayOutEntityMetadata(glowingEntity.getEntityId(), newDataWatcher, true);

            ((CraftPlayer) sendPacketPlayer).getHandle().playerConnection.sendPacket(metadataPacket);
        } catch (IllegalAccessException e) { // Catch statement necessary for FieldUtils.readDeclaredField()
            e.printStackTrace();
        }
    }

    @Nullable
    public static LivingEntity getEntityLookingAt(final Player player){
        return getEntityLookingAt(player, 17);
    }

    public static LivingEntity getEntityLookingAt(final Player player, double threadHoldAngle){
        Vector playerDirection = player.getEyeLocation().getDirection();
        Vector playerEyeVector = player.getEyeLocation().toVector();
        LivingEntity toReturn = null;
        double lastAngleDifference = 360;
        for(Entity e : player.getNearbyEntities(10, 10, 10)){
            if(!(e instanceof LivingEntity))continue;
            if(!player.hasLineOfSight(e))continue;
            LivingEntity entity = (LivingEntity)e;
            Vector entityVector = entity.getLocation().toVector();
            Vector difference = entityVector.subtract(playerEyeVector);
            double angleDifference = Math.toDegrees(playerDirection.angle(difference));
            if(angleDifference<=threadHoldAngle){
                if(toReturn == null ||
                        toReturn.getLocation().distanceSquared(player.getEyeLocation()) > entity.getLocation().distanceSquared(player.getEyeLocation()) ||
                        Math.abs(lastAngleDifference) > Math.abs(angleDifference)){
                    lastAngleDifference = angleDifference;
                    toReturn = entity;
                }
            }else{
                Vector entityEyeVector = entity.getEyeLocation().toVector();
                Vector eyeDifference = entityEyeVector.subtract(playerEyeVector);
                double eyeAngleDifference = Math.toDegrees(playerDirection.angle(eyeDifference));
                if(eyeAngleDifference<=threadHoldAngle){
                    if(toReturn == null ||
                            toReturn.getLocation().distanceSquared(player.getEyeLocation()) > entity.getLocation().distanceSquared(player.getEyeLocation()) ||
                            Math.abs(lastAngleDifference) > Math.abs(angleDifference)){
                        lastAngleDifference = angleDifference;
                        toReturn = entity;
                    }
                }
            }
        }
        return toReturn;
    }

}
