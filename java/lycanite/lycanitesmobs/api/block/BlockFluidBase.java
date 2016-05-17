package lycanite.lycanitesmobs.api.block;

import lycanite.lycanitesmobs.api.info.GroupInfo;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;

public class BlockFluidBase extends BlockFluidClassic {
    public String blockName;
    public GroupInfo group;

    // ==================================================
    //                   Constructor
    // ==================================================
    public BlockFluidBase(Fluid fluid, Material material, GroupInfo group, String blockName) {
        super(fluid, material);
        this.blockName = blockName;
        this.group = group;
        this.setRegistryName(new ResourceLocation(this.group.filename, this.blockName));
        this.setUnlocalizedName(this.blockName);

        this.setRenderLayer(BlockRenderLayer.TRANSLUCENT);
    }


    // ==================================================
    //                Collision Effects
    // ==================================================
    @Override
    public void onEntityCollidedWithBlock(World world, BlockPos pos, Entity entity) {
        super.onEntityCollidedWithBlock(world, pos, entity);
    }

    @Override
    public Boolean isEntityInsideMaterial(IBlockAccess world, BlockPos blockpos, IBlockState iblockstate, Entity entity, double yToTest, Material materialIn, boolean testingHead) {
        if(world instanceof World)
            this.onEntityCollidedWithBlock((World)world, blockpos, entity);
        return super.isEntityInsideMaterial(world, blockpos, iblockstate, entity, yToTest, materialIn, testingHead);
    }


    // ==================================================
    //                      Visuals
    // ==================================================
    @Override
    public EnumBlockRenderType getRenderType(IBlockState blockState) {
        return EnumBlockRenderType.MODEL;
    }
}
