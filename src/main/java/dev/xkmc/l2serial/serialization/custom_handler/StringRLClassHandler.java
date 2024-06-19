package dev.xkmc.l2serial.serialization.custom_handler;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public class StringRLClassHandler<T> extends StringClassHandler<T> {

	@Deprecated
	public StringRLClassHandler(Class<T> cls, Supplier<Registry<T>> reg) {
		super(cls, s -> reg.get().get(ResourceLocation.parse(s)), t -> reg.get().getKey(t).toString());
	}

}
