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
package forestry.core.tiles;

import java.io.IOException;

import Reika.RotaryCraft.API.Power.ShaftPowerReceiver;
import buildcraft.api.core.BCLog;
import net.minecraft.nbt.NBTTagCompound;

import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.common.Optional;

import forestry.api.core.IErrorLogic;
import forestry.core.circuits.ISpeedUpgradable;
import forestry.core.config.Config;
import forestry.core.errors.EnumErrorCode;
import forestry.core.network.DataInputStreamForestry;
import forestry.core.network.DataOutputStreamForestry;
import forestry.core.network.IStreamableGui;
import forestry.core.render.TankRenderInfo;

import buildcraft.api.tiles.IHasWork;

@Optional.Interface(iface = "buildcraft.api.tiles.IHasWork", modid = "BuildCraftAPI|tiles")
public abstract class TilePowered extends TileBase implements IRenderableTile, IHasWork, ISpeedUpgradable, IStreamableGui, ShaftPowerReceiver {
	private static final int WORK_TICK_INTERVAL = 5; // one Forestry work tick happens every WORK_TICK_INTERVAL game ticks

	//private final EnergyManager energyManager;

	private int workCounter;
	private int ticksPerWorkCycle;

	protected float speedMultiplier = 1.0f;
	protected float powerMultiplier = 1.0f;

	private boolean rotaryHasSpeedBoost = false;
	private int rotarySpeedBoostMaxTorque;
	private int rotarySpeedBoostMaxOmega;
	private long rotarySpeedBoostMaxPower;
	private float rotarySpeedBoostMultiplier;

	// the number of work ticks that this tile has had no power
	private int noPowerTime = 0;

	private String nameKey;

	protected TilePowered(String hintKey, int minTorque, int minOmega, int minPower) {
		super(hintKey);
		//this.energyManager = new EnergyManager(maxTransfer, capacity);
		//this.energyManager.setReceiveOnly();

		nameKey = hintKey;

		setMinTorque(minTorque);
		setMinOmega(minOmega);
		setMinPower(minPower);

		this.ticksPerWorkCycle = 4;

		hints.addAll(Config.hints.get("powered.machine"));
	}

	public int getWorkCounter() {
		return workCounter;
	}

	public void setTicksPerWorkCycle(int ticksPerWorkCycle) {
		this.ticksPerWorkCycle = ticksPerWorkCycle;
		this.workCounter = 0;
	}

	public void setPowerToSpeedBoost(int maxTorque, int maxOmega, int maxPower, float speedMultiplier)
	{
		rotaryHasSpeedBoost = true;
		rotarySpeedBoostMaxTorque = (maxTorque >= getMinTorque()) ? maxTorque : getMinTorque();
		rotarySpeedBoostMaxOmega = (maxOmega >= getMinOmega()) ? maxOmega : getMinOmega();
		rotarySpeedBoostMaxPower = (maxPower >= getMinPower()) ? maxPower : getMinPower();
		rotarySpeedBoostMultiplier = speedMultiplier;
	}

	public float getCurrentPowerToSpeedBoost()
	{
		float rotaryCurrentSpeedBoostMultiplier = 1F;
		if (rotaryHasSpeedBoost)
		{
			float torqueScaler = 1;
			if (rotarySpeedBoostMaxTorque > getMinTorque() && getTorque() > getMinTorque())
			{
				torqueScaler = (float)(getTorque() - getMinTorque()) / (float)(rotarySpeedBoostMaxTorque - getMinTorque());
			}
			float omegaScaler = 1;
			if (rotarySpeedBoostMaxOmega > getMinOmega() && getOmega() > getMinOmega())
			{
				omegaScaler = (float)(getOmega() - getMinOmega()) / (float)(rotarySpeedBoostMaxOmega - getMinOmega());
			}
			float powerScaler = 1;
			if (rotarySpeedBoostMaxPower > getMinPower() && getPower() > getMinPower())
			{
				powerScaler = (float)(getPower() - getMinPower()) / (float)(rotarySpeedBoostMaxPower - getMinPower());
			}
			rotaryCurrentSpeedBoostMultiplier = 1F + ((rotarySpeedBoostMultiplier - 1F) * Math.min(torqueScaler, Math.min(omegaScaler, powerScaler)));
			BCLog.logger.info("TilePowered torqueScaler: " + torqueScaler + ", omegaScaler: " + omegaScaler + ", powerScaler: " + powerScaler + ", min: " + Math.min(torqueScaler, Math.min(omegaScaler, powerScaler)) + ", current: " + rotaryCurrentSpeedBoostMultiplier);
		}
		return rotaryCurrentSpeedBoostMultiplier;
	}

	public int getTicksPerWorkCycle() {
		if (worldObj.isRemote) {
			return ticksPerWorkCycle;
		}
		int ticks = (int)Math.floor(ticksPerWorkCycle / getCurrentPowerToSpeedBoost());

		return Math.round(ticks / speedMultiplier);
	}

