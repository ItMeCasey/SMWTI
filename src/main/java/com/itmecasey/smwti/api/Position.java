package com.itmecasey.smwti.api;

/** Where in the overlay a line goes: beside the name, under it, in the middle, or last. */
public enum Position {
    /** To the right of the name, on the same row, for a small mark such as the tool a block needs. */
    BADGE,
    /** Right under the name and mod, for the one thing a block is known by. */
    HEAD,
    /** The main run of lines. */
    BODY,
    /** After everything else, for small print. */
    TAIL
}
