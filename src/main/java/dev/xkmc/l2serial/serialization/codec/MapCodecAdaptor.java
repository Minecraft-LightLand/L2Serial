package dev.xkmc.l2serial.serialization.codec;

import com.google.gson.JsonElement;
import com.mojang.serialization.*;
import dev.xkmc.l2serial.serialization.nulldefer.NullDefer;
import dev.xkmc.l2serial.serialization.type_cache.ClassCache;
import dev.xkmc.l2serial.serialization.type_cache.FieldCache;
import dev.xkmc.l2serial.serialization.type_cache.RecordCache;
import dev.xkmc.l2serial.serialization.type_cache.TypeInfo;
import dev.xkmc.l2serial.serialization.unified_processor.JsonContext;
import dev.xkmc.l2serial.serialization.unified_processor.TagContext;
import dev.xkmc.l2serial.serialization.unified_processor.TreeContext;
import dev.xkmc.l2serial.serialization.unified_processor.UnifiedCodec;
import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.nbt.Tag;
import org.apache.logging.log4j.Level;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public abstract class MapCodecAdaptor<T> extends MapCodec<T> {

	public static <T> MapCodecAdaptor<T> of(Class<T> cls) {
		if (cls.isRecord()) return new Rcd<>(cls);
		return new Cls<>(cls);
	}

	@Nullable
	public static <E, O extends E, A extends E> TreeContext<E, O, A> getContext(DynamicOps<?> ops) {
		if (ops.empty() instanceof JsonElement) {
			DynamicOps<JsonElement> jops = Wrappers.cast(ops);
			return Wrappers.cast(new JsonContext(jops));
		}
		if (ops.empty() instanceof Tag) {
			DynamicOps<Tag> tops = Wrappers.cast(ops);
			return Wrappers.cast(new TagContext(tops, f -> true));
		}
		return null;
	}

	private static class Rcd<T> extends MapCodecAdaptor<T> {

		private final Class<T> cls;
		private final Map<String, Field> map = new LinkedHashMap<>();

		private Rcd(Class<T> cls) {
			this.cls = cls;
			try {
				RecordCache cache = RecordCache.get(cls);
				for (Field f : cache.getFields()) {
					map.put(f.getName(), f);
				}
			} catch (Exception e) {
				CodecAdaptor.LOGGER.throwing(Level.ERROR, e);
			}
		}

		@Override
		public <E> Stream<E> keys(DynamicOps<E> ops) {
			return map.keySet().stream().map(ops::createString);
		}

		@Override
		public <E> DataResult<T> decode(DynamicOps<E> ops, MapLike<E> input) {
			var ctx = getContext(ops);
			if (ctx == null) {
				return DataResult.error(() -> "Unknown ops type " + ops.getClass().getSimpleName());
			}
			try {
				RecordCache cache = RecordCache.get(cls);
				Field[] fields = cache.getFields();
				Object[] objs = new Object[fields.length];
				for (int i = 0; i < fields.length; i++) {
					Field f = fields[i];
					objs[i] = UnifiedCodec.deserializeValue(ctx, input.get(f.getName()), TypeInfo.of(f), null);
				}
				T ans = Wrappers.cast(cache.create(objs));
				return DataResult.success(ans);
			} catch (Exception e) {
				return DataResult.error(e::getMessage);
			}
		}

		@Override
		public <E> RecordBuilder<E> encode(T input, DynamicOps<E> ops, RecordBuilder<E> prefix) {
			var ctx = getContext(ops);
			if (ctx == null) {
				CodecAdaptor.LOGGER.error("Unknown ops type {}", ops.getClass().getSimpleName());
				return prefix;
			}
			try {
				for (var entry : map.entrySet()) {
					var f = entry.getValue();
					E val = Wrappers.cast(UnifiedCodec.serializeValue(ctx, TypeInfo.of(f), f.get(input)));
					prefix.add(entry.getKey(), val);
				}
			} catch (Exception e) {
				CodecAdaptor.LOGGER.throwing(Level.ERROR, e);
			}
			return prefix;
		}
	}

	private static class Cls<T> extends MapCodecAdaptor<T> {


		private final Class<T> cls;
		private final Map<String, FieldCache> map = new LinkedHashMap<>();

		private Cls(Class<T> cls) {
			this.cls = cls;
			try {
				ClassCache cache = ClassCache.get(this.cls);
				while (cache.getSerialAnnotation() != null) {
					for (FieldCache f : cache.getFields()) {
						if (f.getSerialAnnotation() != null) {
							map.put(f.getName(), f);
						}
					}
					cache = cache.getSuperclass();
				}
			} catch (Exception e) {
				CodecAdaptor.LOGGER.throwing(Level.ERROR, e);
			}
		}

		@Override
		public <E> Stream<E> keys(DynamicOps<E> ops) {
			return map.keySet().stream().map(ops::createString);
		}

		@Override
		public <E> DataResult<T> decode(DynamicOps<E> ops, MapLike<E> input) {
			var ctx = getContext(ops);
			if (ctx == null) {
				return DataResult.error(() -> "Unknown ops type " + ops.getClass().getSimpleName());
			}
			try {
				ClassCache cache = ClassCache.get(this.cls);
				Object ans = cache.create();
				for (var entry : map.entrySet()) {
					FieldCache f = entry.getValue();
					if (ctx.shouldRead(input, f)) {
						Object def = f.get(ans);
						Object content;
						content = UnifiedCodec.deserializeValue(ctx, ctx.retrieve(input, f.getName()), f.toType(), def);
						f.set(ans, content);
					} else {
						NullDefer<?> nil = NullDefer.get(f.toType().getAsClass());
						if (nil != null) {
							f.set(ans, nil.getNullDefault());
						}
					}
				}
				return DataResult.success(Wrappers.cast(ans));
			} catch (Exception e) {
				CodecAdaptor.LOGGER.throwing(Level.ERROR, e);
				return DataResult.error(e::getMessage);
			}
		}

		@Override
		public <E> RecordBuilder<E> encode(T input, DynamicOps<E> ops, RecordBuilder<E> prefix) {
			var ctx = getContext(ops);
			if (ctx == null) {
				CodecAdaptor.LOGGER.error("Unknown ops type {}", ops.getClass().getSimpleName());
				return prefix;
			}
			try {
				for (var entry : map.entrySet()) {
					FieldCache f = entry.getValue();
					E content = Wrappers.cast(UnifiedCodec.serializeValue(ctx, f.toType(), f.get(input)));
					prefix.add(entry.getKey(), content);
				}
			} catch (Exception e) {
				CodecAdaptor.LOGGER.throwing(Level.ERROR, e);
			}
			return prefix;
		}

	}

}
