package lycanite.lycanitesmobs.api.mobevent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javafx.util.Pair;
import lycanite.lycanitesmobs.ExtendedWorld;
import lycanite.lycanitesmobs.LycanitesMobs;
import lycanite.lycanitesmobs.Utilities;
import lycanite.lycanitesmobs.api.config.ConfigSpawning;
import lycanite.lycanitesmobs.api.network.MessageMobEvent;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.WorldTickEvent;
import org.apache.commons.lang3.math.NumberUtils;


public class MobEventManager {
	// Global:
    public static MobEventManager instance;
    public static boolean mobEventsEnabled = true;
    public static boolean mobEventsRandom = true;
    public static int minEventsRandomDay = 0;
    public static boolean mobEventsLocked = false;
    public static boolean useTotalWorldTime = true;
    public static int minTicksUntilEvent = 10 * 60 * 20;
    public static int maxTicksUntilEvent = 30 * 60 * 20;
    public static String mobEventsSchedule = "";
    
    // Mob Events:
    public Map<String, MobEventBase> worldMobEvents = new HashMap<String, MobEventBase>();
    public Map<String, Map<String, MobEventBase>> worldMobEventSets = new HashMap<String, Map<String, MobEventBase>>();
    public MobEventBase serverMobEvent = null;
    public MobEventClient clientMobEvent = null;
    public Map<Pair<Integer, Integer>, MobEventBase> eventSchedule;

    // Properties:
    public int baseRate = 10 * 20;
    public int baseRange = 32;
    

    // ==================================================
    //                     Constructor
    // ==================================================
	public MobEventManager() {
		instance = this;
	}


    // ==================================================
    //                  Load Mob Events
    // ==================================================
	/** Called during start up, loads all global events and config settings into the manager. **/
	public void loadMobEvents() {
		ConfigSpawning config = ConfigSpawning.getConfig(LycanitesMobs.group, "mobevents");
        mobEventsEnabled = config.getBool("Global", "Mob Events Enabled", mobEventsEnabled, "If false, all mob events will be completely disabled.");

        mobEventsRandom = config.getBool("Global", "Random Mob Events", mobEventsRandom, "If false, mob events will no longer occur randomly but can still occur via other means such as by schedule. Set this to true if you want both random and scheduled events to take place and also take a look at 'Lock Random Mob Events' if doing so.");
        minEventsRandomDay = config.getInt("Global", "Random Mob Events Day Minimum", minEventsRandomDay, "If random events are enabled, they wont occur until this day is reached. Set to 0 to have random events enabled from the start of a world.");

        mobEventsSchedule = config.getString("Global", "Mob Events Schedule", mobEventsSchedule, "Here you can add a list of scheduled events, leave blank to disable. Each entry should be: 'eventname,day,time' multiple entries should be separated by semicolons. For eventnames use '/lm mobevent list' when in game. Day is the target day number starting from day 0, see 'Use Total World Time' for how the world time is checked. Time is the minute of the in game day that the event should occur (there is 24 minutes in a Minecraft world use 0-23), this can also be random by typing 'random' as a value, note that you can only have 1 random scheduled event per day. You may add spaces anywhere as they will be ignored, also don't add the 'quotations'!");
        mobEventsLocked = config.getBool("Global", "Lock Random Mob Events", mobEventsLocked, "If true, mob events will not occur randomly until they have been started from a schedule (or if an active world's schedule would have started the event at least once in the past).");

        useTotalWorldTime = config.getBool("Global", "Use Total World Time", useTotalWorldTime, "If true, the hidden total world time will be used event minimum days/schedules, if false the current world time is used instead, the current time is the time shown to players however it will reset to 0 if the world time is change via '/time set 0' or other commands/mods.");

        minTicksUntilEvent = config.getInt("Global", "Min Ticks Until Event", minTicksUntilEvent, "Minimum time in ticks until a random event can occur. 20 Ticks = 1 Second.");
		maxTicksUntilEvent = config.getInt("Global", "Max Ticks Until Event", maxTicksUntilEvent, "Maximum time in ticks until a random event can occur. 20 Ticks = 1 Second.");

        baseRate = config.getInt("Global", "Base Spawn Rate", baseRate, "Sets the base interval in ticks (20 ticks = 1 second) between each mob spawn, this is multiplied by 1.5 on easy and 0.5 on hard.");
		baseRange = config.getInt("Global", "Base Spawn Range", baseRange, "Sets the base range in blocks from each player/area that event mobs will spawn.");
		
		config.setCategoryComment("Events Enabled", "Here each event can be turned on or off (true or false).");
	}


