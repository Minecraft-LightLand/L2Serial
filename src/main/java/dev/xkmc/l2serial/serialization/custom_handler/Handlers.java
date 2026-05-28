package dev.xkmc.l2serial.serialization.custom_handler;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.l2serial.serialization.generic_types.*;
import dev.xkmc.l2serial.serialization.nulldefer.NullDefer;
import dev.xkmc.l2serial.serialization.nulldefer.PrimitiveNullDefer;
import dev.xkmc.l2serial.serialization.nulldefer.SimpleNullDefer;
import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.registries.RegistryManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Handlers {

	public static final Map<Class<?>, JsonClassHandler<?>> JSON_MAP = new ConcurrentHashMap<>();
	public static final Map<Class<?>, NBTClassHandler<?, ?>> NBT_MAP = new ConcurrentHashMap<>();
	public static final Map<Class<?>, PacketClassHandler<?>> PACKET_MAP = new ConcurrentHashMap<>();
	public static final Map<Class<?>, CodecReg<?>> CODEC_MAP = new ConcurrentHashMap<>();

	private static final Map<Class<?>, HolderReg<?>> REGMAP = new ConcurrentHashMap<>();

	public static final List<GenericCodec> LIST = new ArrayList<>();
	public static final Map<Class<?>, NullDefer<?>> NULL_DEFER = new ConcurrentHashMap<>();

	// register handlers
	static {
		// primitives

		new ClassHandler<>(long.class, JsonPrimitive::new, JsonElement::getAsLong, FriendlyByteBuf::readLong, FriendlyByteBuf::writeLong, LongTag::value, LongTag::valueOf, Long.class);
		new ClassHandler<>(int.class, JsonPrimitive::new, JsonElement::getAsInt, FriendlyByteBuf::readInt, FriendlyByteBuf::writeInt, IntTag::value, IntTag::valueOf, Integer.class);
		new ClassHandler<ShortTag, Short>(short.class, JsonPrimitive::new, JsonElement::getAsShort, FriendlyByteBuf::readShort, FriendlyByteBuf::writeShort, ShortTag::value, ShortTag::valueOf, Short.class);
		new ClassHandler<>(byte.class, JsonPrimitive::new, JsonElement::getAsByte, FriendlyByteBuf::readByte, FriendlyByteBuf::writeByte, ByteTag::value, ByteTag::valueOf, Byte.class);
		new ClassHandler<ByteTag, Boolean>(boolean.class, JsonPrimitive::new, JsonElement::getAsBoolean, FriendlyByteBuf::readBoolean, FriendlyByteBuf::writeBoolean, tag -> tag.value() != 0, ByteTag::valueOf, Boolean.class);
		new ClassHandler<ByteTag, Character>(char.class, JsonPrimitive::new, JsonElement::getAsCharacter, FriendlyByteBuf::readChar, FriendlyByteBuf::writeChar, t -> (char) t.value(), c -> ByteTag.valueOf((byte) (char) c), Character.class);
		new ClassHandler<>(double.class, JsonPrimitive::new, JsonElement::getAsDouble, FriendlyByteBuf::readDouble, FriendlyByteBuf::writeDouble, DoubleTag::value, DoubleTag::valueOf, Double.class);
		new ClassHandler<>(float.class, JsonPrimitive::new, JsonElement::getAsFloat, FriendlyByteBuf::readFloat, FriendlyByteBuf::writeFloat, FloatTag::value, FloatTag::valueOf, Float.class);

		new ClassHandler<>(String.class, JsonPrimitive::new, JsonElement::getAsString, FriendlyByteBuf::readUtf, FriendlyByteBuf::writeUtf, StringTag::value, StringTag::valueOf);

		// minecraft
		new StringClassHandler<>(Identifier.class, Identifier::parse, Identifier::toString);
		new StringClassHandler<>(UUID.class, UUID::fromString, UUID::toString);

		new CodecHandler<>(ItemStack.class, ItemStack.OPTIONAL_CODEC, ItemStack.OPTIONAL_STREAM_CODEC);
		new CodecHandler<>(ItemStackTemplate.class, ItemStackTemplate.CODEC, ItemStackTemplate.STREAM_CODEC);
		new CodecHandler<>(FluidStack.class, FluidStack.OPTIONAL_CODEC, FluidStack.OPTIONAL_STREAM_CODEC);
		new CodecHandler<>(FluidStackTemplate.class, FluidStackTemplate.CODEC, FluidStackTemplate.STREAM_CODEC);
		new CodecHandler<>(Ingredient.class, Ingredient.CODEC, Ingredient.CONTENTS_STREAM_CODEC);
		new CodecHandler<>(MobEffectInstance.class, MobEffectInstance.CODEC, MobEffectInstance.STREAM_CODEC);
		new CodecHandler<>(Component.class, ComponentSerialization.CODEC, ComponentSerialization.STREAM_CODEC);

		// no JSON
		new ClassHandler<CompoundTag, CompoundTag>(CompoundTag.class, null, null, f -> f.readNbt(), (f, b) -> f.writeNbt(b), e -> e, e -> e);
		new ClassHandler<ListTag, ListTag>(ListTag.class, null, null, buf -> (ListTag) buf.readNbt().get("warp"),
				(buf, tag) -> buf.writeNbt(Util.make(new CompoundTag(), e -> e.put("wrap", tag))), e -> e, e -> e);

		new ClassHandler<>(long[].class, null, null, buf -> buf.readLongArray(), (buf, arr) -> buf.writeLongArray(arr), LongArrayTag::getAsLongArray, LongArrayTag::new);
		new ClassHandler<>(int[].class, null, null, FriendlyByteBuf::readVarIntArray, FriendlyByteBuf::writeVarIntArray, IntArrayTag::getAsIntArray, IntArrayTag::new);
		new ClassHandler<>(byte[].class, null, null, f -> f.readByteArray(), (f, b) -> f.writeByteArray(b), ByteArrayTag::getAsByteArray, ByteArrayTag::new);

		var legacy = RecordCodecBuilder.<BlockPos>create(i -> i.group(
				Codec.INT.fieldOf("x").forGetter(Vec3i::getX),
				Codec.INT.fieldOf("y").forGetter(Vec3i::getY),
				Codec.INT.fieldOf("z").forGetter(Vec3i::getZ)
		).apply(i, BlockPos::new));
		var merged = Codec.either(BlockPos.CODEC, legacy)
				.xmap(e -> e.map(x -> x, x -> x), Either::left);
		new CodecHandler<>(BlockPos.class, merged, BlockPos.STREAM_CODEC);
		new CodecHandler<>(Vec3.class, Vec3.CODEC, Vec3.STREAM_CODEC);
	}

	// register generic codec
	static {
		new RecordCodec();
		new EnumCodec();
		new ArrayCodec();
		new AliasCodec();
		new ListCodec();
		new SetCodec();
		new MapCodec();
		new HolderCodec();
		new HolderSetCodec();
		new OptionalCodec();
	}

	// register null defer
	static {
		new SimpleNullDefer<>(ItemStack.class, ItemStack.EMPTY);
		new PrimitiveNullDefer<>(Integer.class, 0);
		new PrimitiveNullDefer<>(int.class, 0);
		new PrimitiveNullDefer<>(Long.class, 0L);
		new PrimitiveNullDefer<>(long.class, 0L);
		new PrimitiveNullDefer<>(Short.class, (short) 0);
		new PrimitiveNullDefer<>(short.class, (short) 0);
		new PrimitiveNullDefer<>(Byte.class, (byte) 0);
		new PrimitiveNullDefer<>(byte.class, (byte) 0);
		new PrimitiveNullDefer<>(Character.class, (char) 0);
		new PrimitiveNullDefer<>(char.class, (char) 0);
		new PrimitiveNullDefer<>(Double.class, 0d);
		new PrimitiveNullDefer<>(double.class, 0d);
		new PrimitiveNullDefer<>(Float.class, 0f);
		new PrimitiveNullDefer<>(float.class, 0f);
		new PrimitiveNullDefer<>(Boolean.class, false);
		new PrimitiveNullDefer<>(boolean.class, false);
	}


	private static <T> void registerReg(Class<T> cls, HolderReg<T> reg) {
		REGMAP.put(cls, reg);
	}

	public static <T> void registerReg(Class<T> cls, ResourceKey<? extends Registry<T>> reg) {
		registerReg(cls, new HolderReg<>(new CodecReg<>(
				RegistryFixedCodec.create(reg),
				ByteBufCodecs.holderRegistry(reg)
		), new CodecReg<>(
				RegistryCodecs.homogeneousList(reg),
				ByteBufCodecs.holderSet(reg)
		)));
	}

	public static <T> HolderReg<T> getReg(Class<T> cls) {
		return Wrappers.cast(REGMAP.get(cls));
	}

	public static <T> void enableVanilla(Class<T> cls, Registry<T> reg) {
		registerReg(cls, reg.key());
		if (RegistryManager.isNonSyncedBuiltInRegistry(reg)) {
			new CodecHandler<>(cls, reg.byNameCodec(),
					ByteBufCodecs.fromCodecWithRegistries(reg.byNameCodec()));
		} else {
			new CodecHandler<>(cls, reg.byNameCodec(),
					ByteBufCodecs.registry(reg.key()));
		}
	}

	static {
		enableVanilla(Item.class, BuiltInRegistries.ITEM);
		enableVanilla(Block.class, BuiltInRegistries.BLOCK);
		enableVanilla(Potion.class, BuiltInRegistries.POTION);
		enableVanilla(MobEffect.class, BuiltInRegistries.MOB_EFFECT);
		enableVanilla(Attribute.class, BuiltInRegistries.ATTRIBUTE);
		enableVanilla(Fluid.class, BuiltInRegistries.FLUID);
		enableVanilla(Wrappers.cast(EntityType.class), BuiltInRegistries.ENTITY_TYPE);

		registerReg(Enchantment.class, Registries.ENCHANTMENT);
		registerReg(DamageType.class, Registries.DAMAGE_TYPE);
	}

	public static void register() {

	}

}
