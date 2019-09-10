package com.openrsc.server.plugins.skills;

import com.openrsc.server.Server;
import com.openrsc.server.constants.ItemId;
import com.openrsc.server.constants.Quests;
import com.openrsc.server.constants.Skills;
import com.openrsc.server.event.custom.BatchEvent;
import com.openrsc.server.event.rsc.GameStateEvent;
import com.openrsc.server.external.ObjectMiningDef;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.GameObject;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.plugins.listeners.action.ObjectActionListener;
import com.openrsc.server.plugins.listeners.executive.ObjectActionExecutiveListener;
import com.openrsc.server.util.rsc.DataConversions;
import com.openrsc.server.util.rsc.Formulae;

import static com.openrsc.server.plugins.Functions.*;

public final class Mining implements ObjectActionListener,
	ObjectActionExecutiveListener {

	/*static int[] ids;

	static {
		ids = new int[]{176, 100, 101, 102, 103, 104, 105, 106, 107, 108,
			109, 110, 111, 112, 113, 114, 115, 195, 196, 210, 211};
		Arrays.sort(ids);
	}*/

	public static int getAxe(Player p) {
		int lvl = p.getSkills().getLevel(com.openrsc.server.constants.Skills.MINING);
		for (int i = 0; i < Formulae.miningAxeIDs.length; i++) {
			if (p.getInventory().countId(Formulae.miningAxeIDs[i]) > 0) {
				if (lvl >= Formulae.miningAxeLvls[i]) {
					return Formulae.miningAxeIDs[i];
				}
			}
		}
		return -1;
	}

	@Override
	public GameStateEvent onObjectAction(final GameObject object, String command, Player player) {
		return new GameStateEvent(player.getWorld(), player, 0, getClass().getSimpleName() + " " + Thread.currentThread().getStackTrace()[1].getMethodName()) {
			public void init() {
				addState(0, () -> {
					if (object.getID() == 269) {
						if (command.equalsIgnoreCase("mine")) {
							if (hasItem(player, getAxe(player))) {
								if (getCurrentLevel(player, com.openrsc.server.constants.Skills.MINING) >= 50) {
									player.message("you manage to dig a way through the rockslide");
									if (player.getX() <= 425) {
										player.teleport(428, 438);
									} else {
										player.teleport(425, 438);
									}
								} else {
									player.message("You need a mining level of 50 to clear the rockslide");
								}
							} else {
								player.message("you need a pickaxe to clear the rockslide");
							}
						} else if (command.equalsIgnoreCase("prospect")) {
							player.message("these rocks contain nothing interesting");
							player.message("they are just in the way");
						}
					} else if (object.getID() == 770) {
						if (hasItem(player, getAxe(player))) {
							player.setBusyTimer(3);
							message(player, "you mine the rock", "and break of several large chunks");
							addItem(player, ItemId.ROCKS.id(), 1);
						} else {
							player.message("you need a pickaxe to mine this rock");
						}
					} else if (object.getID() == 1026) { // watchtower - rock of dalgroth
						if (command.equalsIgnoreCase("mine")) {
							if (player.getQuestStage(Quests.WATCHTOWER) == 9) {
								if (!hasItem(player, getAxe(player))) {
									player.message("You need a pickaxe to mine the rock");
									return null;
								}
								if (getCurrentLevel(player, com.openrsc.server.constants.Skills.MINING) < 40) {
									player.message("You need a mining level of 40 to mine this crystal out");
									return null;
								}
								if (hasItem(player, ItemId.POWERING_CRYSTAL4.id())) {
									playerTalk(player, null, "I already have this crystal",
										"There is no benefit to getting another");
									return null;
								}
								player.playSound("mine");
								// special bronze pick bubble for rock of dalgroth - see wiki
								showBubble(player, new Item(ItemId.BRONZE_PICKAXE.id()));
								player.message("You have a swing at the rock!");
								message(player, "You swing your pick at the rock...");
								player.message("A crack appears in the rock and you prize a crystal out");
								addItem(player, ItemId.POWERING_CRYSTAL4.id(), 1);
							} else {
								playerTalk(player, null, "I can't touch it...",
									"Perhaps it is linked with the shaman some way ?");
							}
						} else if (command.equalsIgnoreCase("prospect")) {
							player.playSound("prospect");
							message(player, "You examine the rock for ores...");
							player.message("This rock contains a crystal!");
						}
					} else {
						handleMining(object, player, player.click);
					}

					return null;
				});
			}
		};
	}

	private void handleMining(final GameObject object, Player player, int click) {
		final ObjectMiningDef def = player.getWorld().getServer().getEntityHandler().getObjectMiningDef(object.getID());
		final int axeId = getAxe(player);
		final int retrytimes;
		final int mineLvl = player.getSkills().getLevel(com.openrsc.server.constants.Skills.MINING);
		final int mineXP = player.getSkills().getExperience(Skills.MINING);
		final int reqlvl;
		switch (ItemId.getById(axeId)) {
			default:
			case BRONZE_PICKAXE:
				retrytimes = 1;
				reqlvl = 1;
				break;
			case IRON_PICKAXE:
				retrytimes = 2;
				reqlvl = 1;
				break;
			case STEEL_PICKAXE:
				retrytimes = 3;
				reqlvl = 6;
				break;
			case MITHRIL_PICKAXE:
				retrytimes = 5;
				reqlvl = 21;
				break;
			case ADAMANTITE_PICKAXE:
				retrytimes = 8;
				reqlvl = 31;
				break;
			case RUNE_PICKAXE:
				retrytimes = 12;
				reqlvl = 41;
				break;
		}

		player.getWorld().getServer().getGameEventHandler().add(new GameStateEvent(player.getWorld(), player, 0, "Mining") {
			public void init() {
				addState(0, () -> {
					if (getPlayerOwner().isBusy()) {
						return null;
					}
					if (!getPlayerOwner().withinRange(object, 1)) {
						return null;
					}

					if (getPlayerOwner().click == 0 && (def == null || (def.getRespawnTime() < 1 && object.getID() != 496) || (def.getOreId() == 315 && getPlayerOwner().getQuestStage(Quests.FAMILY_CREST) < 6))) {
						if (axeId < 0 || reqlvl > mineLvl) {
							message(getPlayerOwner(), "You need a pickaxe to mine this rock",
								"You do not have a pickaxe which you have the mining level to use");
							return null;
						}
						getPlayerOwner().setBusyTimer(3);
						getPlayerOwner().message("You swing your pick at the rock...");
						return invoke(1, 3);
					}
					if (getPlayerOwner().click == 1) {
						getPlayerOwner().playSound("prospect");
						getPlayerOwner().setBusyTimer(3);
						getPlayerOwner().message("You examine the rock for ores...");
						return invoke(2, 3);
					}
					if (axeId < 0 || reqlvl > mineLvl) {
						message(getPlayerOwner(), "You need a pickaxe to mine this rock",
							"You do not have a pickaxe which you have the mining level to use");
						return null;
					}
					if (getPlayerOwner().getFatigue() >= getPlayerOwner().MAX_FATIGUE) {
						getPlayerOwner().message("You are too tired to mine this rock");
						return null;
					}
					if (object.getID() == 496 && mineXP >= 210) {
						getPlayerOwner().message("Thats enough mining for now");
						return null;
					}
					getPlayerOwner().playSound("mine");
					showBubble(getPlayerOwner(), new Item(ItemId.IRON_PICKAXE.id()));
					getPlayerOwner().message("You swing your pick at the rock...");
					getPlayerOwner().setBatchEvent(new BatchEvent(getPlayerOwner().getWorld(), getPlayerOwner(), getPlayerOwner().getWorld().getServer().getConfig().GAME_TICK * 3, "Mining", retrytimes, true) {
						@Override
						public void action() {
							final Item ore = new Item(def.getOreId());
							if (getOre(getWorld().getServer(), def, getOwner().getSkills().getLevel(com.openrsc.server.constants.Skills.MINING), axeId) && mineLvl >= def.getReqLevel()) {
								if (DataConversions.random(1, 200) <= (getOwner().getInventory().wielding(ItemId.CHARGED_DRAGONSTONE_AMULET.id()) ? 2 : 1)) {
									getOwner().playSound("foundgem");
									Item gem = new Item(getGem(), 1);
									getOwner().getInventory().add(gem);
									getOwner().message("You just found a" + gem.getDef(getWorld()).getName().toLowerCase().replaceAll("uncut", "") + "!");
									interrupt();
								} else {
									//check if there is still ore at the rock
									GameObject obj = getOwner().getViewArea().getGameObject(object.getID(), object.getX(), object.getY());
									if (obj == null) {
										getOwner().message("You only succeed in scratching the rock");
									} else {
										getOwner().getInventory().add(ore);
										getOwner().message("You manage to obtain some " + ore.getDef(getWorld()).getName().toLowerCase());
										getOwner().incExp(com.openrsc.server.constants.Skills.MINING, def.getExp(), true);
									}
									if (object.getID() == 496 && getOwner().getCache().hasKey("tutorial") && getOwner().getCache().getInt("tutorial") == 51)
										getOwner().getCache().set("tutorial", 52);
									if (!getWorld().getServer().getConfig().MINING_ROCKS_EXTENDED || DataConversions.random(1, 100) <= def.getDepletion()) {
										interrupt();
										if (obj != null && obj.getID() == object.getID() && def.getRespawnTime() > 0) {
											GameObject newObject = new GameObject(getWorld(), object.getLocation(), 98, object.getDirection(), object.getType());
											getWorld().replaceGameObject(object, newObject);
											getWorld().delayedSpawnObject(obj.getLoc(), def.getRespawnTime() * 1000);
										}
									}
								}
							} else {
								if (object.getID() == 496) {
									getOwner().message("You fail to make any real impact on the rock");
								} else {
									getOwner().message("You only succeed in scratching the rock");
									if (getRepeatFor() > 1) {
										GameObject checkObj = getOwner().getViewArea().getGameObject(object.getID(), object.getX(), object.getY());
										if (checkObj == null) {
											interrupt();
										}
									}
								}
							}
							if (!isCompleted()) {
								showBubble(getOwner(), new Item(ItemId.IRON_PICKAXE.id()));
								getOwner().message("You swing your pick at the rock...");
							}
						}
					});

					return null;
				});
				addState(1, () -> {
					getPlayerOwner().message("There is currently no ore available in this rock");
					return null;
				});
				addState(2, () -> {
					if (getPlayerOwner().getID() == 496) {
						// Tutorial Island rock handler
						message(getPlayerOwner(), "This rock contains " + new Item(def.getOreId()).getDef(getPlayerOwner().getWorld()).getName(),
							"Sometimes you won't find the ore but trying again may find it",
							"If a rock contains a high level ore",
							"You will not find it until you increase your mining level");
						if (getPlayerOwner().getCache().hasKey("tutorial") && getPlayerOwner().getCache().getInt("tutorial") == 49)
							getPlayerOwner().getCache().set("tutorial", 50);
					} else {
						if (def == null || def.getRespawnTime() < 1) {
							getPlayerOwner().message("There is currently no ore available in this rock");
						} else {
							getPlayerOwner().message("This rock contains " + new Item(def.getOreId()).getDef(getPlayerOwner().getWorld()).getName());
						}
					}
					return null;
				});
			}
		});
	}

	@Override
	public boolean blockObjectAction(GameObject obj, String command, Player player) {
		return (command.equals("mine") || command.equals("prospect")) && obj.getID() != 588 && obj.getID() != 1227;
	}

	/**
	 * Returns a gem ID
	 */
	public int getGem() {
		int rand = DataConversions.random(0, 100);
		if (rand < 10) {
			return ItemId.UNCUT_DIAMOND.id();
		} else if (rand < 30) {
			return ItemId.UNCUT_RUBY.id();
		} else if (rand < 60) {
			return ItemId.UNCUT_EMERALD.id();
		} else {
			return ItemId.UNCUT_SAPPHIRE.id();
		}
	}

	private int calcAxeBonus(Server server, int axeId) {
			//If server doesn't use batching, pickaxe shouldn't improve gathering chance
			if (!server.getConfig().BATCH_PROGRESSION)
				return 0;
			int bonus = 0;
			switch (ItemId.getById(axeId)) {
			case BRONZE_PICKAXE:
				bonus = 0;
				break;
			case IRON_PICKAXE:
				bonus = 1;
				break;
			case STEEL_PICKAXE:
				bonus = 2;
				break;
			case MITHRIL_PICKAXE:
				bonus = 4;
				break;
			case ADAMANTITE_PICKAXE:
				bonus = 8;
				break;
			case RUNE_PICKAXE:
				bonus = 16;
				break;
			}
			return bonus;
	}

	/**
	 * Should we can get an ore from the rock?
	 */
	private boolean getOre(Server server, ObjectMiningDef def, int miningLevel, int axeId) {
		return Formulae.calcGatheringSuccessful(def.getReqLevel(), miningLevel, calcAxeBonus(server, axeId));
	}
}
