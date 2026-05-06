package dev.xkmc.l2serial.serialization.generic_types;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record CodecReg<T>(Codec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> stream) {

}
