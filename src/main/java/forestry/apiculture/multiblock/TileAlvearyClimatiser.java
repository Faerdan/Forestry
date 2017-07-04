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

import Reika.RotaryCraft.API.Power.IShaftPowerInputCaller;
import Reika.RotaryCraft.API.Power.ShaftPowerInputManager;
import buildcraft.api.core.BCLog;
import forestry.apiculture.network.packets.PacketShaftPowerUpdate;
import net.minecraft.nbt.NBTTagCompound;

import forestry.api.core.IClimateControlled;
import forestry.api.multiblock.IAlvearyComponent;
import forestry.apiculture.network.packets.PacketActiveUpdate;
import forestry.core.proxy.Proxies;
import forestry.core.tiles.IActivatable;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public abstract class TileAlvearyClimatiser extends TileAlveary implements IShaftPowerInputCaller, IActivatable, IAlvearyComponent.Climatiser {

	protected interface IClimatiserDefinition {
		float getChangePerTransfer();

		float getBoundaryUp();

		float getBoundaryDown();

		int getIconOff();

		int getIconOn();
	}

	private final IClimatiserDefinition definition;
	public final ShaftPowerInputManager shaftPowerInputManager;

	private int workingTime = 0;

	// CLIENT
	private boolean active;

	protected TileAlvearyClimatiser(IClimatiserDefinition definition, String nameKey, int minTorque) {
		this.definition = definition;

		shaftPowerInputManager = new ShaftPowerInputManager(this, nameKey, minTorque, 128, 1);
	}

	/* UPDATING */
	@Override
	public void changeClimate(int tick, IClimateControlled climateControlled) {
		shaftPowerInputManager.update();
		if (shaftPowerInputManager.isStagePowered(0)) {
			climateControlled.addTemperatureChange(definition.getChangePerTransfer(), definition.getBoundaryDown(), definition.getBoundaryUp());
			setActive(true);
		}
		else {
			setActive(false);
		}
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

	@Override
	public void onPowerChange(ShaftPowerInputManager shaftPowerInputManager) {
		BCLog.logger.info("alveary onPowerChange");
		if (worldObj != null && !worldObj.isRemote) {
			Proxies.net.sendNetworkPacket(new PacketShaftPowerUpdate(this, this), worldObj);
		}
		//this.setNeedsNetworkUpdate();
		//sendNetworkUpdate();
	}

	@Override
	public TileEntity getTileEntity() {
		return this;
	}

	@Override
	public boolean addPower(int addTorque, int addOmega, long addPower, ForgeDirection inputDirection) {
		return shaftPowerInputManager != null && shaftPowerInputManager.addPower(addTorque, addOmega, addPower, inputDirection);
	}

	@Override
	public int getStageCount() {
		return shaftPowerInputManager != null ? shaftPowerInputManager.getStageCount() : 0;
	}

	@Override
	public void setIORenderAlpha(int i) {
		if (shaftPowerInputManager != null) shaftPowerInputManager.setIORenderAlpha(i);
	}

	@Override
	public boolean canReadFrom(ForgeDirection forgeDirection) {
		return true;
	}

	@Override
	public boolean hasMismatchedInputs() {
		return shaftPowerInputManager != null && shaftPowerInputManager.hasMismatchedInputs();
	}

	@Override
	public boolean isReceiving() {
		return shaftPowerInputManager != null && shaftPowerInputManager.isReceiving();
	}

	@Override
	public int getMinTorque(int stageIndex) {
		return shaftPowerInputManager != null ? shaftPowerInputManager.getMinTorque(stageIndex) : 1;
	}

	@Override
	public int getMinOmega(int stageIndex) {
		return shaftPowerInputManager != null ? shaftPowerInputManager.getMinOmega(stageIndex) : 1;
	}

	@Override
	public long getMinPower(int stageIndex) {
		return shaftPowerInputManager != null ? shaftPowerInputManager.getMinPower(stageIndex) : 1;
	}

	@Override
	public long getPower() {
		return shaftPowerInputManager != null ? shaftPowerInputManager.getPower() : 0;
	}

	@Override
	public int getOmega() {
		return shaftPowerInputManager != null ? shaftPowerInputManager.getOmega() : 0;
	}

	@Override
	public int getTorque() {
		return shaftPowerInputManager != null ? shaftPowerInputManager.getTorque() : 0;
	}

	@Override
	public String getName() {
		return shaftPowerInputManager != null ? shaftPowerInputManager.getName() : "[ForestryAlveary]";
	}

	@Override
	public int getIORenderAlpha() {
		return shaftPowerInputManager != null ? shaftPowerInputManager.getIORenderAlpha() : 0;
	}

}
