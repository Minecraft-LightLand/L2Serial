package dev.xkmc.l2serial.serialization.generic_types;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record HolderCodecReg<T>(Codec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> stream) {

}
