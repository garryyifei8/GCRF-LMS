package com.gcrf.library.common.security.annotation;

import com.gcrf.library.common.security.context.Scope;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireScope {
    Scope value();
}
