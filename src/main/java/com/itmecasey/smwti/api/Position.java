package com.itmecasey.smwti.api;

/** Where in the overlay a line goes: under the name, in the middle, or last. */
public enum Position {
    /** Right under the name and mod, for the one thing a block is known by. */
    HEAD,
    /** The main run of lines. */
    BODY,
    /** After everything else, for small print. */
    TAIL
}
