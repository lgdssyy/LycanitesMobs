package lycanite.lycanitesmobs.api.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.IEntityMultiPart;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;


public class EntityHitArea extends Entity {
    public Entity owner;

    public EntityHitArea(Entity ownerEntity, float width, float height) {
        super(ownerEntity.worldObj);
        this.owner = ownerEntity;
        this.setSize(width, height);
    }


    @Override
    protected void entityInit() {}


    @Override
    protected void readEntityFromNBT(NBTTagCompound p_70037_1_) {}


    @Override
    protected void writeEntityToNBT(NBTTagCompound p_70014_1_) {}


    @Override
    public boolean canBeCollidedWith()
    {
        return false;
    }


    @Override
    public boolean attackEntityFrom(DamageSource damageSource, float damageAmount) {
        if(this.isEntityInvulnerable())
            return false;
        if(this.owner instanceof EntityCreatureBase)
            return ((EntityCreatureBase)this.owner).attackEntityFromArea(this, damageSource, damageAmount);
        return this.owner.attackEntityFrom(damageSource, damageAmount);
    }

    @Override
    public boolean isEntityEqual(Entity entity) {
        return this == entity || this.owner == entity;
    }
}