    // ==================================================
    //                 Add Mob Event
    // ==================================================
    /**
     * Adds the provided World Mob Event.
     *  **/
    public void addWorldEvent(MobEventBase mobEvent, String set) {
        if(mobEvent != null && mobEvent.hasSpawners()) {
        	mobEvent.loadFromConfig();
            if(!this.worldMobEventSets.containsKey(set))
                this.worldMobEventSets.put(set, new HashMap<String, MobEventBase>());
            this.worldMobEventSets.get(set).put(mobEvent.name, mobEvent);
            this.worldMobEvents.put(mobEvent.name, mobEvent);
        }
    }

    public void addWorldEvent(MobEventBase mobEvent) {
        this.addWorldEvent(mobEvent, "main");
    }


    // ==================================================
    //                    Event Stats
    // ==================================================
    /* Returns the rate that event mobs are spawned at in ticks. This changes based on the difficulty of the provided world. */
    public int getBaseRate() {
        return this.baseRate;
    }

    /* Returns the distance from the player that event mobs are spawned from. This changes based on the difficulty of the provided world. */
    public int getBaseRange() {
        return this.baseRange;
    }


    // ==================================================
    //                 World Update Event
    // ==================================================
	/** Called every tick in a world and counts down to the next event then fires it! The countdown is paused during an event. **/
	@SubscribeEvent
	public void onWorldUpdate(WorldTickEvent event) {
		World world = event.world;
		ExtendedWorld worldExt = ExtendedWorld.getForWorld(world);
		if(world.isRemote || worldExt == null) return;

        // Check If Events Are Completely Disabled:
        if(!this.mobEventsEnabled || world.difficultySetting == EnumDifficulty.PEACEFUL) {
            if(this.serverMobEvent != null)
                this.stopMobEvent();
            return;
        }

        // Only Tick On World Time Ticks:
        if(worldExt.lastEventUpdateTime == world.getTotalWorldTime()) return;
        worldExt.lastEventUpdateTime = world.getTotalWorldTime();
		
		// Only Run If Players Are Present:
		if(world.playerEntities.size() < 1) {
			return;
		}

        // Scheduled Events:
        if(this.eventSchedule != null && this.eventSchedule.size() > 0) {
            MobEventBase newEvent = this.getScheduledWorldMobEvent(world, worldExt);
            if(newEvent != null) {
                this.startMobEvent(newEvent, world);
            }
        }

        // Update Active Event If Present and Return:
        if(this.serverMobEvent != null) {
            this.serverMobEvent.onUpdate();
            return;
        }

        // Random Events:
        if(!mobEventsRandom) return;
        if(minEventsRandomDay > 0 && Math.floor((useTotalWorldTime ? world.getTotalWorldTime() : world.getWorldTime()) / 24000D) < minEventsRandomDay) return;
        if(worldExt.getMobEventStartTargetTime() <= 0 || worldExt.getMobEventStartTargetTime() > world.getTotalWorldTime() + maxTicksUntilEvent) {
            worldExt.setMobEventStartTargetTime(world.getTotalWorldTime() + this.getRandomEventDelay(world.rand));
        }
        if(world.getTotalWorldTime() >= worldExt.getMobEventStartTargetTime()) {
			MobEventBase newEvent = this.getRandomWorldMobEvent(world, worldExt);
			if(newEvent != null) {
                this.startMobEvent(newEvent, world);
			}
		}
    }
	
	
    // ==================================================
    //                 Client Update Event
    // ==================================================
	/** Called every tick in a world and counts down to the next event then fires it! The countdown is paused during an event. **/
	@SubscribeEvent
	public void onClientUpdate(ClientTickEvent event) {
		if(LycanitesMobs.proxy.getClientPlayer() == null)
			return;
		
		// Update Active Event and Return:
		if(this.clientMobEvent != null) {
			if(this.clientMobEvent.world == LycanitesMobs.proxy.getClientPlayer().worldObj)
				this.clientMobEvent.onUpdate();
			return;
		}
	}


