package dev.xkmc.l2serial.serialization.unified_processor;

import com.google.gson.*;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import dev.xkmc.l2serial.serialization.custom_handler.Handlers;
import dev.xkmc.l2serial.serialization.generic_types.CodecReg;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import dev.xkmc.l2serial.serialization.type_cache.FieldCache;
import dev.xkmc.l2serial.serialization.type_cache.TypeInfo;
import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

public class JsonContext extends TreeContext<JsonElement, JsonObject, JsonArray> {

	public JsonContext(HolderLookup.Provider access) {
		this(RegistryOps.create(JsonOps.INSTANCE, access));
	}

	public JsonContext(DynamicOps<JsonElement> ops) {
		super(Optional.of(Pair.of(JsonNull.INSTANCE, Optional.empty())), ops);
	}

	@Override
	public boolean hasSpecialHandling(Class<?> cls) {
		return Handlers.JSON_MAP.containsKey(cls);
	}

	@Override
	public Object deserializeSpecial(Class<?> cls, JsonElement e) {
		return Handlers.JSON_MAP.get(cls).fromJson(e);
	}

	@Override
	public JsonElement serializeSpecial(Class<?> cls, Object obj) {
		return Handlers.JSON_MAP.get(cls).toJson(obj);
	}

	@Override
	public Object deserializeCodec(CodecReg<?> cls, JsonElement e) {
		return cls.codec().decode(ops(), e).getOrThrow().getFirst();
	}

	@Override
	public JsonElement serializeCodec(CodecReg<?> cls, Object e) {
		return cls.codec().encodeStart(ops(), Wrappers.cast(e)).getOrThrow();
	}

	@Override
	public Optional<Either<Optional<Object>, TypeInfo>> fetchRealClass(@Nullable JsonElement e, TypeInfo def) throws Exception {
		if (e == null || e.isJsonNull()) {
			return Optional.of(Either.left(Optional.empty()));
		}
		if (e.isJsonObject()) {
			JsonObject obj = e.getAsJsonObject();
			if (obj.has("_class")) {
				String scls = obj.get("_class").getAsString();
				if (scls != null && scls.length() > 0) {
					return Optional.of(Either.right(TypeInfo.of(Class.forName(scls))));
				}
			}
		}
		return Optional.empty();
	}

	@Override
	public boolean shouldRead(JsonObject obj, FieldCache field) {
		return obj.has(field.getName());
	}

	@Override
	public JsonElement retrieve(JsonObject obj, String field) {
		return obj.get(field);
	}

	@Override
	public JsonArray castAsList(JsonElement e) {
		return e.getAsJsonArray();
	}

	@Override
	public int getSize(JsonArray arr) {
		return arr.size();
	}

	@Override
	public JsonElement getElement(JsonArray arr, int i) {
		return arr.get(i);
	}

	@Override
	public boolean isListFormat(JsonElement e) {
		return e.isJsonArray();
	}

	@Override
	public JsonObject castAsMap(JsonElement e) {
		return e.getAsJsonObject();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public Object deserializeEfficientMap(JsonElement e, TypeInfo ckey, TypeInfo cval, Object ans) throws Exception {
		((Map) ans).clear();
		for (Map.Entry<String, JsonElement> ent : e.getAsJsonObject().entrySet()) {
			Object key = ckey.getAsClass() == String.class ? ent.getKey() :
					ckey.getAsClass().isEnum() ? Enum.valueOf((Class) ckey.getAsClass(), ent.getKey()) :
							Handlers.JSON_MAP.get(ckey.getAsClass()).fromJson(new JsonPrimitive(ent.getKey()));
			((Map) ans).put(key, UnifiedCodec.deserializeValue(this, ent.getValue(), cval, null));
		}
		return ans;
	}

	@Override
	public String getAsString(@Nullable JsonElement e) {
		if (e == null) {
			return "";
		}
		return e.getAsString();
	}

	@Override
	public void addField(JsonObject obj, String str, JsonElement e) {
		obj.add(str, e);
	}

	@Override
	public JsonArray createList(int size) {
		return new JsonArray(size);
	}

	@Override
	public JsonObject createMap() {
		return new JsonObject();
	}

	@Override
	public void addListItem(JsonArray arr, JsonElement e) {
		arr.add(e);
	}

	@Override
	public boolean canBeString(JsonElement e) {
		return e instanceof JsonPrimitive p && p.isString();
	}

	@Override
	public JsonElement fromString(String str) {
		return new JsonPrimitive(str);
	}

	@Override
	public boolean shouldWrite(SerialField sf) {
		return true;
	}
}
