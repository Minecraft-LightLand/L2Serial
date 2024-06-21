package dev.xkmc.l2serial.serialization.codec;

import dev.xkmc.l2serial.serialization.marker.SerialField;
import dev.xkmc.l2serial.serialization.type_cache.TypeInfo;
import dev.xkmc.l2serial.serialization.unified_processor.PacketContext;
import dev.xkmc.l2serial.serialization.unified_processor.UnifiedCodec;
import dev.xkmc.l2serial.util.Wrappers;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.connection.ConnectionType;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public class PacketCodec {

	public static RegistryFriendlyByteBuf unit(RegistryAccess access) {
		return new RegistryFriendlyByteBuf(Unpooled.buffer(), access, ConnectionType.NEOFORGE);
	}

	public static RegistryFriendlyByteBuf decode(RegistryAccess access, byte[] arr) {
		return new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(arr), access, ConnectionType.NEOFORGE);
	}

	public static byte[] encode(FriendlyByteBuf buf) {
		byte[] arr = new byte[buf.writerIndex()];
		buf.getBytes(0, arr);
		return arr;
	}


	@Nullable
	@SuppressWarnings("unchecked")
	public static <T> T fromBytes(RegistryAccess access, byte[] arr, Class<T> cls, @Nullable T ans, Predicate<SerialField> pred) {
		var buf = decode(access, arr);
		return Wrappers.get(() -> (T) UnifiedCodec.deserializeValue(new PacketContext(buf), buf, TypeInfo.of(cls), ans));
	}

	public static <T extends R, R> byte[] toBytes(RegistryAccess access, T obj, Class<R> r, Predicate<SerialField> pred) {
		var buf = unit(access);
		Wrappers.run(() -> UnifiedCodec.serializeValue(new PacketContext(buf, pred), TypeInfo.of(r), obj));
		return encode(buf);
	}


	/**
	 * Deserialize an object from given data stream.
	 *
	 * @param buf The input data buffer to read from
	 * @param cls The deserialization type information
	 * @param ans The object to inject into. Constructs a new one if it's <code>null</code>
	 * @return The deserialized object. It will be the same as <code>ans</code> if it's a <code>@SerialClass</code> object.
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public static <T> T from(RegistryFriendlyByteBuf buf, Class<T> cls, @Nullable T ans) {
		return Wrappers.get(() -> (T) UnifiedCodec.deserializeValue(new PacketContext(buf), buf, TypeInfo.of(cls), ans));
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public static <T> T from(RegistryFriendlyByteBuf buf, Class<T> cls, @Nullable T ans, Predicate<SerialField> pred) {
		return Wrappers.get(() -> (T) UnifiedCodec.deserializeValue(new PacketContext(buf, pred), buf, TypeInfo.of(cls), ans));
	}

	/**
	 * Serialize an object to data stream
	 *
	 * @param buf The output data buffer to write to
	 * @param obj The object to serialize
	 */
	public static <T> void to(RegistryFriendlyByteBuf buf, T obj) {
		PacketCodec.to(buf, obj, Wrappers.cast(obj.getClass()));
	}

	/**
	 * Serialize an object to data stream
	 *
	 * @param buf The output data buffer to write to
	 * @param obj The object to serialize
	 * @param r   The serialization type information
	 */
	public static <T extends R, R> void to(RegistryFriendlyByteBuf buf, T obj, Class<R> r) {
		Wrappers.run(() -> UnifiedCodec.serializeValue(new PacketContext(buf), TypeInfo.of(r), obj));
	}


	public static <T extends R, R> void to(RegistryFriendlyByteBuf buf, T obj, Class<R> r, Predicate<SerialField> pred) {
		Wrappers.run(() -> UnifiedCodec.serializeValue(new PacketContext(buf, pred), TypeInfo.of(r), obj));
	}

}
