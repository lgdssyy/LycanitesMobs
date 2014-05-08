package lycanite.lycanitesmobs.api.item;

import java.util.List;

import lycanite.lycanitesmobs.ExtendedPlayer;
import lycanite.lycanitesmobs.api.entity.EntityCreatureBase;
import lycanite.lycanitesmobs.api.info.CreatureKnowledge;
import lycanite.lycanitesmobs.api.info.MobInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

public class ItemSoulgazer extends ItemBase {
	
	// ==================================================
	//                   Constructor
	// ==================================================
    public ItemSoulgazer() {
        super();
        this.setMaxStackSize(1);
        this.itemName = "soulgazer";
        this.textureName = this.itemName.toLowerCase();
        setUnlocalizedName(this.itemName);
    }
    
    
	// ==================================================
	//                      Info
	// ==================================================
    @Override
    public void addInformation(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, List par3List, boolean par4) {
    	par3List.add("\u00a7a" + "Right click on a mob");
    	par3List.add("\u00a7a" + "to look into it's soul.");
    	par3List.add("\u00a7a" + "A creatures soul can");
    	par3List.add("\u00a7a" + "teach many things!");
    	super.addInformation(par1ItemStack, par2EntityPlayer, par3List, par4);
    }
	
    
	// ==================================================
	//                      Update
	// ==================================================
	@Override
	public void onUpdate(ItemStack itemStack, World world, Entity entity, int par4, boolean par5) {
		super.onUpdate(itemStack, world, entity, par4, par5);
	}
    
    
	// ==================================================
	//                       Use
	// ==================================================
	// ========== Entity Interaction ==========
    public boolean onItemRightClickOnEntity(EntityPlayer player, Entity entity) {
    	ExtendedPlayer playerExt = ExtendedPlayer.getForPlayer(player);
    	if(playerExt == null)
    		return false;
    	if(!(entity instanceof EntityCreatureBase)) {
    		if(!player.worldObj.isRemote)
    			player.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("message.soulgazer.unknown")));
    		return false;
    	}
    	MobInfo mobInfo = ((EntityCreatureBase)entity).mobInfo;
    	if(playerExt.getBeastiary().hasFullKnowledge(mobInfo.name)) {
    		if(!player.worldObj.isRemote)
    			player.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("message.soulgazer.known")));
    		return false;
    	}
    	
    	if(player.worldObj.isRemote) {
    		for(int i = 0; i < 32; ++i) {
    			entity.worldObj.spawnParticle("happyVillager",
    					entity.posX + (4.0F * player.getRNG().nextFloat()) - 2.0F,
    					entity.posY + (4.0F * player.getRNG().nextFloat()) - 2.0F,
    					entity.posZ + (4.0F * player.getRNG().nextFloat()) - 2.0F,
        				0.0D, 0.0D, 0.0D);
    		}
    	}
    	
    	if(!player.worldObj.isRemote)
    		player.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("message.soulgazer.new")));
    	
    	playerExt.getBeastiary().addToKnowledgeList(new CreatureKnowledge(player, mobInfo.name, 1));
    	return true;
    }
}
