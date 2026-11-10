package dev.xkmc.l2serial.serialization.codec;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import dev.xkmc.l2serial.serialization.SerialClass;
import dev.xkmc.l2serial.serialization.type_cache.TypeInfo;
import dev.xkmc.l2serial.serialization.unified_processor.JsonContext;
import dev.xkmc.l2serial.serialization.unified_processor.TagContext;
import dev.xkmc.l2serial.serialization.unified_processor.UnifiedCodec;
import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Adapts a {@code @SerialClass} type to a {@link Codec}. Backport of the 1.21 class without
 * the registry-aware branches, which the 1.20.1 call sites do not need.
 */
public record CodecAdaptor<T>(Class<T> cls, UnaryOperator<T> validator) implements Codec<T> {

	public static final Logger LOGGER = LogManager.getLogger();

	public CodecAdaptor(Class<T> cls) {
		this(cls, e -> e);
	}

	@Override
	public <E> DataResult<Pair<T, E>> decode(DynamicOps<E> ops, E input) {
		if (ops.empty() instanceof JsonElement) {
			try {
				T val = Wrappers.cast(UnifiedCodec.deserializeValue(new JsonContext(),
						(JsonElement) input, TypeInfo.of(cls), null));
				return DataResult.success(Pair.of(validator.apply(val), input));
			} catch (Exception e) {
				LOGGER.throwing(Level.ERROR, e);
				return DataResult.error(e::getMessage);
			}
		}
		if (ops.empty() instanceof Tag) {
			try {
				T val = Wrappers.cast(UnifiedCodec.deserializeValue(new TagContext(e -> true),
						(Tag) input, TypeInfo.of(cls), null));
				return DataResult.success(Pair.of(validator.apply(val), input));
			} catch (Exception e) {
				LOGGER.throwing(Level.ERROR, e);
				return DataResult.error(e::getMessage);
			}
		}
		return DataResult.error(() -> "[CodecAdaptor::decode] Unsupported ops type " + ops.empty());
	}

	@Override
	public <E> DataResult<E> encode(T input, DynamicOps<E> ops, E prefix) {
		if (ops.empty() instanceof JsonElement) {
			try {
				var json = UnifiedCodec.serializeValue(new JsonContext(), TypeInfo.of(cls), input);
				if (!ops.empty().equals(prefix)) {
					if (json instanceof JsonObject a && prefix instanceof JsonObject b) {
						for (var e : b.entrySet())
							a.add(e.getKey(), e.getValue());
					} else {
						return DataResult.error(() -> "[CodecAdaptor::encode] Non-empty prefix");
					}
				}
				return DataResult.success(Wrappers.cast(json));
			} catch (Exception e) {
				LOGGER.throwing(Level.ERROR, e);
				return DataResult.error(e::getMessage);
			}
		}
		if (ops.empty() instanceof Tag) {
			try {
				var tag = UnifiedCodec.serializeValue(new TagContext(e -> true), TypeInfo.of(cls), input);
				if (!ops.empty().equals(prefix)) {
					if (tag instanceof CompoundTag a && prefix instanceof CompoundTag b) {
						for (var e : b.getAllKeys())
							a.put(e, Objects.requireNonNull(b.get(e)));
					} else {
						return DataResult.error(() -> "[CodecAdaptor::encode] Non-empty prefix");
					}
				}
				return DataResult.success(Wrappers.cast(tag));
			} catch (Exception e) {
				LOGGER.throwing(Level.ERROR, e);
				return DataResult.error(e::getMessage);
			}
		}
		return DataResult.error(() -> "[CodecAdaptor::encode] Unsupported ops type " + ops.empty());
	}

}
