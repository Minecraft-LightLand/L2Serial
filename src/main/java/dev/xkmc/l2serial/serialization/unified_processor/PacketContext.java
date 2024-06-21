package dev.xkmc.l2serial.serialization.unified_processor;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import dev.xkmc.l2serial.serialization.custom_handler.Handlers;
import dev.xkmc.l2serial.serialization.generic_types.CodecReg;
import dev.xkmc.l2serial.serialization.type_cache.ClassCache;
import dev.xkmc.l2serial.serialization.type_cache.FieldCache;
import dev.xkmc.l2serial.serialization.type_cache.TypeInfo;
import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.network.RegistryFriendlyByteBuf;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Predicate;

public class PacketContext extends SingletonContext<RegistryFriendlyByteBuf> {
	private final Predicate<SerialField> pred;

	public PacketContext(RegistryFriendlyByteBuf instance, Predicate<SerialField> pred) {
		super(instance);
		this.pred = pred;
	}

	public PacketContext(RegistryFriendlyByteBuf instance) {
		this(instance, e -> true);
	}

	@Override
	public boolean hasSpecialHandling(Class<?> cls) {
		return Handlers.PACKET_MAP.containsKey(cls);
	}

	@Override
	public Object deserializeSpecial(Class<?> cls, RegistryFriendlyByteBuf self) {
		return Handlers.PACKET_MAP.get(cls).fromPacket(instance);
	}

	@Override
	public RegistryFriendlyByteBuf serializeSpecial(Class<?> cls, Object obj) {
		Handlers.PACKET_MAP.get(cls).toPacket(instance, obj);
		return instance;
	}

	@Override
	public Object deserializeCodec(CodecReg<?> cls, RegistryFriendlyByteBuf buf) {
		return cls.stream().decode(instance);
	}

	@Override
	public RegistryFriendlyByteBuf serializeCodec(CodecReg<?> cls, Object e) {
		cls.stream().encode(instance, Wrappers.cast(e));
		return instance;
	}

	@Override
	public Optional<Either<Optional<Object>, TypeInfo>> fetchRealClass(@Nullable RegistryFriendlyByteBuf obj, TypeInfo cls) throws Exception {
		byte header = instance.readByte();
		if (header == 0) {
			return Optional.of(Either.left(Optional.empty()));
		} else if (header == 2) {
			return Optional.of(Either.right(TypeInfo.of(Class.forName(instance.readUtf()))));
		} else return Optional.empty();
	}

	@Override
	public Optional<Pair<RegistryFriendlyByteBuf, Optional<ClassCache>>> writeRealClass(TypeInfo cls, @Nullable Object obj) throws Exception {
		if (obj == null) {
			instance.writeByte(0);
			return Optional.of(Pair.of(instance, Optional.empty()));
		}
		Optional<Wrappers.ExcSup<RegistryFriendlyByteBuf>> special = UnifiedCodec.serializeSpecial(this, cls, obj);
		if (special.isPresent()) {
			instance.writeByte(1);
			return Optional.of(Pair.of(special.get().get(), Optional.empty()));
		}
		if (obj.getClass() != cls.getAsClass()) {
			ClassCache cache = ClassCache.get(obj.getClass());
			if (cache.getSerialAnnotation() != null) {
				instance.writeByte(2);
				instance.writeUtf(obj.getClass().getName());
				return Optional.of(Pair.of(instance, Optional.of(cache)));
			}
		}
		instance.writeByte(1);
		return Optional.empty();
	}

	@Override
	public boolean shouldRead(RegistryFriendlyByteBuf obj, FieldCache field) throws Exception {
		return pred.test(field.getSerialAnnotation());
	}

	@Override
	public boolean shouldWrite(SerialField sf) {
		return pred.test(sf);
	}

	@Override
	public int getSize(RegistryFriendlyByteBuf self) {
		return instance.readInt();
	}

	@Override
	public String getAsString(RegistryFriendlyByteBuf self) {
		return instance.readUtf();
	}

	@Override
	public RegistryFriendlyByteBuf createList(int size) {
		instance.writeInt(size);
		return instance;
	}

	@Override
	public RegistryFriendlyByteBuf fromString(String str) {
		instance.writeUtf(str);
		return instance;
	}

}
