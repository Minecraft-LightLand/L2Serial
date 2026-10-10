package dev.xkmc.l2serial.serialization.type_cache;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves generic type arguments through class inheritance.
 *
 * <p>Supports non-generic subclasses that bind type arguments in
 * extends/implements, e.g. {@code class StringList extends ArrayList<String>},
 * as well as chains like {@code class Concrete extends Intermediate<String>}
 * where {@code Intermediate<T> extends ArrayList<T>}.</p>
 *
 * <p>The structural mapping for each (child, base) class pair is computed once
 * by walking the inheritance tree, then cached. Later lookups only substitute
 * the actual type arguments into the cached template, without touching
 * reflection over superclasses/interfaces again.</p>
 */
public final class InheritanceGenericResolver {

	private InheritanceGenericResolver() {
	}

	private record Key(Class<?> child, Class<?> base) {
	}

	/**
	 * Cached templates: for each (child, base) class pair, the type expressions
	 * of the base's type parameters in terms of the child's type variables
	 * (or concrete types). Empty means the child does not extend/implement the
	 * base with resolvable type arguments. Failures are cached too, so missed
	 * lookups don't re-walk the tree either.
	 */
	private static final Map<Key, Optional<Type[]>> TEMPLATE_CACHE = new ConcurrentHashMap<>();

	/**
	 * Resolve the i-th type argument of {@code base} for a usage of
	 * {@code child} with the given generic type.
	 *
	 * @param child       the raw class the field is declared with
	 * @param genericType the full generic type of the usage, may be null for raw usages
	 * @param base        the generic base class or interface to resolve against
	 * @param index       which type parameter of the base to resolve
	 * @return the resolved type, or null if there is no inheritance path, or the
	 * result is still an unbound type variable
	 */
	public static Type resolve(Class<?> child, Type genericType, Class<?> base, int index) {
		if (genericType instanceof ParameterizedType ptype && ptype.getRawType() == base) {
			// Fast path: field is declared exactly as the base, e.g. List<String>
			// vs List. No inheritance walk or cache lookup needed.
			Type[] args = ptype.getActualTypeArguments();
			if (index < 0 || index >= args.length) return null;
			Type ans = args[index];
			if (ans instanceof TypeVariable) return null;
			return ans;
		}
		Class<?> raw = child;
		Type[] actualArgs = null;
		if (genericType instanceof ParameterizedType ptype && ptype.getRawType() instanceof Class cls) {
			raw = cls;
			actualArgs = ptype.getActualTypeArguments();
		}
		Map<TypeVariable<?>, Type> bindings = new HashMap<>();
		if (actualArgs != null) {
			TypeVariable<?>[] vars = raw.getTypeParameters();
			for (int j = 0; j < vars.length && j < actualArgs.length; j++) {
				bindings.put(vars[j], actualArgs[j]);
			}
		}
		Type[] template = templateFor(raw, base);
		if (template == null || index < 0 || index >= template.length) return null;
		Type ans = substitute(template[index], bindings);
		if (ans instanceof TypeVariable) return null;
		return ans;
	}

	private static Type[] templateFor(Class<?> child, Class<?> base) {
		Key key = new Key(child, base);
		Optional<Type[]> cached = TEMPLATE_CACHE.get(key);
		if (cached == null) {
			Optional<Type[]> fresh = Optional.ofNullable(buildTemplate(child, base));
			Optional<Type[]> prev = TEMPLATE_CACHE.putIfAbsent(key, fresh);
			cached = prev != null ? prev : fresh;
		}
		return cached.orElse(null);
	}

	private static Type[] buildTemplate(Class<?> child, Class<?> base) {
		if (!base.isAssignableFrom(child)) return null;
		TypeVariable<?>[] params = base.getTypeParameters();
		Type[] ans = new Type[params.length];
		Map<TypeVariable<?>, Type> empty = new HashMap<>();
		for (int i = 0; i < params.length; i++) {
			Type found = search(child, base, params[i], empty);
			if (found == null) return null;
			ans[i] = found;
		}
		return ans;
	}

