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
package forestry.apiculture.network;

import forestry.apiculture.network.packets.*;
import forestry.core.network.PacketRegistry;

public class PacketRegistryApiculture extends PacketRegistry {
	@Override
	public void registerPackets() {
		registerClientPacket(new PacketImprintSelectionResponse());
		registerClientPacket(new PacketShaftPowerUpdate());
		registerClientPacket(new PacketActiveUpdate());
		registerClientPacket(new PacketBeeLogicActive());
		registerClientPacket(new PacketBeeLogicActiveEntity());
		registerClientPacket(new PacketHabitatBiomePointer());
		registerClientPacket(new PacketCandleUpdate());
		
		registerServerPacket(new PacketBeeLogicEntityRequest());
	}
}
