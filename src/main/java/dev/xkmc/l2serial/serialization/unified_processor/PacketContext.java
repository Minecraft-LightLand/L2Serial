package dev.xkmc.l2serial.serialization.unified_processor;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import dev.xkmc.l2serial.serialization.custom_handler.Handlers;
import dev.xkmc.l2serial.serialization.type_cache.ClassCache;
import dev.xkmc.l2serial.serialization.type_cache.FieldCache;
import dev.xkmc.l2serial.serialization.type_cache.TypeInfo;
import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;
import java.util.Optional;

public class PacketContext extends SingletonContext<FriendlyByteBuf> {

	public PacketContext(FriendlyByteBuf instance) {
		super(instance);
	}

	@Override
	public boolean hasSpecialHandling(Class<?> cls) {
		return Handlers.PACKET_MAP.containsKey(cls);
	}

	@Override
	public Object deserializeSpecial(Class<?> cls, FriendlyByteBuf self) {
		return Handlers.PACKET_MAP.get(cls).fromPacket(instance);
	}

	@Override
	public FriendlyByteBuf serializeSpecial(Class<?> cls, Object obj) {
		Handlers.PACKET_MAP.get(cls).toPacket(instance, obj);
		return instance;
	}

	@Override
	public Optional<Either<Optional<Object>, TypeInfo>> fetchRealClass(@Nullable FriendlyByteBuf obj, TypeInfo cls) throws Exception {
		byte header = instance.readByte();
		if (header == 0) {
			return Optional.of(Either.left(Optional.empty()));
		} else if (header == 2) {
			return Optional.of(Either.right(TypeInfo.of(Class.forName(instance.readUtf()))));
		} else return Optional.empty();
	}

	@Override
	public Optional<Pair<FriendlyByteBuf, Optional<ClassCache>>> writeRealClass(TypeInfo cls, @Nullable Object obj) throws Exception {
		if (obj == null) {
			instance.writeByte(0);
			return Optional.of(Pair.of(instance, Optional.empty()));
		}
		Optional<Wrappers.ExcSup<FriendlyByteBuf>> special = UnifiedCodec.serializeSpecial(this, cls, obj);
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
	public boolean shouldRead(FriendlyByteBuf obj, FieldCache field) {
		return true;
	}

	@Override
	public int getSize(FriendlyByteBuf self) {
		return instance.readInt();
	}

	@Override
	public String getAsString(FriendlyByteBuf self) {
		return instance.readUtf();
	}

	@Override
	public FriendlyByteBuf createList(int size) {
		instance.writeInt(size);
		return instance;
	}

	@Override
	public FriendlyByteBuf fromString(String str) {
		instance.writeUtf(str);
		return instance;
	}

}
