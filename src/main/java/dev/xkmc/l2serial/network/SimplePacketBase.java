package dev.xkmc.l2serial.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public interface SimplePacketBase {

	void handle(IPayloadContext context);

}
