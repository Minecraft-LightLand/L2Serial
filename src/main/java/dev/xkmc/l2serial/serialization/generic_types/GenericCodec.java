package dev.xkmc.l2serial.serialization.generic_types;

import dev.xkmc.l2serial.serialization.custom_handler.Handlers;
import dev.xkmc.l2serial.serialization.type_cache.TypeInfo;
import dev.xkmc.l2serial.serialization.unified_processor.UnifiedContext;

import javax.annotation.Nullable;

public abstract class GenericCodec {

	protected GenericCodec() {
		synchronized (Handlers.LIST) {
			Handlers.LIST.add(this);
		}
	}

	public abstract boolean predicate(TypeInfo info, @Nullable Object obj);

	public abstract <C extends UnifiedContext<E, O, A>, E, O extends E, A extends E>
	Object deserializeValue(C ctx, E e, TypeInfo cls, @Nullable Object ans) throws Exception;

	public abstract <C extends UnifiedContext<E, O, A>, E, O extends E, A extends E>
	E serializeValue(C ctx, TypeInfo cls, Object obj) throws Exception;
}
