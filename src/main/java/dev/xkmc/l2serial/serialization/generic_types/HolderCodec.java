package dev.xkmc.l2serial.serialization.generic_types;

import dev.xkmc.l2serial.serialization.custom_handler.Handlers;
import dev.xkmc.l2serial.serialization.type_cache.TypeInfo;
import dev.xkmc.l2serial.serialization.unified_processor.TreeContext;
import dev.xkmc.l2serial.serialization.unified_processor.UnifiedContext;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.data.loading.DatagenModLoader;
import org.jetbrains.annotations.Nullable;

public class HolderCodec extends GenericCodec {

	public static HolderCodec INS;

	public HolderCodec() {
		INS = this;
	}

	@Override
	public boolean predicate(Class<?> cls) {
		return Holder.class.isAssignableFrom(cls);
	}

	@Override
	public <C extends UnifiedContext<E, O, A>, E, O extends E, A extends E> Object deserializeValue(C ctx, E e, TypeInfo cls, @Nullable Object ans) throws Exception {
		var type = cls.getGenericType(0).getAsClass();
		return ctx.deserializeCodec(Handlers.getReg(type).holder(), e);
	}

	@Override
	public <C extends UnifiedContext<E, O, A>, E, O extends E, A extends E> E serializeValue(C ctx, TypeInfo cls, Object obj) throws Exception {
		if (ctx instanceof TreeContext<?, ?, ?> && DatagenModLoader.isRunningDataGen()) {
			Holder<?> holder = (Holder<?>) obj;
			return ctx.serializeSpecial(ResourceLocation.class, holder.getKey().location());
		}
		var type = cls.getGenericType(0).getAsClass();
		return ctx.serializeCodec(Handlers.getReg(type).holder(), obj);
	}

}
