package dev.xkmc.l2serial.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class BasePacketHandler {

	public final ResourceLocation CHANNEL_NAME;
	public final int NETWORK_VERSION;
	public final String NETWORK_VERSION_STR;
	public SimpleChannel channel;

	private final Function<BasePacketHandler, LoadedPacket<?>>[] values;

	private int index = 0;

	/** Required to be registered manually */
	@SafeVarargs
	public BasePacketHandler(ResourceLocation id, int version, Function<BasePacketHandler, LoadedPacket<?>>... values) {
		CHANNEL_NAME = id;
		NETWORK_VERSION = version;
		NETWORK_VERSION_STR = String.valueOf(NETWORK_VERSION);
		this.values = values;
	}

	public <T extends SimplePacketBase> LoadedPacket<T> create(Class<T> type, Function<FriendlyByteBuf, T> factory,
															   NetworkDirection direction) {
		return new LoadedPacket<>(type, factory, direction);
	}

	@SuppressWarnings("unchecked")
	public <T extends SerialPacketBase> LoadedPacket<T> create(Class<T> type, NetworkDirection direction) {
		var base = (Function<FriendlyByteBuf, T>) (Function<FriendlyByteBuf, SerialPacketBase>)
				buf -> SerialPacketBase.serial(type, buf);
		Function<FriendlyByteBuf, T> decoder = base;
		if (direction == NetworkDirection.PLAY_TO_CLIENT)
			decoder = buf -> {
				try {
					return base.apply(buf);
				} finally {
					buf.release();
				}
			};
		return new LoadedPacket<>(type, decoder, direction);
	}


	public void toServer(SimplePacketBase packet) {
		channel.sendToServer(packet);
	}

	public void toTrackingPlayers(SimplePacketBase packet, Entity e) {
		channel.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> e), packet);
	}

	public void toClientPlayer(SimplePacketBase packet, ServerPlayer e) {
		channel.sendTo(packet, e.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
	}

	public void toAllClient(SimplePacketBase packet) {
		channel.send(PacketDistributor.ALL.noArg(), packet);
	}


	public void sendToNear(Level world, BlockPos pos, int range, Object message) {
		channel.send(PacketDistributor.NEAR
				.with(PacketDistributor.TargetPoint.p(pos.getX(), pos.getY(), pos.getZ(), range, world.dimension())), message);
	}

	/** Call this in common setup event */
	public void registerPackets() {
		channel = NetworkRegistry.ChannelBuilder.named(CHANNEL_NAME)
				.serverAcceptedVersions(NETWORK_VERSION_STR::equals)
				.clientAcceptedVersions(NETWORK_VERSION_STR::equals)
				.networkProtocolVersion(() -> NETWORK_VERSION_STR)
				.simpleChannel();
		for (Function<BasePacketHandler, LoadedPacket<?>> packet : values)
			packet.apply(this).register();
	}

	public class LoadedPacket<T extends SimplePacketBase> {

		private final BiConsumer<T, FriendlyByteBuf> encoder;
		private final Function<FriendlyByteBuf, T> decoder;
		private final BiConsumer<T, Supplier<NetworkEvent.Context>> handler;
		private final Class<T> type;
		private final NetworkDirection direction;

		private LoadedPacket(Class<T> type, Function<FriendlyByteBuf, T> factory, NetworkDirection direction) {
			encoder = T::write;
			decoder = factory;
			handler = T::handle;
			this.type = type;
			this.direction = direction;
		}

		private void register() {
			channel.messageBuilder(type, index++, direction)
					.encoder(encoder)
					.decoder(decoder)
					.consumerMainThread(handler)
					.add();
		}
	}

}
