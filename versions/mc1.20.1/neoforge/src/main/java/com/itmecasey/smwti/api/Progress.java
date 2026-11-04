package com.itmecasey.smwti.api;

import java.util.OptionalDouble;
import net.minecraft.world.inventory.ContainerData;

/**
 * Helpers for the common ways a machine keeps its progress, for use in a {@link ProgressSource}.
 * Many machines expose two numbers (time done and time needed) through a {@link ContainerData}, the
 * same object their screens read.
 */
public final class Progress {
    private Progress() {
    }

    /** {@code done / total} limited to 0 to 1; empty when {@code total} is not above zero or nothing is done. */
    public static OptionalDouble ratio(double done, double total) {
        if (total <= 0 || done <= 0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(Math.min(1.0, done / total));
    }

    /** The progress held in two slots of a container data object: the time done and the time needed. */
    public static OptionalDouble fromData(ContainerData data, int doneIndex, int totalIndex) {
        return ratio(data.get(doneIndex), data.get(totalIndex));
    }
}
