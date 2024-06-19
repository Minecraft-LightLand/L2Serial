package dev.xkmc.l2serial.serialization.unified_processor;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import dev.xkmc.l2serial.serialization.generic_types.HolderCodecReg;
import dev.xkmc.l2serial.serialization.type_cache.ClassCache;
import dev.xkmc.l2serial.serialization.type_cache.FieldCache;
import dev.xkmc.l2serial.serialization.type_cache.TypeInfo;

import javax.annotation.Nullable;
import java.util.Optional;

public abstract class UnifiedContext<E, O, A> {

	protected UnifiedContext() {

	}

	public abstract boolean hasSpecialHandling(Class<?> cls);

	public abstract Object deserializeSpecial(Class<?> cls, E e);

	public abstract E serializeSpecial(Class<?> cls, Object obj);

	public abstract Object deserializeCodec(HolderCodecReg<?> cls, E e);

	public abstract E serializeCodec(HolderCodecReg<?> cls, Object e);

	/**
	 * Optional.empty() : normal
	 * Optional.of(Either.left(...)) : fast return
	 * Optional.of(Either.right(...)) : class override
	 */
	public abstract Optional<Either<Optional<Object>, TypeInfo>> fetchRealClass(@Nullable E obj, TypeInfo def) throws Exception;

	/**
	 * Optional.empty() : normal
	 * Optional.of(Pair.of(..., Optional.empty())) : fast return
	 * Optional.of(Pair.of(..., Optional.of(...))) : class override
	 */
	public abstract Optional<Pair<E, Optional<ClassCache>>> writeRealClass(TypeInfo cls, @Nullable Object obj) throws Exception;

	public abstract boolean shouldRead(O obj, FieldCache field) throws Exception;

	/**
	 * used for object deserialization
	 */
	public abstract E retrieve(O obj, String field);

	/**
	 * used for collection-like deserialization
	 */
	public abstract A castAsList(E e);

	/**
	 * used for collection-like deserialization
	 */
	public abstract int getSize(A a);

	/**
	 * used for collection-like deserialization
	 */
	public abstract E getElement(A arr, int i);

	/**
	 * used for map deserialization
	 */
	public abstract boolean isListFormat(E e);

	/**
	 * used for map-like and object deserialization
	 */
	public abstract O castAsMap(E element);

	/**
	 * used for map-like deserialization, to retrieve key-value pair
	 */
	public abstract E getKeyOfEntry(O obj);

	/**
	 * used for map-like deserialization, to retrieve key-value pair
	 */
	public abstract E getValueOfEntry(O obj);


	/**
	 * used for map-like deserialization, to retrieve key-value pair
	 */
	public abstract void setKeyOfEntry(O obj, E e);

	/**
	 * used for map-like deserialization, to retrieve key-value pair
	 */
	public abstract void setValueOfEntry(O obj, E e);

	/**
	 * when isListFormat return false, call this one to deserialize non-list maps
	 */
	public abstract Object deserializeEfficientMap(E e, TypeInfo ckey, TypeInfo cval, Object ans) throws Exception;

	/**
	 * for enum
	 */
	public abstract String getAsString(E e);

	public abstract void addField(O obj, String str, E e);

	public abstract void addOptionalClass(O obj, Class<?> objcls, Class<?> clsinfo);

	public abstract A createList(int size);

	public abstract O createMap();

	public abstract void addListItem(A a, E e);

	/**
	 * for map serialization. Return false to disable efficient map
	 */
	public abstract boolean canBeString(E e);

	public abstract E fromString(String str);

	public abstract boolean shouldWrite(SerialField sf);

}
