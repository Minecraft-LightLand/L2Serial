package dev.xkmc.l2serial.serialization.type_cache;

import dev.xkmc.l2serial.util.LazyExc;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"rawtypes", "unsafe"})
public class RecordCache {

	private static final Map<Class<?>, RecordCache> CACHE = new ConcurrentHashMap<>();

	public static RecordCache get(Class<?> cls) {
		return CACHE.computeIfAbsent(cls, RecordCache::new);
	}

	private final LazyExc<Field[]> fields;
	private final LazyExc<Constructor<?>> factory;

	private RecordCache(Class<?> cls) {
		fields = new LazyExc<>(() -> {
			var ans = Arrays.stream(cls.getDeclaredFields()).filter(f -> !Modifier.isStatic(f.getModifiers())).toArray(Field[]::new);
			for (Field f : ans) {
				f.setAccessible(true);
			}
			return ans;
		});
		factory = new LazyExc<>(() -> {
			Class[] clss = Arrays.stream(fields.get()).map(Field::getType).toArray(Class[]::new);
			var ans = cls.getConstructor(clss);
			ans.setAccessible(true);
			return ans;
		});
	}

	public Field[] getFields() throws Exception {
		return fields.get();
	}

	public Object create(Object[] objs) throws Exception {
		return factory.get().newInstance(objs);
	}
}
