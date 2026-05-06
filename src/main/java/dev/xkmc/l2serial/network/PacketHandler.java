package dev.xkmc.l2serial.network;

import dev.xkmc.l2serial.serialization.codec.CodecAdaptor;
import dev.xkmc.l2serial.util.ModContainerHack;
import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("unused")
public class PacketHandler {

	public static final Logger LOGGER = LogManager.getLogger();

	public enum NetDir {
		PLAY_TO_CLIENT,
		PLAY_TO_SERVER;

		public <T extends SimplePacketBase> void register(PayloadRegistrar reg, PacketConfiguration<T> p) {
			switch (this) {
				case PLAY_TO_CLIENT -> reg.playToClient(p.id, p.wrapCodec(), p);
				case PLAY_TO_SERVER -> reg.playToServer(p.id, p.wrapCodec(), p);
			}
		}

	}

	public final String modid;
	public final int ver;
	public final String verStr;

	private final Function<PacketHandler, PacketConfiguration<?>>[] values;
	private final Map<Class<?>, PacketConfiguration<?>> map = new LinkedHashMap<>();

	/**
	 * Required to be registered manually
	 */
	@SafeVarargs
	public PacketHandler(String id, int version, Function<PacketHandler, PacketConfiguration<?>>... values) {
		modid = id;
		ver = version;
		verStr = String.valueOf(ver);
		this.values = values;
		var cont = ModContainerHack.getMod(id);
		var bus = cont.getEventBus();
		if (bus != null) bus.addListener(this::register);
		else throw new IllegalStateException("Event bus is null for " + id);
	}

	private Identifier of(Class<?> cls) {
		String name = cls.getSimpleName();
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < name.length(); i++) {
			char ch = name.charAt(i);
			if (ch >= 'a' && ch <= 'z' || ch >= '0' && ch <= '9') {
				builder.append(ch);
			} else if (ch >= 'A' && ch <= 'Z') {
				builder.append((char) (ch - 'A' + 'a'));
			}
		}
		return Identifier.fromNamespaceAndPath(this.modid, builder.toString());
	}

	private <T extends SimplePacketBase> PacketConfiguration<T> of(Class<T> cls, StreamCodec<RegistryFriendlyByteBuf, T> factory, NetDir dir) {
		return new PacketConfiguration<T>(new CustomPacketPayload.Type<>(of(cls)), cls, factory, dir);
	}

	public <T extends SimplePacketBase> PacketConfiguration<T> create(Class<T> type, StreamCodec<RegistryFriendlyByteBuf, T> factory, NetDir dir) {
		return of(type, factory, dir);
	}

	public <T extends Record & SerialPacketBase<T>> PacketConfiguration<T> create(Class<T> type, NetDir dir) {
		return of(type, new CodecAdaptor<>(type).toNetwork(), dir);
	}

	private <T extends SimplePacketBase> BasePayload<T> get(T val) {
		return new BasePayload<>(Wrappers.cast(map.get(val.getClass())), val);
	}

	public void toServer(SimplePacketBase packet) {
		ClientPacketDistributor.sendToServer(get(packet));//TODO
	}

	public void toTrackingPlayers(SimplePacketBase packet, Entity e) {
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(e, get(packet));
	}

	public void toTrackingOnly(SimplePacketBase packet, Entity e) {
		PacketDistributor.sendToPlayersTrackingEntity(e, get(packet));
	}

	public void toClientPlayer(SimplePacketBase packet, ServerPlayer e) {
		PacketDistributor.sendToPlayer(e, get(packet));
	}

	public void toAllClient(SimplePacketBase packet) {
		PacketDistributor.sendToAllPlayers(get(packet));
	}

	public void toTrackingChunk(ServerLevel sl, ChunkPos pos, SimplePacketBase packet) {
		PacketDistributor.sendToPlayersTrackingChunk(sl, pos, get(packet));
	}

	public void sendToNear(ServerLevel world, BlockPos pos, int range, SimplePacketBase packet) {
		PacketDistributor.sendToPlayersNear(world, null, pos.getX(), pos.getY(), pos.getZ(), range, get(packet));
	}

	private void register(RegisterPayloadHandlersEvent event) {
		var reg = event.registrar(modid).versioned(verStr).optional();
		for (var packet : values) {
			var config = packet.apply(this);
			config.register(reg);
			map.put(config.cls, config);
		}
	}

	public record PacketConfiguration<T extends SimplePacketBase>(
			CustomPacketPayload.Type<BasePayload<T>> id,
			Class<T> cls,
			StreamCodec<RegistryFriendlyByteBuf, T> codec,
			NetDir dir
	) implements IPayloadHandler<BasePayload<T>> {

		private void register(PayloadRegistrar reg) {
			dir.register(reg, this);
		}

		@Override
		public void handle(BasePayload<T> payload, IPayloadContext context) {
			payload.packet().handle(context);
		}

		public StreamCodec<RegistryFriendlyByteBuf, BasePayload<T>> wrapCodec() {
			return codec.map(t -> new BasePayload<>(this, t), p -> p.packet);
		}

	}

	public record BasePayload<T extends SimplePacketBase>(PacketConfiguration<T> config, T packet)
			implements CustomPacketPayload {

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return config.id;
		}

	}

}
