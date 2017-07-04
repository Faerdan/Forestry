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
package forestry.apiculture.network.packets;

import Reika.RotaryCraft.API.Power.IShaftPowerInputCaller;
import buildcraft.api.core.BCLog;
import forestry.core.network.DataInputStreamForestry;
import forestry.core.network.DataOutputStreamForestry;
import forestry.core.network.IForestryPacketClient;
import forestry.core.network.PacketIdClient;
import forestry.core.network.packets.PacketCoordinates;
import forestry.core.proxy.Proxies;
import forestry.core.tiles.IActivatable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

import java.io.IOException;

public class PacketShaftPowerUpdate extends PacketCoordinates implements IForestryPacketClient {

	private int torque;
	private int omega;

	public PacketShaftPowerUpdate() {
	}

	public PacketShaftPowerUpdate(IActivatable tile, IShaftPowerInputCaller caller) {
		super(tile.getCoordinates());
		BCLog.logger.info("alveary do PacketShaftPowerUpdate");
		this.torque = caller.getTorque();
		this.omega = caller.getOmega();
	}

	@Override
	public PacketIdClient getPacketId() {
		return PacketIdClient.TILE_FORESTRY_ACTIVE;
	}

	@Override
	protected void writeData(DataOutputStreamForestry data) throws IOException {
		super.writeData(data);
		BCLog.logger.info("Write packet for alveary power t: " + torque + ", o: " + omega);
		data.writeInt(torque);
		data.writeInt(omega);
	}

	@Override
	public void readData(DataInputStreamForestry data) throws IOException {
		super.readData(data);
		BCLog.logger.info("Read packet for alveary power t: " + torque + ", o: " + omega);
		torque = data.readInt();
		omega = data.readInt();
	}

	@Override
	public void onPacketData(DataInputStreamForestry data, EntityPlayer player) {
		TileEntity tile = getTarget(Proxies.common.getRenderWorld());
		BCLog.logger.info("Open packet for alveary power t: " + torque + ", o: " + omega);
		if (tile instanceof IShaftPowerInputCaller) {
			BCLog.logger.info("Open packet tile found for alveary power t: " + torque + ", o: " + omega);
			((IShaftPowerInputCaller) tile).addPower(torque, omega, (long)torque * (long)omega, null);
		}
	}
}