	/* STATE INFORMATION */
	public boolean hasResourcesMin(float percentage) {
		return false;
	}

	public boolean hasFuelMin(float percentage) {
		return false;
	}

	public abstract boolean hasWork();

	@Override
	protected void updateServerSide() {
		super.updateServerSide();

		if (!updateOnInterval(WORK_TICK_INTERVAL)) {
			return;
		}

		IErrorLogic errorLogic = getErrorLogic();


		// Disabled redstone disabling...
		/*boolean disabled = isRedstoneActivated();
		errorLogic.setCondition(disabled, EnumErrorCode.DISABLED_BY_REDSTONE);
		if (disabled) {
			return;
		}*/

		if (!hasWork()) {
			return;
		}

		//BCLog.logger.info("TilePowered Torque: " + rotaryTorque + " / " + getMinTorque(getTorque()) + ", Torque: " + rotaryOmega + " / " + getMinOmega(getOmega()) + ", Power: " + rotaryPower + " / " + getMinPower(getPower()));

		int ticksPerWorkCycle = getTicksPerWorkCycle();

		if (workCounter < ticksPerWorkCycle) {
			if (rotaryTorque >= getMinTorque(getTorque()) && rotaryOmega >= getMinOmega(getOmega()) && rotaryPower >= getMinPower(getPower())) {
				//BCLog.logger.info("TilePowered (CanWork) Omega: " + rotaryOmega + ",Torque: " + rotaryTorque + ", Power: " + rotaryPower);
				errorLogic.setCondition(false, EnumErrorCode.NO_POWER);
				workCounter++;
				noPowerTime = 0;
			} else {
				noPowerTime++;
				if (noPowerTime > 4) {
					errorLogic.setCondition(true, EnumErrorCode.NO_POWER);
				}
			}
		}

		if (workCounter >= ticksPerWorkCycle) {
			if (workCycle()) {
				workCounter = 0;
			}
		}
	}

	protected abstract boolean workCycle();

	public int getProgressScaled(int i) {
		int ticksPerWorkCycle = getTicksPerWorkCycle();
		if (ticksPerWorkCycle == 0) {
			return 0;
		}

		return (workCounter * i) / ticksPerWorkCycle;
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		//energyManager.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		//energyManager.readFromNBT(nbt);
	}

	@Override
	public void writeGuiData(DataOutputStreamForestry data) throws IOException {
		//energyManager.writeData(data);
		data.writeVarInt(workCounter);
		data.writeVarInt(getTicksPerWorkCycle());
	}

	@Override
	public void readGuiData(DataInputStreamForestry data) throws IOException {
		//energyManager.readData(data);
		workCounter = data.readVarInt();
		ticksPerWorkCycle = data.readVarInt();
	}

	/* ISpeedUpgradable */
	@Override
	public void applySpeedUpgrade(double speedChange, double powerChange) {
		speedMultiplier += speedChange;
		powerMultiplier += powerChange;
		workCounter = 0;
	}

	/* IRenderableTile */
	@Override
	public TankRenderInfo getResourceTankInfo() {
		return TankRenderInfo.EMPTY;
	}

	@Override
	public TankRenderInfo getProductTankInfo() {
		return TankRenderInfo.EMPTY;
	}

	/* IPowerHandler */
	/*@Override
	public EnergyManager getEnergyManager() {
		return energyManager;
	}

	@Override
	public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
		return energyManager.receiveEnergy(from, maxReceive, simulate);
	}

	@Override
	public int extractEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
		return energyManager.extractEnergy(from, maxReceive, simulate);
	}

	@Override
	public int getEnergyStored(ForgeDirection from) {
		return energyManager.getEnergyStored(from);
	}

	@Override
	public int getMaxEnergyStored(ForgeDirection from) {
		return energyManager.getMaxEnergyStored(from);
	}

	@Override
	public boolean canConnectEnergy(ForgeDirection from) {
		return energyManager.canConnectEnergy(from);
	}*/

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
		return hasWork();
	}

	public int getMinTorque() {
		return getMinTorque(getTorque());
	}

	@Override
	public int getMinTorque(int i) {
		return (rotaryMinTorque * (int)Math.ceil(powerMultiplier));
	}

	public int getMinOmega() {
		return getMinOmega(getOmega());
	}

	public int getMinOmega(int i) {
		return rotaryMinOmega;
	}

	public long getMinPower() {
		return getMinPower(getPower());
	}

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

	public void setMinTorque(int i) {
		if (i >= 1)
		{
			rotaryMinTorque = i;
		}
	}

	public void setMinOmega(int i) {
		if (i >= 1)
		{
			rotaryMinOmega = i;
		}
	}

	public void setMinPower(long l) {
		if (l >= 1)
		{
			rotaryMinPower = l;
		}
	}
}
