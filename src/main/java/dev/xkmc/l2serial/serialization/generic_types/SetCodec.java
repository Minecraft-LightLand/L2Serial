package dev.xkmc.l2serial.serialization.generic_types;

import dev.xkmc.l2serial.serialization.unified_processor.UnifiedCodec;
import dev.xkmc.l2serial.serialization.unified_processor.UnifiedContext;
import dev.xkmc.l2serial.serialization.type_cache.TypeInfo;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

@SuppressWarnings({"unchecked", "unsafe", "rawtypes"})
public class SetCodec extends GenericCodec {

	@Override
	public boolean predicate(TypeInfo cls, @Nullable Object obj) {
		return Set.class.isAssignableFrom(cls.getAsClass());
	}

	@Override
	public <C extends UnifiedContext<E, O, A>, E, O extends E, A extends E>
	Object deserializeValue(C ctx, E e, TypeInfo cls, @Nullable Object ans) throws Exception {
		A arr = ctx.castAsList(e);
		TypeInfo com = cls.getGenericType(0);
		int n = ctx.getSize(arr);
		if (ans == null) ans = cls.newInstance();
		Set list = (Set) ans;
		list.clear();
		for (int i = 0; i < n; i++) {
			list.add(UnifiedCodec.deserializeValue(ctx, ctx.getElement(arr, i), com, null));
		}
		return ans;
	}

	@Override
	public <C extends UnifiedContext<E, O, A>, E, O extends E, A extends E>
	E serializeValue(C ctx, TypeInfo cls, @Nullable Object obj) throws Exception {
		Set<?> list = (Set<?>) obj;
		A ans = ctx.createList(list.size());
		TypeInfo com = cls.getGenericType(0);
		for (Object o : list) {
			ctx.addListItem(ans, UnifiedCodec.serializeValue(ctx, com, o));
		}
		return ans;
	}

}
