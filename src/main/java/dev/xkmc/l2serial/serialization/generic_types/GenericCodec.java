package dev.xkmc.l2serial.serialization.generic_types;

import dev.xkmc.l2serial.serialization.custom_handler.Handlers;
import dev.xkmc.l2serial.serialization.type_cache.TypeInfo;
import dev.xkmc.l2serial.serialization.unified_processor.UnifiedContext;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class GenericCodec {

	private static Set<Class<?>> NONMATCH = ConcurrentHashMap.newKeySet();
	private static Map<Class<?>, GenericCodec> CACHE = new ConcurrentHashMap<>();

	@Nullable
	public static GenericCodec find(TypeInfo info, @Nullable Object obj) {
		var cls = info.getAsClass();
		if (NONMATCH.contains(cls)) return null;
		var e = CACHE.get(info.getAsClass());
		if (e != null) return e;
		for (var x : Handlers.LIST) {
			if (x.predicate(cls)) {
				CACHE.put(cls, x);
				return x;
			}
		}
		NONMATCH.add(cls);
		return null;
	}

	protected GenericCodec() {
		Handlers.LIST.add(this);
	}

	public abstract boolean predicate(Class<?> cls);

	public abstract <C extends UnifiedContext<E, O, A>, E, O extends E, A extends E>
	Object deserializeValue(C ctx, E e, TypeInfo cls, @Nullable Object ans) throws Exception;

	public abstract <C extends UnifiedContext<E, O, A>, E, O extends E, A extends E>
	E serializeValue(C ctx, TypeInfo cls, Object obj) throws Exception;
}
