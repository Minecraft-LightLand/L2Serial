package dev.xkmc.l2serial.serialization.generic_types;

import dev.xkmc.l2serial.serialization.unified_processor.UnifiedContext;
import dev.xkmc.l2serial.serialization.type_cache.TypeInfo;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"unchecked", "unsafe", "rawtypes"})
public class EnumCodec extends GenericCodec {

	@Override
	public boolean predicate(TypeInfo cls, @Nullable Object obj) {
		return cls.getAsClass().isEnum();
	}

	@Override
	public <C extends UnifiedContext<E, O, A>, E, O extends E, A extends E>
	Object deserializeValue(C ctx, E e, TypeInfo cls, @Nullable Object ans) throws Exception {
		return Enum.valueOf((Class) cls.getAsClass(), ctx.getAsString(e));
	}

	@Override
	public <C extends UnifiedContext<E, O, A>, E, O extends E, A extends E>
	E serializeValue(C ctx, TypeInfo cls, @Nullable Object obj) throws Exception {
		return ctx.fromString(((Enum<?>) obj).name());
	}

}