    // ==================================================
    //                Scheduled Events
    // ==================================================
    public void loadEventSchedule(String schedule) {
        schedule = schedule.replace(" ", "");
        if("".equals(schedule))
            return;
        this.eventSchedule = new HashMap<Pair<Integer, Integer>, MobEventBase>();

        for(String scheduleEntry : schedule.split(";")) {
            String[] scheduleEntryParts = scheduleEntry.split(",");
            if(scheduleEntryParts.length >= 3) {
                if(!NumberUtils.isNumber(scheduleEntryParts[1])) continue;
                int scheduleEntryDay = Integer.parseInt(scheduleEntryParts[1]);

                if(!NumberUtils.isNumber(scheduleEntryParts[2])) continue;
                int scheduleEntryMinute = Integer.parseInt(scheduleEntryParts[2]);

                if(!worldMobEvents.containsKey(scheduleEntryParts[0])) continue;
                MobEventBase scheduleEntryEvent = worldMobEvents.get(scheduleEntryParts[0]);

                this.eventSchedule.put(new Pair<Integer, Integer>(scheduleEntryDay, scheduleEntryMinute), scheduleEntryEvent);
            }
        }
    }

    public MobEventBase getScheduledWorldMobEvent(World world, ExtendedWorld worldExt) {
        if(eventSchedule == null)
            return null;

        int dimensionID = 0;
        if(world.provider != null)
            dimensionID = world.provider.dimensionId;

        int currentDay = (int)Math.floor((MobEventManager.useTotalWorldTime ? world.getTotalWorldTime() : world.getWorldTime()) / 24000D);
        int currentMinute = (int)((long)Math.floor((MobEventManager.useTotalWorldTime ? world.getTotalWorldTime() : world.getWorldTime()) / 1000D) % 24);
        if(worldExt.lastEventScheduleMinute != currentMinute) {
            worldExt.lastEventScheduleMinute = currentMinute;
            if(currentMinute == 0 && eventSchedule.containsKey(new Pair<Integer, Integer>(currentDay, -1))) {
                MobEventBase randomMinuteEvent = eventSchedule.get(new Pair<Integer, Integer>(currentDay, -1));
                eventSchedule.put(new Pair<Integer, Integer>(currentDay, currentMinute + 1 + world.rand.nextInt(22 - currentMinute)), randomMinuteEvent);
                eventSchedule.remove(new Pair<Integer, Integer>(currentDay, -1));
            }
            if(eventSchedule.containsKey(new Pair<Integer, Integer>(currentDay, currentMinute)))
                return eventSchedule.get(new Pair<Integer, Integer>(currentDay, currentMinute));
        }

        return null;
    }


    // ==================================================
    //                 Random Mob Events
    // ==================================================
	/**
	 * Returns a random world-based event for the given world.
	 * @return Returns a an appropriate weighted random Mob Event or null if none are available.
	 *  **/
	public MobEventBase getRandomWorldMobEvent(World world, ExtendedWorld worldExt) {
		int dimensionID = 0;
		if(world.provider != null)
			dimensionID = world.provider.dimensionId;
		
		// Seasonal Events:
		if(Utilities.isHalloween() && this.worldMobEvents.containsKey("halloween"))
			if(this.worldMobEvents.get("halloween").isEnabled() && this.worldMobEvents.get("halloween").canStart(world, worldExt))
				return this.worldMobEvents.get("halloween");
		/*if(Utilities.isNewYear() && this.worldMobEvents.containsKey("newyear"))
			if(this.worldMobEvents.get("newyear").isEnabled() && this.worldMobEvents.get("newyear").canStart(world, worldExt))
				return this.worldMobEvents.get("newyear");*/
		
		// Seasonal Event Sets:
        Map<String, MobEventBase> worldMobEventSet = this.worldMobEventSets.get("main");
        if(Utilities.isYuletide() && this.worldMobEventSets.containsKey("yule"))
            worldMobEventSet = this.worldMobEventSets.get("yule");

        // Get Events and Weights:
		List<MobEventBase> validMobEvents = new ArrayList<MobEventBase>();
		int totalWeights = 0;
		for(MobEventBase mobEventEntry : worldMobEventSet.values()) {
			if(mobEventEntry.isEnabled() && mobEventEntry.canStart(world, worldExt)) {
				totalWeights += mobEventEntry.weight;
				validMobEvents.add(mobEventEntry);
			}
		}
		if(totalWeights <= 0)
			return null;
		
		// Pick Random Event Using Weights:
		int randomWeight = 1;
		if(totalWeights > 1)
			randomWeight = world.rand.nextInt(totalWeights - 1) + 1;
		int searchWeight = 0;
		MobEventBase mobEvent = null;
		for(MobEventBase mobEventEntry : validMobEvents) {
			if(mobEventEntry.isEnabled()) {
				mobEvent = mobEventEntry;
				if(mobEventEntry.weight + searchWeight > randomWeight)
					break;
				searchWeight += mobEventEntry.weight;
			}
		}
		
		return mobEvent;
	}


