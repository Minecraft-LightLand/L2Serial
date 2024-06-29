package dev.xkmc.l2serial.serialization.custom_handler;

import com.mojang.serialization.Codec;
import dev.xkmc.l2serial.serialization.generic_types.CodecReg;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class CodecHandler<T> {

	public CodecHandler(Class<T> cls, Codec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
		Handlers.CODEC_MAP.put(cls, new CodecReg<>(codec, streamCodec));
	}

}
