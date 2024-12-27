package dev.xkmc.l2serial.serialization.unified_processor;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapLike;
import dev.xkmc.l2serial.serialization.generic_types.CodecReg;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import dev.xkmc.l2serial.serialization.type_cache.ClassCache;
import dev.xkmc.l2serial.serialization.type_cache.FieldCache;
import dev.xkmc.l2serial.serialization.type_cache.TypeInfo;
import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.resources.RegistryOps;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class RealCodecContext<T> extends TreeContext<T, T, T> {

	private final RegistryOps<T> ops;

	protected RealCodecContext(Optional<Pair<T, Optional<ClassCache>>> nil, RegistryOps<T> ops) {
		super(nil, ops);
		this.ops = ops;
	}

	@Override
	public boolean hasSpecialHandling(Class<?> cls) {
		return false;//TODO
	}

	@Override
	public Object deserializeSpecial(Class<?> cls, T t) {
		return null;//TODO
	}

	@Override
	public T serializeSpecial(Class<?> cls, Object obj) {
		return null;//TODO
	}

	@Override
	public Object deserializeCodec(CodecReg<?> cls, T t) {
		return cls.codec().decode(ops, t).getOrThrow().getFirst();
	}

	@Override
	public T serializeCodec(CodecReg<?> cls, Object e) {
		return cls.codec().encodeStart(ops, Wrappers.cast(e)).getOrThrow();
	}

	@Override
	public Optional<Either<Optional<Object>, TypeInfo>> fetchRealClass(@Nullable T e, TypeInfo def) throws Exception {
		if (e == null) {
			return Optional.of(Either.left(Optional.empty()));
		}
		var map = ops.getMap(e);
		if (map.isSuccess()) {
			var m = map.getOrThrow();
			var scls = m.get("_class");
			if (scls != null) {
				var cls = ops.getStringValue(scls);
				if (cls.isSuccess() && !cls.getOrThrow().isEmpty()) {
					return Optional.of(Either.right(TypeInfo.of(Class.forName(cls.getOrThrow()))));
				}
			}
		}
		return Optional.empty();
	}

	@Override
	public boolean shouldRead(T obj, FieldCache field) throws Exception {
		return ops.get(obj, field.getName()).isSuccess();
	}

	@Override
	public T retrieve(T obj, String field) {
		return ops.get(obj, field).getOrThrow();
	}

	@Override
	public T castAsList(T t) {
		return t;
	}

	@Override
	public int getSize(T t) {
		return 0;//TODO
	}

	@Override
	public T getElement(T arr, int i) {
		return arr;//TODO
	}

	@Override
	public boolean isListFormat(T t) {
		return ops.getList(t).isSuccess();
	}

	@Override
	public T castAsMap(T element) {
		return element;
	}

	@Override
	public Object deserializeEfficientMap(T t, TypeInfo ckey, TypeInfo cval, Object ans) throws Exception {
		//TODO
		return null;
	}

	@Override
	public String getAsString(T t) {
		return ops.getStringValue(t).getOrThrow();
	}

	@Override
	public void addField(T obj, String str, T t) {
		ops.set(obj, str, t);
	}

	@Override
	public T createList(int size) {
		return ops.emptyList();
	}

	@Override
	public T createMap() {
		return ops.emptyMap();
	}

	@Override
	public void addListItem(T t, T t2) {
		ops.mergeToList(t, t2);
	}

	@Override
	public boolean canBeString(T t) {
		return ops.getStringValue(t).isSuccess();
	}

	@Override
	public T fromString(String str) {
		return ops.createString(str);
	}

	@Override
	public boolean shouldWrite(SerialField sf) {
		return true;
	}

	@Override
	public boolean shouldReadMap(MapLike<T> obj, FieldCache field) throws Exception {
		return obj.get(field.getName()) != null;
	}

	@Override
	public T retrieveMap(MapLike<T> obj, String field) {
		return obj.get(field);
	}

}
