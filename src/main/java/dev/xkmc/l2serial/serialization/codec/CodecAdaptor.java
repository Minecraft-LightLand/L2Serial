package dev.xkmc.l2serial.serialization.codec;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import dev.xkmc.l2serial.serialization.type_cache.TypeInfo;
import dev.xkmc.l2serial.serialization.unified_processor.JsonContext;
import dev.xkmc.l2serial.serialization.unified_processor.TagContext;
import dev.xkmc.l2serial.serialization.unified_processor.TreeContext;
import dev.xkmc.l2serial.serialization.unified_processor.UnifiedCodec;
import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.UnaryOperator;

public record CodecAdaptor<T>(Class<T> cls, UnaryOperator<T> validator) implements Codec<T> {

	public static final Logger LOGGER = LogManager.getLogger();

	public CodecAdaptor(Class<T> cls) {
		this(cls, e -> e);
	}

	@Override
	public <E> DataResult<Pair<T, E>> decode(DynamicOps<E> ops, E input) {
		if (ops.empty() instanceof JsonElement) {
			DynamicOps<JsonElement> jops = Wrappers.cast(ops);
			try {
				T val = Wrappers.cast(UnifiedCodec.deserializeValue(new JsonContext(jops), (JsonElement) input, TypeInfo.of(cls), null));
				return DataResult.success(Pair.of(validator.apply(val), input));
			} catch (Exception e) {
				LOGGER.throwing(Level.ERROR, e);
				return DataResult.error(e::getMessage);
			}
		}
		if (ops.empty() instanceof Tag) {
			DynamicOps<Tag> jops = Wrappers.cast(ops);
			try {
				T val = Wrappers.cast(UnifiedCodec.deserializeValue(new TagContext(jops, e -> true), (Tag) input, TypeInfo.of(cls), null));
				return DataResult.success(Pair.of(validator.apply(val), input));
			} catch (Exception e) {
				LOGGER.throwing(Level.ERROR, e);
				return DataResult.error(e::getMessage);
			}
		}
		return DataResult.error(() -> "Unsupported ops type " + ops.getClass().getSimpleName() + " and value " + input.getClass().getSimpleName());
	}

	@Override
	public <E> DataResult<E> encode(T input, DynamicOps<E> ops, E prefix) {
		if (ops.empty() instanceof JsonElement) {
			RegistryOps<JsonElement> jops = Wrappers.cast(ops);
			try {
				var json = UnifiedCodec.serializeValue(new JsonContext(jops), TypeInfo.of(cls), input);
				if (!ops.empty().equals(prefix)) {
					if (json instanceof JsonObject a && prefix instanceof JsonObject b) {
						for (var e : b.entrySet()) {
							a.add(e.getKey(), e.getValue());
						}
					} else {
						return DataResult.error(() -> "Non-empty prefix for type " + ops.getClass().getSimpleName());
					}
					return DataResult.success(Wrappers.cast(json));
				}
			} catch (Exception e) {
				LOGGER.throwing(Level.ERROR, e);
				return DataResult.error(e::getMessage);
			}
		}
		if (ops.empty() instanceof Tag) {
			RegistryOps<Tag> tops = Wrappers.cast(ops);
			try {
				var tag = UnifiedCodec.serializeValue(new TagContext(tops, f -> true), TypeInfo.of(cls), input);
				if (!ops.empty().equals(prefix)) {
					if (tag instanceof CompoundTag a && prefix instanceof CompoundTag b) {
						for (var e : b.getAllKeys()) {
							a.put(e, Objects.requireNonNull(b.get(e)));
						}
					} else {
						return DataResult.error(() -> "Non-empty prefix for type " + ops.getClass().getSimpleName());
					}
					return DataResult.success(Wrappers.cast(tag));
				}
			} catch (Exception e) {
				LOGGER.throwing(Level.ERROR, e);
				return DataResult.error(e::getMessage);
			}
		}
		return DataResult.error(() -> "Unknown ops type " + ops.getClass().getSimpleName());
	}

	public <B extends RegistryFriendlyByteBuf> StreamCodec<B, T> toNetwork() {
		return StreamCodec.of(PacketCodec::to, (b) -> Objects.requireNonNull(PacketCodec.from(b, cls, null)));
	}

}
