package com.gcrf.library.common.security.context;

/**
 * SecurityContext: data scope levels.
 * Ordinals matter: higher ordinal subsumes lower (REGION ⊇ SCHOOL ⊇ GRADE ⊇ CLASS ⊇ SELF).
 */
public enum Scope {
    SELF,
    CLASS,
    GRADE,
    SCHOOL,
    REGION;

    public boolean covers(Scope other) {
        return this.ordinal() >= other.ordinal();
    }
}