	private static Type search(Class<?> current, Class<?> target,
							   TypeVariable<?> targetVar,
							   Map<TypeVariable<?>, Type> map) {
		if (current == target) {
			return substitute(targetVar, map);
		}
		Type ans = searchInSuperType(current.getGenericSuperclass(), target, targetVar, map);
		if (ans != null) return ans;
		for (Type iface : current.getGenericInterfaces()) {
			ans = searchInSuperType(iface, target, targetVar, map);
			if (ans != null) return ans;
		}
		return null;
	}

	private static Type searchInSuperType(Type sup, Class<?> target,
										  TypeVariable<?> targetVar,
										  Map<TypeVariable<?>, Type> map) {
		if (sup instanceof Class raw) {
			if (!target.isAssignableFrom(raw)) return null;
			return search(raw, target, targetVar, map);
		}
		if (sup instanceof ParameterizedType ptype && ptype.getRawType() instanceof Class raw) {
			if (!target.isAssignableFrom(raw)) return null;
			Map<TypeVariable<?>, Type> childMap = new HashMap<>(map);
			Type[] args = ptype.getActualTypeArguments();
			TypeVariable<?>[] vars = raw.getTypeParameters();
			for (int j = 0; j < vars.length && j < args.length; j++) {
				childMap.put(vars[j], substitute(args[j], map));
			}
			return search(raw, target, targetVar, childMap);
		}
		return null;
	}

	private static Type substitute(Type t, Map<TypeVariable<?>, Type> map) {
		java.util.Set<Type> seen = new java.util.HashSet<>();
		while (t instanceof TypeVariable<?> var && seen.add(t)) {
			Type next = map.get(var);
			if (next == null) return t;
			t = next;
		}
		if (t instanceof ParameterizedType ptype && ptype.getRawType() instanceof Class) {
			Type[] args = ptype.getActualTypeArguments();
			Type[] resolved = new Type[args.length];
			boolean changed = false;
			for (int j = 0; j < args.length; j++) {
				resolved[j] = substitute(args[j], map);
				changed |= resolved[j] != args[j];
			}
			if (!changed) return t;
			return new ResolvedParamType(ptype.getRawType(), resolved, ptype.getOwnerType());
		}
		if (t instanceof GenericArrayType array) {
			Type com = array.getGenericComponentType();
			Type resolved = substitute(com, map);
			if (resolved == com) return t;
			return new ResolvedArrayType(resolved);
		}
		if (t instanceof WildcardType wild) {
			Type[] upper = wild.getUpperBounds();
			Type[] lower = wild.getLowerBounds();
			Type[] rupper = new Type[upper.length];
			Type[] rlower = new Type[lower.length];
			boolean changed = false;
			for (int j = 0; j < upper.length; j++) {
				rupper[j] = substitute(upper[j], map);
				changed |= rupper[j] != upper[j];
			}
			for (int j = 0; j < lower.length; j++) {
				rlower[j] = substitute(lower[j], map);
				changed |= rlower[j] != lower[j];
			}
			if (!changed) return t;
			return new ResolvedWildType(rupper, rlower);
		}
		return t;
	}

	private record ResolvedParamType(Type raw, Type[] args, Type owner) implements ParameterizedType {

		@Override
		public Type[] getActualTypeArguments() {
			return args.clone();
		}

		@Override
		public Type getRawType() {
			return raw;
		}

		@Override
		public Type getOwnerType() {
			return owner;
		}

	}

	private record ResolvedArrayType(Type component) implements GenericArrayType {

		@Override
		public Type getGenericComponentType() {
			return component;
		}

	}

	private record ResolvedWildType(Type[] upper, Type[] lower) implements WildcardType {

		@Override
		public Type[] getUpperBounds() {
			return upper.clone();
		}

		@Override
		public Type[] getLowerBounds() {
			return lower.clone();
		}

	}

}
