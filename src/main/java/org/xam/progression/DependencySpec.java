package org.xam.progression;

public final class DependencySpec {
    public enum Kind { COUNT, PERCENT, MASTERED }
    public final String pathId;
    public final Kind kind;
    public final int value; // count o percent (0 for mastered)

    public DependencySpec(String pathId, Kind kind, int value) {
        this.pathId = pathId;
        this.kind = kind;
        this.value = value;
    }

    public static DependencySpec parse(String spec) {
        if (spec == null || spec.isEmpty()) {
            return null;
        }
        String[] parts = spec.split(":");
        String depPathId = parts[0];
        String amountStr = parts.length > 1 ? parts[1] : "mastered";

        if (amountStr.equalsIgnoreCase("mastered") || amountStr.equalsIgnoreCase("all")) {
            return new DependencySpec(depPathId, Kind.MASTERED, 0);
        }

        try {
            if (amountStr.endsWith("%")) {
                int pct = Integer.parseInt(amountStr.replace("%", ""));
                return new DependencySpec(depPathId, Kind.PERCENT, pct);
            } else {
                int count = Integer.parseInt(amountStr);
                return new DependencySpec(depPathId, Kind.COUNT, count);
            }
        } catch (NumberFormatException e) {
            return new DependencySpec(depPathId, Kind.MASTERED, 0);
        }
    }
}
