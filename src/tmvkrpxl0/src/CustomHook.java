package tmvkrpxl0.src;

import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.util.Random;

public class CustomHook extends EntityFishingHook {
    protected boolean hasHookedEntity = false;
    protected boolean pullable = false;
    private Field hookedField;
    private final Random b = new Random();
    private Field amField;
    private Object FLYING;
    private Object HOOKED_IN_ENTITY;
    protected Arrow arrow;

    public CustomHook(EntityHuman entityhuman, World world, Location location, Vector velocity, boolean rideArrow) {
        super(entityhuman, world, 0, 0);
        setNoGravity(true);
        try {
            hookedField = EntityFishingHook.class.getDeclaredField("hooked");
            hookedField.setAccessible(true);
            amField = EntityFishingHook.class.getDeclaredField("am");
            amField.setAccessible(true);
            Class<?> hookStateClass = Class.forName("net.minecraft.server.v1_16_R3.EntityFishingHook$HookState");
            Object[] enums = hookStateClass.getEnumConstants();
            FLYING = enums[0];
            HOOKED_IN_ENTITY = enums[1];
        } catch (NoSuchFieldException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        if(rideArrow){
            arrow = world.getWorld().spawnArrow(location, velocity, 3, 0);
            arrow.setGravity(false);
            arrow.setShooter(entityhuman.getBukkitEntity());
            arrow.addPassenger(this.getBukkitEntity());
        }else{
            setPosition(location.getX(), location.getY(), location.getZ());
            setMot(velocity.getX(), velocity.getY(), velocity.getZ());
        }
    }

    private boolean inFishingRodOnHand(EntityHuman human){
        ItemStack itemstack = human.getItemInMainHand();
        ItemStack itemstack1 = human.getItemInOffHand();
        boolean flag = itemstack.getItem() == Items.FISHING_ROD;
        boolean flag1 = itemstack1.getItem() == Items.FISHING_ROD;
        if (!human.dead && human.isAlive() && (flag || flag1) && this.h(human) <= 10000) {
            return false;
        } else {
            this.die();
            return true;
        }
    }

    @Override
    public void tick() {
        //Copy of tick() method from Entity.java
        if (!this.world.isClientSide) {
            this.setFlag(6, this.bE());
        }
        this.entityBaseTick();

        //Copy of tick() method from EntityFishingHook.java
        this.b.setSeed(this.getUniqueID().getLeastSignificantBits() ^ this.world.getTime());
        EntityHuman entityhuman = this.getOwner();
        if (entityhuman == null) {
            this.die();
        } else if (this.world.isClientSide || !this.inFishingRodOnHand(entityhuman)) {
            BlockPosition blockposition = this.getChunkCoordinates();
            Fluid fluid = this.world.getFluid(blockposition);
            Object tempAm = null;
            Entity hooked = k();
            try {
                tempAm = amField.get(this);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if (fluid.a(TagsFluid.LAVA)) {
                this.die();
            }
            if(this.h(entityhuman)>=10000){
                this.die();
            }
            if (tempAm == FLYING) {
                if (hooked != null) {
                    this.setMot(Vec3D.ORIGIN);
                    try {
                        amField.set(this, HOOKED_IN_ENTITY);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    return;
                }

                MovingObjectPosition movingobjectposition = ProjectileHelper.a(this, this::a);
                this.a(movingobjectposition);

            } else if (tempAm == HOOKED_IN_ENTITY) {
                    if (hooked != null) {
                        if (hooked.dead) {
                            setHookedField(null);
                            die();
                            try {
                                amField.set(this, FLYING);
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        } else {
                            this.setPosition(hooked.locX(), hooked.e(0.8D), hooked.locZ());
                            hasHookedEntity = true;
                        }
                    }

                    return;
                }

            this.move(EnumMoveType.SELF, this.getMot());
            this.x();
            if (tempAm == FLYING && (this.onGround || this.positionChanged)) {
                this.setMot(Vec3D.ORIGIN);
            }
            this.af();
        }
        if(arrow!=null && (arrow.isDead() || !arrow.isValid())){
            this.getBukkitEntity().leaveVehicle();
            arrow.removePassenger(this.getBukkitEntity());
        }
    }

    private void setHookedField(Entity entity){
        try {
            hookedField.set(this, entity);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }


    protected void onEntityHit(Entity entity){
        a(new MovingObjectPositionEntity(entity));
    }


    @Override
    public void die(){
        if(arrow!=null)arrow.remove();
        super.die();
    }
}
