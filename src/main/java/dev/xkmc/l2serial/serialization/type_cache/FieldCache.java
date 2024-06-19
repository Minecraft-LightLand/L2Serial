package dev.xkmc.l2serial.serialization.type_cache;

import dev.xkmc.l2serial.serialization.marker.SerialField;
import dev.xkmc.l2serial.util.Wrappers;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class FieldCache {

	private final Field field;
	private final String name;
	private final Map<Class<? extends Annotation>, ? extends Annotation> cache = new HashMap<>();

	FieldCache(Field field) {
		this.field = field;
		this.name = field.getName();
		this.field.setAccessible(true);
	}

	@Nullable
	public <T extends Annotation> T getAnnotation(Class<T> cls) throws Exception {
		if (cache.containsKey(cls)) {
			return Wrappers.cast(cache.get(cls));
		}
		T ans = field.getAnnotation(cls);
		cache.put(cls, Wrappers.cast(ans));
		return ans;
	}

	@Nullable
	public SerialField getSerialAnnotation() throws Exception {
		return getAnnotation(SerialField.class);
	}

	public String getName() {
		return name;
	}

	public Object get(@Nullable Object ans) throws Exception {
		return field.get(ans);
	}

	public void set(Object target, @Nullable Object value) throws Exception {
		field.set(target, value);
	}

	public TypeInfo toType() {
		return TypeInfo.of(field);
	}
}
