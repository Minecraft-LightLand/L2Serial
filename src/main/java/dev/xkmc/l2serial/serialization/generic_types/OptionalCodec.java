package dev.xkmc.l2serial.serialization.generic_types;

import dev.xkmc.l2serial.serialization.type_cache.TypeInfo;
import dev.xkmc.l2serial.serialization.unified_processor.UnifiedCodec;
import dev.xkmc.l2serial.serialization.unified_processor.UnifiedContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@SuppressWarnings({"unchecked", "unsafe", "rawtypes"})
public class OptionalCodec extends GenericCodec {

	public static final Logger LOGGER = LogManager.getLogger();

	@Override
	public boolean predicate(Class<?> cls) {
		return Optional.class.isAssignableFrom(cls);
	}

	@Override
	public <C extends UnifiedContext<E, O, A>, E, O extends E, A extends E>
	Object deserializeValue(C ctx, E e, TypeInfo cls, @Nullable Object ans) throws Exception {
		TypeInfo com = cls.getGenericType(0);
		try {
			var obj = UnifiedCodec.deserializeValue(ctx, e, com, null);
			return Optional.ofNullable(obj);
		} catch (Throwable err) {
			LOGGER.info("L2Serial: Failed to deserialize " + cls);
			LOGGER.debug(err);
			return Optional.empty();
		}
	}

	@Override
	public <C extends UnifiedContext<E, O, A>, E, O extends E, A extends E>
	E serializeValue(C ctx, TypeInfo cls, @Nullable Object obj) throws Exception {
		TypeInfo com = cls.getGenericType(0);
		return UnifiedCodec.serializeValue(ctx, com, ((Optional) obj).orElse(null));
	}

}
