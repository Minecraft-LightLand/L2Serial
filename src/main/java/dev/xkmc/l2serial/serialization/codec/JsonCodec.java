package dev.xkmc.l2serial.serialization.codec;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.xkmc.l2serial.serialization.unified_processor.UnifiedCodec;
import dev.xkmc.l2serial.serialization.unified_processor.JsonContext;
import dev.xkmc.l2serial.serialization.type_cache.ClassCache;
import dev.xkmc.l2serial.serialization.type_cache.TypeInfo;
import dev.xkmc.l2serial.util.Wrappers;

import javax.annotation.Nullable;

public class JsonCodec {

	@Nullable
	@SuppressWarnings("unchecked")
	public static <T> T from(JsonElement obj, Class<T> cls, @Nullable T ans) {
		return Wrappers.get(() -> (T) UnifiedCodec.deserializeValue(new JsonContext(), obj, TypeInfo.of(cls), ans));
	}

	@Nullable
	public static <T> JsonElement toJson(T obj) {
		return Wrappers.get(() -> UnifiedCodec.serializeValue(new JsonContext(), TypeInfo.of(obj.getClass()), obj));
	}

	@Nullable
	public static <T extends R, R> JsonElement toJson(T obj, Class<R> cls) {
		return Wrappers.get(() -> UnifiedCodec.serializeValue(new JsonContext(), TypeInfo.of(cls), obj));
	}

	@Nullable
	public static <T> JsonObject toJsonObject(T obj, JsonObject input) {
		return Wrappers.get(() -> UnifiedCodec.serializeObject(new JsonContext(), input, ClassCache.get(obj.getClass()), obj));
	}

}