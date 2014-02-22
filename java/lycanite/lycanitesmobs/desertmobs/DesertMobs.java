package lycanite.lycanitesmobs.desertmobs;

import lycanite.lycanitesmobs.Config;
import lycanite.lycanitesmobs.LycanitesMobs;
import lycanite.lycanitesmobs.ObjectLists;
import lycanite.lycanitesmobs.ObjectManager;
import lycanite.lycanitesmobs.PacketHandler;
import lycanite.lycanitesmobs.api.ILycaniteMod;
import lycanite.lycanitesmobs.api.dispenser.DispenserBehaviorMobEggCustom;
import lycanite.lycanitesmobs.api.item.ItemCustomFood;
import lycanite.lycanitesmobs.desertmobs.dispenser.DispenserBehaviorMudshot;
import lycanite.lycanitesmobs.desertmobs.dispenser.DispenserBehaviorThrowingScythe;
import lycanite.lycanitesmobs.desertmobs.entity.EntityClink;
import lycanite.lycanitesmobs.desertmobs.entity.EntityCrusk;
import lycanite.lycanitesmobs.desertmobs.entity.EntityCryptZombie;
import lycanite.lycanitesmobs.desertmobs.entity.EntityErepede;
import lycanite.lycanitesmobs.desertmobs.entity.EntityGorgomite;
import lycanite.lycanitesmobs.desertmobs.entity.EntityJoust;
import lycanite.lycanitesmobs.desertmobs.entity.EntityJoustAlpha;
import lycanite.lycanitesmobs.desertmobs.entity.EntityManticore;
import lycanite.lycanitesmobs.desertmobs.entity.EntityMudshot;
import lycanite.lycanitesmobs.desertmobs.entity.EntityThrowingScythe;
import lycanite.lycanitesmobs.desertmobs.item.ItemDesertEgg;
import lycanite.lycanitesmobs.desertmobs.item.ItemMudshotCharge;
import lycanite.lycanitesmobs.desertmobs.item.ItemScepterMudshot;
import lycanite.lycanitesmobs.desertmobs.item.ItemScepterScythe;
import lycanite.lycanitesmobs.desertmobs.item.ItemThrowingScythe;
import net.minecraft.block.BlockDispenser;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(modid = DesertMobs.modid, name = DesertMobs.name, version = LycanitesMobs.version, dependencies = "required-after:" + LycanitesMobs.modid)
@NetworkMod(clientSideRequired=true, serverSideRequired=false, channels = {DesertMobs.modid}, packetHandler = PacketHandler.class)
public class DesertMobs implements ILycaniteMod {
	
	public static final String modid = "DesertMobs";
	public static final String name = "Lycanites Desert Mobs";
	public static final String domain = modid.toLowerCase();
	public static int mobID = -1;
	public static int projectileID = 99;
	public static Config config = new SubConfig();
	
	// Instance:
	@Instance(modid)
	public static DesertMobs instance;
	
	// Proxy:
	@SidedProxy(clientSide="lycanite.lycanitesmobs.desertmobs.ClientSubProxy", serverSide="lycanite.lycanitesmobs.desertmobs.CommonSubProxy")
	public static CommonSubProxy proxy;
	
	// ==================================================
	//                Pre-Initialization
	// ==================================================
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		// ========== Config ==========
		config.init(modid);
		
		// ========== Set Current Mod ==========
		ObjectManager.setCurrentMod(this);
		
		// ========== Create Items ==========
		ObjectManager.addItem("DesertEgg", "Spawn", new ItemDesertEgg(config.itemIDs.get("DesertEgg")));
		ObjectManager.addItem("ThrowingScythe", "Throwing Scythe", new ItemThrowingScythe(config.itemIDs.get("ThrowingScythe")));
		
		ObjectManager.addItem("JoustMeatRaw", "Raw Joust Meat", new ItemCustomFood(config.itemIDs.get("JoustMeatRaw"), "JoustMeatRaw", domain, 2, 0.5F).setPotionEffect(Potion.moveSlowdown.id, 45, 2, 0.8F));
		ObjectLists.addItem("RawMeat", ObjectManager.getItem("JoustMeatRaw"));
		ObjectManager.addItem("JoustMeatCooked", "Cooked Joust Meat", new ItemCustomFood(config.itemIDs.get("JoustMeatCooked"), "JoustMeatCooked", domain, 6, 0.7F));
		ObjectLists.addItem("CookedMeat", ObjectManager.getItem("JoustMeatCooked"));
		ObjectManager.addItem("AmberCake", "Amber Cake", new ItemCustomFood(config.itemIDs.get("AmberCake"), "AmberCake", domain, 6, 0.7F).setPotionEffect(Potion.moveSpeed.id, 60, 2, 1.0F).setAlwaysEdible().setMaxStackSize(16));
		ObjectLists.addItem("CookedMeat", ObjectManager.getItem("AmberCake"));
		