    // ==================================================
    //                Random Event Delay
    // ==================================================
	/** Gets a random time until the next random event will start. **/
	public int getRandomEventDelay(Random random) {
		int max = Math.max(20, maxTicksUntilEvent);
		int min = Math.max(20, minTicksUntilEvent);
		if(max <= min)
			return min;
		
		return min + random.nextInt(max - min);
	}


    // ==================================================
    //                 Stop Mob Event
    // ==================================================
    /**
     * Stops the Mob Event.
     *  **/
    public void stopMobEvent() {
        // Server Side:
        if(this.serverMobEvent != null) {
            this.serverMobEvent.onFinish();
            if(this.serverMobEvent.world != null) {
                ExtendedWorld worldExt = ExtendedWorld.getForWorld(this.serverMobEvent.world);
                worldExt.setMobEventType("");
            }
            this.serverMobEvent = null;
            this.updateAllClients();
        }

        // Client Side:
        if(this.clientMobEvent != null) {
            if(LycanitesMobs.proxy.getClientPlayer() != null)
            	this.clientMobEvent.onFinish(LycanitesMobs.proxy.getClientPlayer());
            this.clientMobEvent = null;
        }
    }


    // ==================================================
    //                 Start Mob Event
    // ==================================================
    /**
     * Starts the provided Mob Event (provided by instance) on the provided world.
     *  **/
    public void startMobEvent(MobEventBase mobEvent, World world) {
        if(mobEvent == null) {
            LycanitesMobs.printWarning("", "Tried to start a null mob event, stopping any event instead.");
            this.stopMobEvent();
        }

        // Server Side:
        if(!world.isRemote) {
            if(this.serverMobEvent != null)
                this.stopMobEvent();
            this.serverMobEvent = mobEvent;
            ExtendedWorld worldExt = ExtendedWorld.getForWorld(world);
            worldExt.setMobEventType(this.serverMobEvent.name);
            worldExt.increaseMobEventCount();
            worldExt.setMobEventStartTargetTime(0);
            worldExt.setMobEventLastStartedTime(world.getTotalWorldTime());
            this.serverMobEvent.onStart(world);
            this.updateAllClients();
        }

        // Client Side:
        if(world.isRemote) {
            if(this.clientMobEvent != null)
                this.stopMobEvent();
            this.clientMobEvent = mobEvent.getClientEvent(world);
            if(LycanitesMobs.proxy.getClientPlayer() != null)
            	this.clientMobEvent.onStart(world, LycanitesMobs.proxy.getClientPlayer());
        }
    }

    /**
     * Starts the provided Mob Event (provided by name) on the provided world.
     *  **/
    public void startMobEvent(String mobEventName, World world) {
        MobEventBase mobEvent = null;
        if(this.worldMobEvents.containsKey(mobEventName)) {
            mobEvent = this.worldMobEvents.get(mobEventName);
            if(!mobEvent.isEnabled())
            	mobEvent = null;
    	}
        else {
        	LycanitesMobs.printWarning("", "Tried to start an event with the invalid name: '" + mobEventName + "' on " + (world.isRemote ? "Client" : "Server"));
        	return;
        }
        
        this.startMobEvent(mobEvent, world);
    }


    // ==================================================
    //                  Update Clients
    // ==================================================
    /** Sends a packet to all clients updating their event. **/
    public void updateAllClients() {
        MessageMobEvent message = new MessageMobEvent(this.serverMobEvent);
        LycanitesMobs.packetHandler.sendToAll(message);
        
    }
}
