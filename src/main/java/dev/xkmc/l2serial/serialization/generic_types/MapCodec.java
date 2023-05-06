package dev.xkmc.l2serial.serialization.generic_types;

import com.mojang.datafixers.util.Pair;
import dev.xkmc.l2serial.serialization.unified_processor.UnifiedCodec;
import dev.xkmc.l2serial.serialization.unified_processor.UnifiedContext;
import dev.xkmc.l2serial.serialization.type_cache.TypeInfo;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"unchecked", "unsafe", "rawtypes"})
public class MapCodec extends GenericCodec {

	@Override
	public boolean predicate(TypeInfo cls, @Nullable Object obj) {
		return Map.class.isAssignableFrom(cls.getAsClass());
	}

	@Override
	public <C extends UnifiedContext<E, O, A>, E, O extends E, A extends E>
	Object deserializeValue(C ctx, E e, TypeInfo cls, @Nullable Object ans) throws Exception {
		if (ans == null)
			ans = cls.newInstance();
		TypeInfo ckey = cls.getGenericType(0);
		TypeInfo cval = cls.getGenericType(1);
		if (ctx.isListFormat(e)) {
			A arr = ctx.castAsList(e);
			int n = ctx.getSize(arr);
			((Map) ans).clear();
			for (int i = 0; i < n; i++) {
				O jeo = ctx.castAsMap(ctx.getElement(arr, i));
				Object key = UnifiedCodec.deserializeValue(ctx, ctx.getKeyOfEntry(jeo), ckey, null);
				Object val = UnifiedCodec.deserializeValue(ctx, ctx.getValueOfEntry(jeo), cval, null);
				((Map) ans).put(key, val);
			}
			return ans;
		} else {
			return ctx.deserializeEfficientMap(e, ckey, cval, ans);
		}
	}

	@Override
	public <C extends UnifiedContext<E, O, A>, E, O extends E, A extends E>
	E serializeValue(C ctx, TypeInfo cls, @Nullable Object obj) throws Exception {
		Map<?, ?> map = (Map<?, ?>) obj;
		TypeInfo ckey = cls.getGenericType(0);
		TypeInfo cval = cls.getGenericType(1);
		List<Pair<E, E>> list = new ArrayList<>();
		A lans = ctx.createList(map.size());
		boolean can_be_map = true;
		for (Map.Entry<?, ?> ent : map.entrySet()) {
			E k = UnifiedCodec.serializeValue(ctx, ckey, ent.getKey());
			E v = UnifiedCodec.serializeValue(ctx, cval, ent.getValue());
			list.add(Pair.of(k, v));
			can_be_map &= ctx.canBeString(k);
		}
		if (can_be_map) {
			O ans = ctx.createMap();
			for (Pair<E, E> p : list) {
				ctx.addField(ans, ctx.getAsString(p.getFirst()), p.getSecond());
			}
			return ans;
		} else {
			for (Pair<E, E> p : list) {
				O entry = ctx.createMap();
				ctx.setKeyOfEntry(entry, p.getFirst());
				ctx.setValueOfEntry(entry, p.getSecond());
				ctx.addListItem(lans, entry);
			}
			return lans;
		}
	}

}
