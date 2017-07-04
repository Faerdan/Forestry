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

import Reika.RotaryCraft.API.Power.IShaftPowerInputCaller;
import Reika.RotaryCraft.API.Power.ShaftPowerInputManager;
import buildcraft.api.core.BCLog;
import net.minecraft.nbt.NBTTagCompound;

import net.minecraft.tileentity.TileEntity;
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
public abstract class TilePowered extends TileBase implements IShaftPowerInputCaller, IRenderableTile, IHasWork, ISpeedUpgradable, IStreamableGui {
	private static final int WORK_TICK_INTERVAL = 5; // one Forestry work tick happens every WORK_TICK_INTERVAL game ticks

	private final ShaftPowerInputManager shaftPowerInputManager;

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

	protected TilePowered(String hintKey, int minTorque, int minOmega, int minPower) {
		super(hintKey);

		shaftPowerInputManager = new ShaftPowerInputManager(this, hintKey, minTorque, minOmega, minPower);

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
		rotarySpeedBoostMaxTorque = (maxTorque >= getMinTorque(0)) ? maxTorque : getMinTorque(0);
		rotarySpeedBoostMaxOmega = (maxOmega >= getMinOmega(0)) ? maxOmega : getMinOmega(0);
		rotarySpeedBoostMaxPower = (maxPower >= getMinPower(0)) ? maxPower : getMinPower(0);
		rotarySpeedBoostMultiplier = speedMultiplier;
	}

	public float getCurrentPowerToSpeedBoost()
	{
		float rotaryCurrentSpeedBoostMultiplier = 1F;
		if (rotaryHasSpeedBoost)
		{
			float torqueScaler = 1;
			if (rotarySpeedBoostMaxTorque > getMinTorque(0) && getTorque() > getMinTorque(0))
			{
				torqueScaler = (float)(getTorque() - getMinTorque(0)) / (float)(rotarySpeedBoostMaxTorque - getMinTorque(0));
			}
			float omegaScaler = 1;
			if (rotarySpeedBoostMaxOmega > getMinOmega(0) && getOmega() > getMinOmega(0))
			{
				omegaScaler = (float)(getOmega() - getMinOmega(0)) / (float)(rotarySpeedBoostMaxOmega - getMinOmega(0));
			}
			float powerScaler = 1;
			if (rotarySpeedBoostMaxPower > getMinPower(0) && getPower() > getMinPower(0))
			{
				powerScaler = (float)(getPower() - getMinPower(0)) / (float)(rotarySpeedBoostMaxPower - getMinPower(0));
			}
			rotaryCurrentSpeedBoostMultiplier = 1F + ((rotarySpeedBoostMultiplier - 1F) * Math.min(torqueScaler, Math.min(omegaScaler, powerScaler)));
			//BCLog.logger.info("TilePowered torqueScaler: " + torqueScaler + ", omegaScaler: " + omegaScaler + ", powerScaler: " + powerScaler + ", min: " + Math.min(torqueScaler, Math.min(omegaScaler, powerScaler)) + ", current: " + rotaryCurrentSpeedBoostMultiplier);
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
	protected void updateClientSide() {
		shaftPowerInputManager.update();
	}

	@Override
	protected void updateServerSide() {
		super.updateServerSide();

		shaftPowerInputManager.update();

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
			if (shaftPowerInputManager.isStagePowered(0)) {
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
	public void writeData(DataOutputStreamForestry stream) throws IOException {
		super.writeData(stream);

		stream.writeInt(shaftPowerInputManager.getTorque());
		stream.writeInt(shaftPowerInputManager.getOmega());
		stream.writeBoolean(shaftPowerInputManager.hasMismatchedInputs());

		for (int stageIndex = 0; stageIndex < getStageCount(); stageIndex++)
		{
			stream.writeInt(getMinTorque(stageIndex));
			stream.writeInt(getMinOmega(stageIndex));
			stream.writeLong(getMinPower(stageIndex));
		}
	}

	@Override
	public void readData(DataInputStreamForestry stream) throws IOException {
		super.readData(stream);

		shaftPowerInputManager.setState(stream.readInt(), stream.readInt(), stream.readBoolean());

		for (int stageIndex = 0; stageIndex < getStageCount(); stageIndex++)
		{
			shaftPowerInputManager.setMinTorque(stageIndex, stream.readInt());
			shaftPowerInputManager.setMinOmega(stageIndex, stream.readInt());
			shaftPowerInputManager.setMinPower(stageIndex, stream.readLong());
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
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


	/* Rotary Power */

	@Override
	public void onPowerChange(ShaftPowerInputManager shaftPowerInputManager) {
		this.setNeedsNetworkUpdate();
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
		return shaftPowerInputManager != null ? shaftPowerInputManager.getName() : "[Forestry]";
	}

	@Override
	public int getIORenderAlpha() {
		return shaftPowerInputManager != null ? shaftPowerInputManager.getIORenderAlpha() : 0;
	}
}
