package dev.xkmc.l2serial.util;

public class LazyExc<T> {

	private final Wrappers.ExcSup<T> factory;
	private volatile T value;

	public LazyExc(Wrappers.ExcSup<T> factory) {
		this.factory = factory;
	}

	public T get() throws Exception {
		if (value != null) return value;
		synchronized (this) {
			if (value == null)
				value = factory.get();
		}
		return value;
	}

}
