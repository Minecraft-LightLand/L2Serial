package dev.xkmc.l2serial.serialization.codec;

import dev.xkmc.l2serial.serialization.marker.SerialField;
import dev.xkmc.l2serial.serialization.type_cache.ClassCache;
import dev.xkmc.l2serial.serialization.unified_processor.TagContext;
import dev.xkmc.l2serial.serialization.unified_processor.UnifiedCodec;
import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.util.function.Predicate;

@SuppressWarnings({"unused"})
public class ValueCodec {

	private Predicate<SerialField> pred = e -> true;

	public ValueCodec() {
	}

	public ValueCodec pred(Predicate<SerialField> pred) {
		this.pred = pred;
		return this;
	}

	@Nullable
	public <T> T fromTag(ValueInput tag, Class<?> cls) {
		return fromTag(tag, cls, null, "data");
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public <T> T fromTag(ValueInput tag, Class<?> cls, @Nullable T obj, String field) {
		var opt = tag.read(field, CompoundTag.CODEC);
		if (opt.isEmpty()) return obj;
		return (T) Wrappers.get(() -> UnifiedCodec.deserializeObject(new TagContext(tag.lookup(), pred), opt.get(), ClassCache.get(cls), obj));
	}

	public void toTag(HolderLookup.Provider pvd, ValueOutput tag, Object obj) {
		toTag(pvd, tag, obj.getClass(), obj, "data");
	}

	public void toTag(HolderLookup.Provider pvd, ValueOutput tag, Class<?> cls, Object obj, String field) {
		var ans = Wrappers.get(() -> UnifiedCodec.serializeObject(new TagContext(pvd, pred), new CompoundTag(), ClassCache.get(cls), obj));
		tag.store(field, CompoundTag.CODEC, ans);
	}

}