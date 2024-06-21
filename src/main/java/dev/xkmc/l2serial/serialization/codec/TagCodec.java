package dev.xkmc.l2serial.serialization.codec;

import dev.xkmc.l2serial.serialization.marker.SerialField;
import dev.xkmc.l2serial.serialization.type_cache.ClassCache;
import dev.xkmc.l2serial.serialization.type_cache.TypeInfo;
import dev.xkmc.l2serial.serialization.unified_processor.TagContext;
import dev.xkmc.l2serial.serialization.unified_processor.UnifiedCodec;
import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.function.Predicate;

@SuppressWarnings({"unused"})
public class TagCodec {

	private final HolderLookup.Provider access;
	private Predicate<SerialField> pred = e -> true;

	public TagCodec(HolderLookup.Provider access) {
		this.access = access;
	}

	public TagCodec pred(Predicate<SerialField> pred) {
		this.pred = pred;
		return this;
	}

	/**
	 * Deserialize all fields by default.
	 * The data must not represent a null value.
	 * Supports only <code>@SerialClass</code> objects.
	 * For other objects, use <code>valueFromTag</code>
	 *
	 * @param tag source of data
	 * @param cls deserialization type information
	 * @return the deserialized object, or null if failed.
	 */
	@Nullable
	public <T> T fromTag(CompoundTag tag, Class<?> cls) {
		return fromTag(tag, cls, null);
	}

	/**
	 * The data must not represent a null value.
	 * Supports only <code>@SerialClass</code> objects.
	 * For other objects, use <code>valueFromTag</code>
	 *
	 * @param tag  source of data
	 * @param cls  deserialization type information
	 * @param obj  optional. The object to inject into. Constructs a new object if it's null.
	 * @return the deserialized object, or null if failed.
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T fromTag(CompoundTag tag, Class<?> cls, @Nullable T obj) {
		return (T) Wrappers.get(() -> UnifiedCodec.deserializeObject(new TagContext(access, pred), tag, ClassCache.get(cls), obj));
	}

	/**
	 * Supports only <code>@SerialClass</code> objects.
	 * For other objects, use <code>valueToTag</code>
	 *
	 * @param tag Destination to write to
	 * @param obj The object to serialize
	 * @return the <code>tag</code> provided
	 */
	@Nullable
	public CompoundTag toTag(CompoundTag tag, Object obj) {
		return toTag(tag, obj.getClass(), obj);
	}

	/**
	 * Supports only <code>@SerialClass</code> objects.
	 * For other objects, use <code>valueToTag</code>
	 *
	 * @param tag Destination to write to
	 * @param cls Serialization type information
	 * @param obj The object to serialize
	 * @return the <code>tag</code> provided
	 */
	@Nullable
	public CompoundTag toTag(CompoundTag tag, Class<?> cls, Object obj) {
		return Wrappers.get(() -> UnifiedCodec.serializeObject(new TagContext(access, pred), tag, ClassCache.get(cls), obj));
	}

	/**
	 * Deserialize any nonnull value
	 *
	 * @param tag  source of data
	 * @param cls  deserialization type information
	 * @return the deserialized value, or null if failed.
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T valueFromTag(Tag tag, Class<?> cls) {
		return (T) Wrappers.get(() -> UnifiedCodec.deserializeValue(new TagContext(access, pred), tag, TypeInfo.of(cls), null));
	}

	/**
	 * Serialize any value.
	 *
	 * @param cls  deserialization type information
	 * @param obj  The value to serialize
	 * @return a <code>Tag</code> representing the value
	 */
	@Nullable
	public Tag valueToTag(Class<?> cls, Object obj) {
		return Wrappers.get(() -> UnifiedCodec.serializeValue(new TagContext(access, pred), TypeInfo.of(cls), obj));
	}

}