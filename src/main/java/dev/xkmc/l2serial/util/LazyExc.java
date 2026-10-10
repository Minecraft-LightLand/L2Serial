package dev.xkmc.l2serial.util;

public class LazyExc<T> {

	private Wrappers.ExcSup<T> factory;
	private T value;

	public LazyExc(Wrappers.ExcSup<T> factory) {
		this.factory = factory;
	}

	public synchronized T get() throws Exception {
		if (factory != null) {
			value = factory.get();
			factory = null;
		}
		assert value != null;
		return value;
	}

}
