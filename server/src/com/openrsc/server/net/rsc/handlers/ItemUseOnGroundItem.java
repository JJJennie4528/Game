package com.openrsc.server.net.rsc.handlers;

import com.openrsc.server.model.Point;
import com.openrsc.server.model.action.WalkToPointAction;
import com.openrsc.server.model.container.Inventory;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.GroundItem;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.states.Action;
import com.openrsc.server.net.Packet;
import com.openrsc.server.net.rsc.PacketHandler;

public class ItemUseOnGroundItem implements PacketHandler {

	private GroundItem getItem(int id, Point location, Player player) {
		int x = location.getX();
		int y = location.getY();
		for (GroundItem i : player.getViewArea().getItemsInView()) {
			if (i.getID() == id && !i.isInvisibleTo(player) && i.getX() == x && i.getY() == y) {
				return i;
			}
		}
		return null;
	}

	public void handlePacket(Packet p, final Player player) throws Exception {
		if (player.isBusy()) {
			player.resetPath();
			return;
		}

		player.resetAll();
		Point location = Point.location(p.readShort(), p.readShort());
		final int id = p.readShort();
		final int groundItemId = p.readShort();
		if (player.getWorld().getServer().getConfig().WANT_EQUIPMENT_TAB && id > Inventory.MAX_SIZE) {
			player.message("Please unequip your item and try again.");
			return;
		}
		final Item myItem = player.getCarriedItems().getInventory().get(id);
		if (myItem == null)
			return;

		final GroundItem item = getItem(groundItemId, location, player);

		if (item == null || myItem == null) {
			player.setSuspiciousPlayer(true, "item use on ground item null item");
			player.resetPath();
			return;
		}
		player.setStatus(Action.USING_Item_ON_GITEM);
		player.setWalkToAction(new WalkToPointAction(player,
			item.getLocation(), 1) {
			public void executeInternal() {
				if (getPlayer().isBusy()
					|| getPlayer().isRanging()
					|| getItem(groundItemId, getLocation(), getPlayer()) == null
					|| !getPlayer().canReach(item)
					|| getPlayer().getStatus() != Action.USING_Item_ON_GITEM) {
					return;
				}
				if (myItem == null || item == null)
					return;

				if ((myItem.getDef(getPlayer().getWorld()).isMembersOnly() || item.getDef()
					.isMembersOnly())
					&& !getPlayer().getWorld().getServer().getConfig().MEMBER_WORLD) {
					getPlayer().message(getPlayer().MEMBER_MESSAGE);
					return;
				}

				getPlayer().getWorld().getServer().getPluginHandler().handlePlugin(getPlayer(), "InvUseOnGroundItem", new Object[]{myItem, item, getPlayer()}, this);
			}
		});

	}

}
