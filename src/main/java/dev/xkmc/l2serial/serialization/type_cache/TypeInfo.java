package dev.xkmc.l2serial.serialization.type_cache;

import java.lang.reflect.*;

public class TypeInfo {

	public static TypeInfo of(Class<?> cls) {
		return new TypeInfo(cls, null);
	}

	public static TypeInfo of(Field field) {
		return new TypeInfo(field.getType(), field.getGenericType());
	}

	public static TypeInfo of(RecordComponent field) {
		return new TypeInfo(field.getType(), field.getGenericType());
	}

	private static TypeInfo of(Type type) {
		if (type instanceof Class cls) {
			return new TypeInfo(cls, null);
		}
		if (type instanceof ParameterizedType ptype && ptype.getRawType() instanceof Class cls) {
			return new TypeInfo(cls, ptype);
		}
		if (type instanceof GenericArrayType array) {
			TypeInfo sub = of(array.getGenericComponentType());
			return new TypeInfo(sub.cls.arrayType(), array);
		}
		if (type instanceof TypeVariable var) {
			throw new IllegalStateException("type parameter " + var + " cannot be resolved to a class. " +
					"It is likely a type variable of a generic class. " +
					"Declare the field with concrete type arguments, e.g. List<String> instead of List<T>");
		}
		if (type instanceof WildcardType wild) {
			Type[] upper = wild.getUpperBounds();
			if (upper.length > 0 && !(upper[0] instanceof WildcardType) && !(upper[0] instanceof TypeVariable)) {
				return of(upper[0]);
			}
			throw new IllegalStateException("wildcard type " + type + " cannot be resolved to a class. " +
					"Declare the field with concrete type arguments, e.g. List<String> instead of List<? extends T>");
		}
		throw new IllegalStateException("type parameter cannot be converted to class. Generic Type: " + type + ", class: " + type.getClass());
	}

	private final Class<?> cls;
	private final Type type;

	private TypeInfo(Class<?> cls, Type type) {
		this.cls = cls;
		this.type = type;
	}

	public Class<?> getAsClass() {
		return cls;
	}

	public TypeInfo getComponentType() {
		Type com = null;
		if (type instanceof GenericArrayType array)
			com = array.getGenericComponentType();
		return new TypeInfo(cls.getComponentType(), com);
	}

	public TypeInfo getGenericType(int i) {
		if (type instanceof ParameterizedType param) {
			Type[] types = param.getActualTypeArguments();
			if (types.length <= i)
				throw new IllegalArgumentException("generic type " + type + "has " + types.length + " fields, accessing index " + i);
			return TypeInfo.of(types[i]);
		}
		// Fallback for non-generic subclasses that bind type arguments in
		// extends/implements, e.g. class StringList extends ArrayList<String>.
		// Without a target base class this is ambiguous, so only support the
		// single-candidate case here. Codecs should use getGenericType(Class, int).
		TypeInfo resolved = findFirstGenericArgument(i);
		if (resolved != null) return resolved;
		throw new IllegalStateException("type parameter is missing. Type: " + type + ", Class: " + cls + ". " +
				"The class is not declared with type parameters, " +
				"and no generic superclass/interface with concrete type arguments was found. " +
				"Declare the field with explicit type arguments, e.g. List<String>, " +
				"or bind them in the subclass, e.g. class StringList extends ArrayList<String>");
	}

	/**
	 * Resolve the i-th type argument of {@code base} as seen from this type.
	 * Supports non-generic subclasses that bind type arguments in
	 * extends/implements, e.g. {@code class StringList extends ArrayList<String>},
	 * as well as chains like {@code class Concrete extends Intermediate<String>}
	 * where {@code Intermediate<T> extends ArrayList<T>}.
	 * The inheritance walk is cached per (child, base) class pair in
	 * {@link InheritanceGenericResolver}.
	 */
	public TypeInfo getGenericType(Class<?> base, int i) {
		TypeVariable<?>[] params = base.getTypeParameters();
		if (params.length <= i)
			throw new IllegalArgumentException("class " + base + " has " + params.length + " type parameters, accessing index " + i);
		Type resolved = InheritanceGenericResolver.resolve(cls, type, base, i);
		if (resolved == null)
			throw new IllegalStateException("cannot resolve type argument " + i + " of " + base +
					" from Type: " + type + ", Class: " + cls + ". " +
					"The class does not extend/implement " + base.getName() + " with concrete type arguments. " +
					"Declare the field with explicit type arguments, e.g. List<String>, " +
					"or bind them in the subclass, e.g. class StringList extends ArrayList<String>");
		try {
			return TypeInfo.of(resolved);
		} catch (IllegalStateException e) {
			throw new IllegalStateException("cannot resolve type argument " + i + " of " + base +
					" from Type: " + type + ", Class: " + cls +
					". The resolved type is still generic: " + resolved +
					". Declare the field with concrete type arguments.", e);
		}
	}

	public Object newInstance() throws Exception {
		return cls.getConstructor().newInstance();
	}

	private TypeInfo findFirstGenericArgument(int i) {
		java.util.Set<Type> visited = new java.util.HashSet<>();
		java.util.ArrayDeque<Type> queue = new java.util.ArrayDeque<>();
		if (cls.getGenericSuperclass() != null) queue.add(cls.getGenericSuperclass());
		queue.addAll(java.util.Arrays.asList(cls.getGenericInterfaces()));
		while (!queue.isEmpty()) {
			Type sup = queue.poll();
			if (!visited.add(sup)) continue;
			if (sup instanceof ParameterizedType ptype) {
				Type[] args = ptype.getActualTypeArguments();
				if (args.length > i) {
					try {
						return TypeInfo.of(args[i]);
					} catch (IllegalStateException ignored) {
						// type variable or wildcard that cannot be resolved; keep searching
					}
				}
				if (ptype.getRawType() instanceof Class raw) {
					if (raw.getGenericSuperclass() != null) queue.add(raw.getGenericSuperclass());
					queue.addAll(java.util.Arrays.asList(raw.getGenericInterfaces()));
				}
			} else if (sup instanceof Class raw) {
				if (raw.getGenericSuperclass() != null) queue.add(raw.getGenericSuperclass());
				queue.addAll(java.util.Arrays.asList(raw.getGenericInterfaces()));
			}
		}
		return null;
	}

	public boolean isArray() {
		return cls.isArray();
	}

	@Override
	public String toString() {
		return "{cls = " + cls + ", type = " + type + "}";
	}

	public ClassCache toCache() {
		return ClassCache.get(cls);
	}
}
