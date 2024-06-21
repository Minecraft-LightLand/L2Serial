package dev.xkmc.l2serial.serialization.custom_handler;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.xkmc.l2serial.serialization.generic_types.CodecReg;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class CodecHandler<T> {

	public CodecHandler(Class<T> cls, Codec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
		Handlers.CODEC_MAP.put(cls, new CodecReg<>(codec, streamCodec));
	}

}
