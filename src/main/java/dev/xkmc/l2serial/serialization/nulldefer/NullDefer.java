package dev.xkmc.l2serial.serialization.nulldefer;

import dev.xkmc.l2serial.serialization.custom_handler.Handlers;
import dev.xkmc.l2serial.util.Wrappers;

import javax.annotation.Nullable;

public abstract class NullDefer<T> {

	@Nullable
	public static <T> NullDefer<T> get(Class<T> cls) {
		return Wrappers.cast(Handlers.NULL_DEFER.get(cls));
	}

	protected NullDefer(Class<T> cls) {
		Handlers.NULL_DEFER.put(cls, this);
	}

	public abstract boolean predicate(T obj);

	public abstract T getNullDefault();

}
