package dev.xkmc.l2serial.serialization.type_cache;

import dev.xkmc.l2serial.serialization.SerialClass;
import dev.xkmc.l2serial.util.LazyExc;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClassCache {

	private static final Map<Class<?>, ClassCache> CACHE = new ConcurrentHashMap<>();

	public static ClassCache get(Class<?> cls) {
		return CACHE.computeIfAbsent(cls, ClassCache::new);
	}

	public final Class<?> cls;

	private final LazyExc<SerialClass> annotation;
	private final LazyExc<ClassCache> superClass;
	private final LazyExc<Constructor<?>> constructor;
	private final LazyExc<FieldCache[]> fields;
	private final LazyExc<MethodCache[]> methods;

	private ClassCache(Class<?> cls) {
		this.cls = cls;
		this.constructor = new LazyExc<>(() -> {
			var ans = cls.getConstructor();
			ans.setAccessible(true);
			return ans;
		});
		this.annotation = new LazyExc<>(() -> cls.getAnnotation(SerialClass.class));
		this.fields = new LazyExc<>(() -> Arrays.stream(cls.getDeclaredFields()).map(FieldCache::new).toArray(FieldCache[]::new));
		this.methods = new LazyExc<>(() -> Arrays.stream(cls.getDeclaredMethods()).map(MethodCache::new).toArray(MethodCache[]::new));
		this.superClass = new LazyExc<>(() -> get(cls.getSuperclass()));
	}

	public Object create() throws Exception {
		return constructor.get().newInstance();
	}

	@Nullable
	public SerialClass getSerialAnnotation() throws Exception {
		return annotation.get();
	}

	public FieldCache[] getFields() throws Exception {
		return fields.get();
	}

	public ClassCache getSuperclass() throws Exception {
		return superClass.get();
	}

	public MethodCache[] getMethods() throws Exception {
		return methods.get();
	}

}
