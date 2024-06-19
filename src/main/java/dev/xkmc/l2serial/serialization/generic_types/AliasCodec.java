package dev.xkmc.l2serial.serialization.generic_types;

import dev.xkmc.l2serial.serialization.codec.AliasCollection;
import dev.xkmc.l2serial.serialization.type_cache.TypeInfo;
import dev.xkmc.l2serial.serialization.unified_processor.UnifiedCodec;
import dev.xkmc.l2serial.serialization.unified_processor.UnifiedContext;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings({"unsafe", "ConstantConditions"})
public class AliasCodec extends GenericCodec {

	@Override
	public boolean predicate(Class<?> cls) {
		return AliasCollection.class.isAssignableFrom(cls);
	}

	@Override
	public <C extends UnifiedContext<E, O, A>, E, O extends E, A extends E>
	Object deserializeValue(C ctx, E e, TypeInfo cls, @Nullable Object ans) throws Exception {
		AliasCollection<?> alias = (AliasCollection<?>) ans;
		A arr = ctx.castAsList(e);
		TypeInfo com = TypeInfo.of(alias.getElemClass());
		int n = ctx.getSize(arr);
		for (int i = 0; i < n; i++) {
			alias.setRaw(n, i, UnifiedCodec.deserializeValue(ctx, ctx.getElement(arr, i), com, null));
		}
		return alias;
	}

	@Override
	public <C extends UnifiedContext<E, O, A>, E, O extends E, A extends E>
	E serializeValue(C ctx, TypeInfo cls, Object obj) throws Exception {
		AliasCollection<?> alias = (AliasCollection<?>) obj;
		List<?> list = alias.getAsList();
		A ans = ctx.createList(list.size());
		TypeInfo com = TypeInfo.of(alias.getElemClass());
		for (Object o : list) {
			ctx.addListItem(ans, UnifiedCodec.serializeValue(ctx, com, o));
		}
		return ans;
	}

}
