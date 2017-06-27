/*******************************************************************************
 * Copyright (c) 2011-2014 SirSengir.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Various Contributors including, but not limited to:
 * SirSengir (original work), CovertJaguar, Player, Binnie, MysteriousAges
 ******************************************************************************/
package forestry.core.gui.ledgers;

import forestry.core.render.TextureManager;
import forestry.core.tiles.TilePowered;
import forestry.core.utils.StringUtil;

public class PowerLedger extends Ledger {

	private final TilePowered tile;

	public PowerLedger(LedgerManager manager, TilePowered tile) {
		super(manager, "power");
		this.tile = tile;
		maxHeight = 94;
	}

	@Override
	public void draw(int x, int y) {
		// Draw background
		drawBackground(x, y);

		// Draw icon
		drawIcon(TextureManager.getInstance().getDefault("misc/energy"), x + 3, y + 4);

		if (!isFullyOpened()) {
			return;
		}

		int xHeader = x + 22;
		int xBody = x + 12;

		drawHeader(StringUtil.localize("gui.energy"), xHeader, y + 8);

		drawSubheader(StringUtil.localize("gui.power") + (this.tile.getMinPower() > 1 ? " (Min " + this.tile.getMinPower() + "):" : ":"), xBody, y + 20);
		drawText(this.tile.getPower() + " kW", xBody, y + 32);

		drawSubheader(StringUtil.localize("gui.torque") + (this.tile.getMinTorque() > 1 ? " (Min " + this.tile.getMinTorque() + "):" : ":"), xBody, y + 44);
		drawText(this.tile.getTorque() + " Nm", xBody, y + 56);

		drawSubheader(StringUtil.localize("gui.speed") + (this.tile.getMinOmega() > 1 ? " (Min " + this.tile.getMinOmega() + "):" : ":"), xBody, y + 68);
		drawText(this.tile.getOmega() + " Rad/s", xBody, y + 80);
	}

	@Override
	public String getTooltip() {
		return this.tile.getPower() + " kW";
	}

}
