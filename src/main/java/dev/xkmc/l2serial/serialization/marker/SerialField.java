package dev.xkmc.l2serial.serialization.marker;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface SerialField {

	boolean toClient() default true;

	boolean toTracking() default true;

	/**
	 * Extra properties allowing finer control of serialization filter
	 */
	String type() default "";

}
