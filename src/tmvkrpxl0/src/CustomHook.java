package tmvkrpxl0.src;

import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.util.Vector;

import java.util.Random;

public class CustomHook extends EntityFishingHook {
    protected boolean hasHookedEntity = false; //Has this hook ever hooked entity? *it's not (hooked!=null)*
    protected boolean pullable = false;
    protected Arrow arrow;
    private final Random b = new Random();
    public CustomHook(EntityHuman entityhuman, World world, Location location, Vector velocity, boolean rideArrow) {
        super(entityhuman, world, 0, 0);
        setNoGravity(true);
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

    private boolean isFishingRodOnHand(EntityHuman human){
        return a(human);
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
        } else if (this.world.isClientSide || !this.isFishingRodOnHand(entityhuman)) {
            BlockPosition blockposition = this.getChunkCoordinates();
            Fluid fluid = this.world.getFluid(blockposition);
            if (fluid.a(TagsFluid.LAVA)) {
                this.die();
            }
            if(this.h(entityhuman)>=10000){
                this.die();
            }
            if (hookState == HookState.FLYING) {
                if (hooked != null) {
                    this.setMot(Vec3D.ORIGIN);
                    hookState = HookState.HOOKED_IN_ENTITY;
                    return;
                }

                MovingObjectPosition movingobjectposition = ProjectileHelper.a(this, this::a);
                this.a(movingobjectposition);

            } else if (hookState == HookState.HOOKED_IN_ENTITY) {
                    if (hooked != null) {
                        if (hooked.dead) {
                            hooked = null;
                            die();
                            hookState = HookState.FLYING;
                        } else {
                            this.setPosition(hooked.locX(), hooked.e(0.8D), hooked.locZ());
                            hasHookedEntity = true;
                        }
                    }

                    return;
                }

            this.move(EnumMoveType.SELF, this.getMot());
            this.x();
            if (hookState == HookState.FLYING && (this.onGround || this.positionChanged)) {
                this.setMot(Vec3D.ORIGIN);
            }
            this.af();
        }
        if(arrow!=null && (arrow.isDead() || !arrow.isValid())){
            stopRiding();
            arrow.removePassenger(this.getBukkitEntity());
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
