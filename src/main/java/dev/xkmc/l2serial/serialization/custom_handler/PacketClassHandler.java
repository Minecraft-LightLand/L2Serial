package dev.xkmc.l2serial.serialization.custom_handler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;

public interface PacketClassHandler<T> {

	void toPacket(RegistryFriendlyByteBuf buf, Object obj);

	T fromPacket(RegistryFriendlyByteBuf buf);
}