		ObjectManager.addItem("MudshotCharge", "Mudshot Charge", new ItemMudshotCharge(config.itemIDs.get("MudshotCharge")));
		ObjectManager.addItem("ScytheScepter", "Scythe Scepter", new ItemScepterScythe(config.itemIDs.get("ScytheScepter")));
		ObjectManager.addItem("MudshotScepter", "Mudshot Scepter", new ItemScepterMudshot(config.itemIDs.get("MudshotScepter")));
		
		// ========== Create Blocks ==========
		
	}
	
	
	// ==================================================
	//                Initialization
	// ==================================================
	@EventHandler
	public void load(FMLInitializationEvent event) {
		
		// ========== Set Current Mod ==========
		ObjectManager.setCurrentMod(this);
		
		// ========== Create Mobs ==========
		BlockDispenser.dispenseBehaviorRegistry.putObject(ObjectManager.getItem("DesertEgg"), new DispenserBehaviorMobEggCustom());
		ObjectManager.addMob("CryptZombie", "Crypt Zombie", EntityCryptZombie.class, 0xCC9966, 0xAA8800);
		ObjectManager.addMob("Crusk", EntityCrusk.class, 0xFFDDAA, 0x000000);
		ObjectManager.addMob("Clink", EntityClink.class, 0xFFAAAA, 0x999999);
		ObjectManager.addMob("Joust", EntityJoust.class, 0xFF9900, 0xFFFF00);
		ObjectManager.addMob("JoustAlpha", "Joust Alpha", EntityJoustAlpha.class, 0xFF0000, 0xFFFF00);
		ObjectManager.addMob("Erepede", EntityErepede.class, 0xCCCCCC, 0x333333);
		ObjectManager.addMob("Gorgomite", EntityGorgomite.class, 0xCC9900, 0x884400);
		ObjectManager.addMob("Manticore", EntityManticore.class, 0x442200, 0x990000);
		
		// ========== Create Projectiles ==========
		ObjectManager.addProjectile("ThrowingScythe", EntityThrowingScythe.class, ObjectManager.getItem("ThrowingScythe"), new DispenserBehaviorThrowingScythe());
		ObjectManager.addProjectile("Mudshot", EntityMudshot.class, ObjectManager.getItem("MudshotCharge"), new DispenserBehaviorMudshot());
		
		// ========== Register Models ==========
		proxy.registerModels();
	}
	
	
	// ==================================================
	//                Post-Initialization
	// ==================================================
	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		// ========== Set Current Mod ==========
		ObjectManager.setCurrentMod(this);
		
		// ========== Remove Vanilla Spawns ==========
		BiomeGenBase[] biomes = this.config.getSpawnBiomesTypes();
		if(config.getFeatureBool("ControlVanilla")) {
			EntityRegistry.removeSpawn(EntityZombie.class, EnumCreatureType.monster, biomes);
			EntityRegistry.removeSpawn(EntitySkeleton.class, EnumCreatureType.monster, biomes);
			EntityRegistry.removeSpawn(EntityCreeper.class, EnumCreatureType.monster, biomes);
			EntityRegistry.removeSpawn(EntitySpider.class, EnumCreatureType.monster, biomes);
		}
		
		// ========== Crafting ==========
		GameRegistry.addRecipe(new ShapelessOreRecipe(
				new ItemStack(ObjectManager.getItem("ThrowingScythe"), 17, 0),
				new Object[] { Item.ingotIron, ObjectManager.getItem("ThrowingScythe") }
			));
		
		GameRegistry.addRecipe(new ShapedOreRecipe(
				new ItemStack(ObjectManager.getItem("ScytheScepter"), 1, 0),
				new Object[] { "CCC", "CRC", "CRC",
				Character.valueOf('C'), ObjectManager.getItem("ThrowingScythe"),
				Character.valueOf('R'), Item.blazeRod
			}));
		
		GameRegistry.addRecipe(new ShapedOreRecipe(
				new ItemStack(ObjectManager.getItem("MudshotScepter"), 1, 0),
				new Object[] { " C ", " R ", " R ",
				Character.valueOf('C'), ObjectManager.getItem("MudshotCharge"),
				Character.valueOf('R'), Item.blazeRod
			}));

		GameRegistry.addRecipe(new ShapelessOreRecipe(
				new ItemStack(ObjectManager.getItem("AmberCake"), 1, 0),
				new Object[] {
					Item.sugar,
					new ItemStack(Item.dyePowder, 1, 2),
					ObjectManager.getItem("JoustMeatCooked")
				}
			));
		
		// ========== Smelting ==========
		GameRegistry.addSmelting(ObjectManager.getItem("JoustMeatRaw").itemID, new ItemStack(ObjectManager.getItem("JoustMeatCooked"), 1), 0.5f);
	}
	
	
	// ==================================================
	//                    Mod Info
	// ==================================================
	@Override
	public DesertMobs getInstance() { return instance; }
	
	@Override
	public String getModID() { return modid; }
	
	@Override
	public String getDomain() { return domain; }
	
	@Override
	public Config getConfig() { return config; }
	
	@Override
	public int getNextMobID() { return ++this.mobID; }
	
	@Override
	public int getNextProjectileID() { return ++this.projectileID; }
}