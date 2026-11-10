package dev.xkmc.l2serial.network;

import dev.xkmc.l2serial.serialization.codec.PacketCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet base for {@code record} payloads. Records cannot extend {@link SerialPacketBase},
 * so this mirrors its wire and handling behaviour as an interface.
 */
public interface RecordPacketBase {

	default void write(FriendlyByteBuf buffer) {
		PacketCodec.to(buffer, this);
	}

	default void handle(Supplier<NetworkEvent.Context> context) {
		NetworkEvent.Context ctx = context.get();
		ctx.enqueueWork(() -> handle(getPlayer(ctx)));
		ctx.setPacketHandled(true);
	}

	void handle(Player player);

	private static Player getPlayer(NetworkEvent.Context ctx) {
		if (ctx.getDirection() == NetworkDirection.PLAY_TO_CLIENT && FMLEnvironment.dist == Dist.CLIENT)
			return Client.get();
		return ctx.getSender();
	}

	class Client {

		private static Player get() {
			return Minecraft.getInstance().player;
		}

	}

}
