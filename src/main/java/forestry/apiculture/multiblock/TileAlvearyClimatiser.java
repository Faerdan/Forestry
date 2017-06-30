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
package forestry.apiculture.multiblock;

import Reika.RotaryCraft.API.Power.IShaftPowerReceiver;
import net.minecraft.nbt.NBTTagCompound;

import net.minecraftforge.common.util.ForgeDirection;

import forestry.api.core.IClimateControlled;
import forestry.api.multiblock.IAlvearyComponent;
import forestry.apiculture.network.packets.PacketActiveUpdate;
import forestry.core.proxy.Proxies;
import forestry.core.tiles.IActivatable;

public abstract class TileAlvearyClimatiser extends TileAlveary implements IShaftPowerReceiver, IActivatable, IAlvearyComponent.Climatiser {

	protected interface IClimitiserDefinition {
		float getChangePerTransfer();

		float getBoundaryUp();

		float getBoundaryDown();

		int getIconOff();

		int getIconOn();
	}

	private final IClimitiserDefinition definition;

	private int workingTime = 0;

	private String nameKey;

	// CLIENT
	private boolean active;

	protected TileAlvearyClimatiser(IClimitiserDefinition definition, String nameKey, int minTorque) {
		this.definition = definition;
		this.nameKey = nameKey;

		setMinTorque(minTorque);
		setMinOmega(128);
	}

	/* UPDATING */
	@Override
	public void changeClimate(int tick, IClimateControlled climateControlled) {
		if (getTorque() >= getMinTorque() && getOmega() >= getMinOmega() && getPower() >= getMinPower()) {
			climateControlled.addTemperatureChange(definition.getChangePerTransfer(), definition.getBoundaryDown(), definition.getBoundaryUp());
			setActive(true);
		}
		else {
			setActive(false);
		}
		noInputMachine();
	}

	/* TEXTURES */
	@Override
	public int getIcon(int side) {
		if (active) {
			return definition.getIconOn();
		} else {
			return definition.getIconOff();
		}
	}

	/* LOADING & SAVING */
	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);
		workingTime = nbttagcompound.getInteger("Heating");
		setActive(workingTime > 0);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		super.writeToNBT(nbttagcompound);
		nbttagcompound.setInteger("Heating", workingTime);
	}

	/* Network */
	@Override
	protected void encodeDescriptionPacket(NBTTagCompound packetData) {
		super.encodeDescriptionPacket(packetData);
		packetData.setBoolean("Active", active);
	}

	@Override
	protected void decodeDescriptionPacket(NBTTagCompound packetData) {
		super.decodeDescriptionPacket(packetData);
		setActive(packetData.getBoolean("Active"));
	}

	/* IActivatable */
	@Override
	public boolean isActive() {
		return active;
	}

	@Override
	public void setActive(boolean active) {
		if (this.active == active) {
			return;
		}

		this.active = active;

		if (worldObj != null) {
			if (worldObj.isRemote) {
				worldObj.func_147479_m(xCoord, yCoord, zCoord);
			} else {
				Proxies.net.sendNetworkPacket(new PacketActiveUpdate(this), worldObj);
			}
		}
	}

	/* Rotary Power */
	private int rotaryMinTorque = 1;
	private int rotaryMinOmega = 1;
	private long rotaryMinPower = 1;

	private int rotaryOmega;
	private int rotaryTorque;
	private long rotaryPower;
	private int rotaryIORenderAlpha;

	@Override
	public void setOmega(int i) {
		//BCLog.logger.info("Quarry setOmega: " + i);
		rotaryOmega = i;
	}

	@Override
	public void setTorque(int i) {
		//BCLog.logger.info("Quarry setTorque: " + i);
		rotaryTorque = i;
	}

	@Override
	public void setPower(long l) {
		//BCLog.logger.info("Quarry setPower: " + l);
		rotaryPower = l;
	}

	@Override
	public void noInputMachine() {
		rotaryOmega = 0;
		rotaryTorque = 0;
		rotaryPower = 0;
	}

	@Override
	public boolean canReadFrom(ForgeDirection forgeDirection) {
		return true; // (forgeDirection == ForgeDirection.EAST || forgeDirection == ForgeDirection.WEST || forgeDirection == ForgeDirection.NORTH);
	}

	@Override
	public boolean isReceiving() {
		return true;
	}

	@Override
	public int getMinTorque() {
		return getMinTorque(getTorque());
	}

	@Override
	public int getMinTorque(int i) {
		return rotaryMinTorque;
	}

	@Override
	public int getMinOmega() {
		return getMinOmega(getOmega());
	}

	@Override
	public int getMinOmega(int i) {
		return rotaryMinOmega;
	}

	@Override
	public long getMinPower() {
		return getMinPower(getPower());
	}

	@Override
	public long getMinPower(long l) {
		return rotaryMinPower;
	}

	@Override
	public int getOmega() {
		return rotaryOmega;
	}

	@Override
	public int getTorque() {
		return rotaryTorque;
	}

	@Override
	public long getPower() {
		return rotaryPower;
	}

	@Override
	public String getName() {
		return nameKey;
	}

	@Override
	public int getIORenderAlpha() {
		return rotaryIORenderAlpha;
	}

	@Override
	public void setIORenderAlpha(int i) {
		rotaryIORenderAlpha = i;
	}

	@Override
	public void setMinTorque(int i) {
		if (i >= 1)
		{
			rotaryMinTorque = i;
		}
	}

	@Override
	public void setMinOmega(int i) {
		if (i >= 1)
		{
			rotaryMinOmega = i;
		}
	}

	@Override
	public void setMinPower(long l) {
		if (l >= 1)
		{
			rotaryMinPower = l;
		}
	}

}
