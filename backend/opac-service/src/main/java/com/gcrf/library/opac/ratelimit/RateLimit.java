package com.gcrf.library.opac.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RateLimit {
    /** Max requests in the window. */
    int value();
    /** Window in seconds. */
    int periodSeconds() default 1;
}
