package dev.xkmc.l2serial.serialization.custom_handler;

import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import net.minecraft.core.MappedRegistry;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

public class RLClassHandler<R extends Tag, T> extends ClassHandler<R, T> {

	@Deprecated
	public RLClassHandler(Class<T> cls, MappedRegistry<T> r) {
		super(cls, e -> e == null ? JsonNull.INSTANCE : new JsonPrimitive(r.getKey(e).toString()),
				e -> e.isJsonNull() ? null : r.get(new ResourceLocation(e.getAsString())),
				p -> {
					int index = p.readInt();
					if (index == -1) return null;
					return r.byId(index);
				},
				(p, t) -> p.writeInt(t == null ? -1 : r.getId(t)),
				s -> s.getAsString().isEmpty() ? null : r.get(new ResourceLocation(s.getAsString())),
				t -> t == null ? StringTag.valueOf("") : StringTag.valueOf(r.getKey(t).toString()));
	}

}
