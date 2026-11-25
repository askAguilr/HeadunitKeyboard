package dev.headunit.keyboard;

/** Geometry for a face centered inside its key's unchanged touch target. */
public enum KeycapShape {
    SQUARE, CIRCLE, WIDE, PILL;

    public float radius(float height) {
        return height * (this == CIRCLE || this == PILL ? .5f : .17f);
    }
}
