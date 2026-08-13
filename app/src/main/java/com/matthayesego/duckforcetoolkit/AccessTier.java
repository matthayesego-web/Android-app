package com.matthayesego.duckforcetoolkit;

import android.graphics.Color;

public enum AccessTier {
    GREEN(0, "Green • Member", Color.rgb(63, 185, 80)),
    ORANGE(1, "Orange • Elevated", Color.rgb(240, 180, 41)),
    RED(2, "Red • Leadership", Color.rgb(248, 81, 73)),
    BLACK(3, "Black • Leadership", Color.rgb(201, 209, 217)),
    GLOBAL(4, "Global • Leader/Co-leader", Color.rgb(121, 192, 255));

    public final int level;
    public final String label;
    private final int color;

    AccessTier(int level, String label, int color) {
        this.level = level;
        this.label = label;
        this.color = color;
    }

    public int displayColor() { return color; }
}
