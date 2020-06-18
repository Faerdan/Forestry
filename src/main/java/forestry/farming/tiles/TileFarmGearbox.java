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
package forestry.farming.tiles;

import Reika.RotaryCraft.API.Power.IShaftPowerInputCaller;
import Reika.RotaryCraft.API.Power.ShaftPowerInputManager;
import net.minecraft.nbt.NBTTagCompound;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import forestry.api.multiblock.IFarmComponent;
import forestry.api.multiblock.IFarmController;
import forestry.core.tiles.IPowerHandler;
import forestry.energy.EnergyManager;


public class TileFarmGearbox extends TileFarm implements IShaftPowerInputCaller, IFarmComponent.Active {

	private static final int WORK_CYCLES = 4;
	//private static final int ENERGY_PER_OPERATION = WORK_CYCLES * 50;

	private final ShaftPowerInputManager shaftPowerInputManager;

	private int activationDelay = 0;
	private int previousDelays = 0;
	private int workCounter;

	public TileFarmGearbox() {

		shaftPowerInputManager = new ShaftPowerInputManager(this, "Farm Gearbox", 128, 0, 16384);
	}

	/* SAVING & LOADING */
	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);

		activationDelay = nbttagcompound.getInteger("ActivationDelay");
		previousDelays = nbttagcompound.getInteger("PrevDelays");

		shaftPowerInputManager.setState(nbttagcompound.getInteger("torque"), nbttagcompound.getInteger("omega"));
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		super.writeToNBT(nbttagcompound);

		nbttagcompound.setInteger("ActivationDelay", activationDelay);
		nbttagcompound.setInteger("PrevDelays", previousDelays);

		nbttagcompound.setInteger("torque", shaftPowerInputManager.getTorque());
		nbttagcompound.setInteger("omega", shaftPowerInputManager.getOmega());
	}

	@Override
	public void updateServer(int tickCount) {
		shaftPowerInputManager.update();

		if (activationDelay > 0) {
			activationDelay--;
			return;
		}

		if (!shaftPowerInputManager.isStagePowered(0)) {
			return;
		}

		// Hard limit to 4 cycles / second.
		if (workCounter < WORK_CYCLES) {
			workCounter++;
		}

		if (workCounter >= WORK_CYCLES && (tickCount % 5 == 0)) {
			IFarmController farmController = getMultiblockLogic().getController();
			if (farmController.doWork()) {
				workCounter = 0;
				previousDelays = 0;
			} else {
				// If the central TE doesn't have work, we add to the activation delay to throttle the CPU usage.
				activationDelay = 10 * previousDelays < 120 ? 10 * previousDelays : 120;
				previousDelays++; // First delay is free!
			}
		}
	}

	public boolean isPowered()
	{
		return shaftPowerInputManager.isStagePowered(0);
	}

	@Override
	public void updateClient(int tickCount) {
		shaftPowerInputManager.update();
	}


	/* Rotary Power */

	@Override
	public void onPowerChange(ShaftPowerInputManager shaftPowerInputManager) {

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
