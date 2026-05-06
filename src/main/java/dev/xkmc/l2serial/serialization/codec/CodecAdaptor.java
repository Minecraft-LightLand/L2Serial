package dev.xkmc.l2serial.serialization.codec;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import dev.xkmc.l2serial.serialization.type_cache.TypeInfo;
import dev.xkmc.l2serial.serialization.unified_processor.JsonContext;
import dev.xkmc.l2serial.serialization.unified_processor.TagContext;
import dev.xkmc.l2serial.serialization.unified_processor.UnifiedCodec;
import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.Unit;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
		if (ops instanceof RegistryOps<E>) {
			var tag = TagParser.FLATTENED_CODEC.decode(ops, input).getOrThrow().getFirst();
			var ti = tag.get("value");
			DynamicOps<Tag> jops = ((RegistryOps<E>) ops).withParent(NbtOps.INSTANCE);
			try {
				T val = Wrappers.cast(UnifiedCodec.deserializeValue(new TagContext(jops, e -> true), ti, TypeInfo.of(cls), null));
				return DataResult.success(Pair.of(validator.apply(val), input));
			} catch (Exception e) {
				LOGGER.throwing(Level.ERROR, e);
				return DataResult.error(e::getMessage);
			}
		}
		return DataResult.error(() -> "[CodecAdaptor::decode] Unsupported ops type " + ops.empty() + " and value " + input.getClass().getSimpleName());
	}

	@Override
	public <E> DataResult<E> encode(T input, DynamicOps<E> ops, E prefix) {
		if (ops.empty() == Unit.INSTANCE) {
			return DataResult.success(Wrappers.cast(Unit.INSTANCE));
		}
		if (ops.empty() == JsonNull.INSTANCE) {
			RegistryOps<JsonElement> jops = Wrappers.cast(ops);
			try {
				var json = UnifiedCodec.serializeValue(new JsonContext(jops), TypeInfo.of(cls), input);
				if (!ops.empty().equals(prefix)) {
					if (json instanceof JsonObject a && prefix instanceof JsonObject b) {
						for (var e : b.entrySet()) {
							a.add(e.getKey(), e.getValue());
						}
					} else {
						return DataResult.error(() -> "[CodecAdaptor::encode] Non-empty prefix for type " + ops.empty());
					}
				}
				return DataResult.success(Wrappers.cast(json));
			} catch (Exception e) {
				LOGGER.throwing(Level.ERROR, e);
				return DataResult.error(e::getMessage);
			}
		}
		if (ops.empty() == EndTag.INSTANCE) {
			RegistryOps<Tag> tops = Wrappers.cast(ops);
			try {
				var tag = UnifiedCodec.serializeValue(new TagContext(tops, f -> true), TypeInfo.of(cls), input);
				if (!ops.empty().equals(prefix)) {
					if (tag instanceof CompoundTag a && prefix instanceof CompoundTag b) {
						for (var e : b.keySet()) {
							a.put(e, Objects.requireNonNull(b.get(e)));
						}
					} else {
						return DataResult.error(() -> "[CodecAdaptor::encode] Non-empty prefix for type " + ops.empty());
					}
				}
				return DataResult.success(Wrappers.cast(tag));
			} catch (Exception e) {
				LOGGER.throwing(Level.ERROR, e);
				return DataResult.error(e::getMessage);
			}
		}
		if (ops instanceof RegistryOps<E> rops) {
			RegistryOps<Tag> tops = rops.withParent(NbtOps.INSTANCE);
			try {
				var tag = UnifiedCodec.serializeValue(new TagContext(tops, f -> true), TypeInfo.of(cls), input);
				if (!ops.empty().equals(prefix)) {
					if (tag instanceof CompoundTag a && prefix instanceof CompoundTag b) {
						for (var e : b.keySet()) {
							a.put(e, Objects.requireNonNull(b.get(e)));
						}
					} else {
						return DataResult.error(() -> "[CodecAdaptor::encode] Non-empty prefix for type " + ops.empty());
					}
				}
				CompoundTag ans = new CompoundTag();
				ans.put("value", tag);
				return TagParser.FLATTENED_CODEC.encodeStart(ops, ans);
			} catch (Exception e) {
				LOGGER.throwing(Level.ERROR, e);
				return DataResult.error(e::getMessage);
			}
		}
		String str = "[CodecAdaptor::encode] Unknown ops type with empty ops: " + ops.empty();
		LOGGER.throwing(Level.ERROR, new IllegalStateException(str));
		return DataResult.error(() -> str);
	}

	public <B extends RegistryFriendlyByteBuf> StreamCodec<B, T> toNetwork() {
		return StreamCodec.of(PacketCodec::to, (b) -> Objects.requireNonNull(PacketCodec.from(b, cls, null)));
	}

}
