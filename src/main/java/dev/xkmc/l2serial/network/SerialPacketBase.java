package dev.xkmc.l2serial.network;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public interface SerialPacketBase<T extends Record & SerialPacketBase<T>> extends SimplePacketBase {

	@Override
	default void handle(IPayloadContext context) {
		context.enqueueWork(() -> handle(context.player()));
	}

	void handle(Player ctx);

}
