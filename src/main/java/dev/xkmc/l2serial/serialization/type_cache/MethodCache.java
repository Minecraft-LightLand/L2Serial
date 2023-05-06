package dev.xkmc.l2serial.serialization.type_cache;

import dev.xkmc.l2serial.serialization.SerialClass;
import dev.xkmc.l2serial.util.LazyExc;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

public class MethodCache {

	private final Method method;
	private final LazyExc<SerialClass.OnInject> serial;

	MethodCache(Method method) {
		this.method = method;
		serial = new LazyExc<>(() -> method.getAnnotation(SerialClass.OnInject.class));
	}

	@Nullable
	public SerialClass.OnInject getInjectAnnotation() throws Exception {
		return serial.get();
	}

	public void invoke(Object ans) throws Exception {
		method.invoke(ans);
	}
}
