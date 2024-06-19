package dev.xkmc.l2serial.serialization.custom_handler;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;

public class CodecHandler<T> extends ClassHandler<Tag, T> {

	public static RegistryOps<JsonElement> json() {
		return RegistryOps.create(JsonOps.INSTANCE, Minecraft.getInstance().level.registryAccess());
	}

	public static RegistryOps<Tag> nbt() {
		return RegistryOps.create(NbtOps.INSTANCE, Minecraft.getInstance().level.registryAccess());
	}

	public CodecHandler(Class<T> cls, Codec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
		super(cls, e -> codec.encodeStart(json(), e).result().get(),
				e -> codec.decode(json(), e).result().get().getFirst(),
				streamCodec::decode,
				streamCodec::encode,
				e -> codec.decode(nbt(), e).result().get().getFirst(),
				e -> codec.encodeStart(nbt(), e).result().get());

	}

}
