package dev.xkmc.l2serial.serialization.type_cache;

import com.mojang.serialization.*;
import dev.xkmc.l2serial.util.Wrappers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@SuppressWarnings({"rawtypes", "unsafe"})
public class RecordCache {

	private static final Map<Class<?>, RecordCache> CACHE = new ConcurrentHashMap<>();

	public static RecordCache get(Class<?> cls) {
		return CACHE.computeIfAbsent(cls, RecordCache::new);
	}

	private final Class<?> cls;
	private volatile boolean init = false;
	private volatile Field[] fields;
	private volatile RecordComponent[] components;
	private volatile Constructor<?> factory;

	private RecordCache(Class<?> cls) {
		this.cls = cls;
	}

	private synchronized void init() throws Exception {
		if (init) return;
		init = true;
		components = cls.getRecordComponents();
		fields = Arrays.stream(cls.getDeclaredFields()).filter(f -> !Modifier.isStatic(f.getModifiers())).toArray(Field[]::new);
		for (Field f : fields) {
			f.setAccessible(true);
		}
		Class[] clss = Arrays.stream(fields).map(Field::getType).toArray(Class[]::new);
		factory = cls.getConstructor(clss);
		factory.setAccessible(true);
	}

	public Field[] getFields() throws Exception {
		init();
		return fields;
	}

	public RecordComponent[] getComponents() throws Exception {
		init();
		return components;
	}

	public <T> T get(Object obj, int index) throws Exception {
		init();
		return Wrappers.cast(components[index].getAccessor().invoke(obj));
	}

	public Object create(Object[] objs) throws Exception {
		init();
		return factory.newInstance(objs);
	}

	public MapCodec<?> getCodec(List<MapCodec<?>> list) {
		return new RecordListCodec<>(list);
	}

	private class RecordListCodec<O> extends MapCodec<O> {

		private final List<MapCodec<?>> list;

		public RecordListCodec(List<MapCodec<?>> list) {
			this.list = list;
		}

		@Override
		public <T> Stream<T> keys(DynamicOps<T> ops) {
			return list.stream().flatMap(e -> e.keys(ops));
		}

		@Override
		public <T> DataResult<O> decode(DynamicOps<T> ops, MapLike<T> map) {
			try {
				init();
				int n = fields.length;
				Object[] args = new Object[n];
				for (int i = 0; i < n; i++) {
					var res = list.get(i).decode(ops, map);
					if (res.error().isPresent()) {
						return DataResult.error(res.error().get().messageSupplier());
					} else if (res.result().isPresent()) {
						args[i] = res.result().get();
					}
				}
				return DataResult.success(Wrappers.cast(create(args)));
			} catch (Exception e) {
				return DataResult.error(e::getMessage);
			}
		}

		@Override
		public <T> RecordBuilder<T> encode(O o, DynamicOps<T> ops, RecordBuilder<T> builder) {
			try {
				init();
				for (int i = 0; i < list.size(); i++) {
					list.get(i).encode(get(o, i), ops, builder);
				}
				return builder;
			} catch (Exception e) {
				return builder.withErrorsFrom(DataResult.error(e::getMessage));
			}
		}

	}

}
