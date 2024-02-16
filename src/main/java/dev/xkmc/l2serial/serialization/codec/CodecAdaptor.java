package dev.xkmc.l2serial.serialization.codec;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.UnaryOperator;

public record CodecAdaptor<T>(Class<T> cls, UnaryOperator<T> validator) implements Codec<T> {

	public CodecAdaptor(Class<T> cls) {
		this(cls, e -> e);
	}

	@Override
	public <E> DataResult<Pair<T, E>> decode(DynamicOps<E> ops, E input) {
		if (ops instanceof JsonOps && input instanceof JsonElement elem) {
			T val = JsonCodec.from(elem, cls, null);
			return DataResult.success(Pair.of(validator.apply(val), input));
		}
		if (ops instanceof NbtOps && input instanceof Tag tag) {
			T val = TagCodec.valueFromTag(tag, cls);
			return DataResult.success(Pair.of(validator.apply(val), input));
		}
		return DataResult.error(() -> "Unknown ops type " + ops.getClass().getSimpleName() + " and value " + input.getClass().getSimpleName());
	}

	@Override
	public <E> DataResult<E> encode(T input, DynamicOps<E> ops, E prefix) {
		if (!ops.empty().equals(prefix)) {
			return DataResult.error(() -> "Non-empty prefix for type " + ops.getClass().getSimpleName());
		}
		if (ops instanceof JsonOps) {
			var json = JsonCodec.toJson(input, cls);
			assert json != null;
			return DataResult.success(Wrappers.cast(json));
		}
		if (ops instanceof NbtOps) {
			var tag = TagCodec.valueToTag(cls, input, e -> true);
			assert tag != null;
			return DataResult.success(Wrappers.cast(tag));
		}
		return DataResult.error(() -> "Unknown ops type " + ops.getClass().getSimpleName());
	}

}
