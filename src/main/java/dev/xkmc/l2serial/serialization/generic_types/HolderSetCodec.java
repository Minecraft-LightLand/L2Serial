package dev.xkmc.l2serial.serialization.generic_types;

import dev.xkmc.l2serial.serialization.custom_handler.Handlers;
import dev.xkmc.l2serial.serialization.type_cache.TypeInfo;
import dev.xkmc.l2serial.serialization.unified_processor.UnifiedContext;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import org.jetbrains.annotations.Nullable;

public class HolderSetCodec extends GenericCodec {

	@Override
	public boolean predicate(Class<?> cls) {
		return HolderSet.class.isAssignableFrom(cls);
	}

	@Override
	public <C extends UnifiedContext<E, O, A>, E, O extends E, A extends E> Object deserializeValue(C ctx, E e, TypeInfo cls, @Nullable Object ans) throws Exception {
		var type = cls.getGenericType(0).getAsClass();
		return ctx.deserializeCodec(Handlers.getReg(type).set(), e);
	}

	@Override
	public <C extends UnifiedContext<E, O, A>, E, O extends E, A extends E> E serializeValue(C ctx, TypeInfo cls, Object obj) throws Exception {
		var type = cls.getGenericType(0).getAsClass();
		return ctx.serializeCodec(Handlers.getReg(type).set(), obj);
	}

}
