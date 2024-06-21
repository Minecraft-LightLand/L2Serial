package dev.xkmc.l2serial.serialization.codec;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.xkmc.l2serial.serialization.type_cache.ClassCache;
import dev.xkmc.l2serial.serialization.type_cache.TypeInfo;
import dev.xkmc.l2serial.serialization.unified_processor.JsonContext;
import dev.xkmc.l2serial.serialization.unified_processor.UnifiedCodec;
import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.core.HolderLookup;

import javax.annotation.Nullable;

@SuppressWarnings({"unused"})
public class JsonCodec {

	private final HolderLookup.Provider access;

	public JsonCodec(HolderLookup.Provider access) {
		this.access = access;
	}

	/**
	 * The data must not represent a null object.
	 * @param obj source of data
	 * @param cls deserialization type information
	 * @param ans the object to inject into. Construct a new object if it's null.
	 * @return The deserialized object, or null if failed.
	 * If <code>ans</code> is provided, the return valud will be the same object
	 * as long as it's a <code>@SerialClass</code> object
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T from(JsonElement obj, Class<T> cls, @Nullable T ans) {
		return Wrappers.get(() -> (T) UnifiedCodec.deserializeValue(new JsonContext(access), obj, TypeInfo.of(cls), ans));
	}

	/**
	 * Serialization type information will be assumed to be the object type.
	 * The deserialization type of this object must match its class.
	 * @param obj The object to be serialized.
	 * @return The serialized json, or null if failed.
	 */
	@Nullable
	public <T> JsonElement toJson(T obj) {
		return Wrappers.get(() -> UnifiedCodec.serializeValue(new JsonContext(access), TypeInfo.of(obj.getClass()), obj));
	}

	/**
	 * @param obj The object to be serialized.
	 * @param cls The serialization type information that will be used to deserialize the result json.
	 * @return The serialized json, or null if failed.
	 */
	@Nullable
	public <T extends R, R> JsonElement toJson(T obj, Class<R> cls) {
		return Wrappers.get(() -> UnifiedCodec.serializeValue(new JsonContext(access), TypeInfo.of(cls), obj));
	}

	/**
	 * Serialize the object into provided json object.
	 * Supports only <code>@SerialClass</code> objects.
	 * Primarily used for recipes.
	 * @param obj The object to be serialized.
	 * @param input The json to write to
	 * @return The same <code>JsonObject</code> as <code>input</code>
	 * */
	@Nullable
	public <T> JsonObject toJsonObject(T obj, JsonObject input) {
		return Wrappers.get(() -> UnifiedCodec.serializeObject(new JsonContext(access), input, ClassCache.get(obj.getClass()), obj));
	}

}