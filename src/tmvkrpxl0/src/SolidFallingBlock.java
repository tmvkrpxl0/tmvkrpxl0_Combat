package tmvkrpxl0.src;

import net.minecraft.server.v1_16_R3.*;

public class SolidFallingBlock extends EntityFallingBlock {
    public SolidFallingBlock(World world, double d0, double d1, double d2, IBlockData iblockdata){
        super(world, d0, d1, d2, iblockdata);
        this.ticksLived = 1;
        this.dropItem = false;
    }

    public SolidFallingBlock(EntityTypes entityTypes, World world) {
        super(EntityTypes.FALLING_BLOCK, world);
    }

    @Override
    public void tick() {
        super.tick();
        this.ticksLived = 1;
    }

    @Override
    public String getName() {
        return "solid_falling_block";
    }
}
