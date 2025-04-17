package data;

/**
 * 1 -> Stamina, 2 -> Perfection, 3 -> kill count, 4 -> Mechanical, 5 -> Restriction, 6 -> Speed
 */
public enum TaskType {
    STAMINA(1),
    PERFECTION(2),
    KILL_COUNT(3),
    MECHANICAL(4),
    RESTRICTION(5),
    SPEED(6);

    private final int value;

    TaskType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static TaskType fromValue(int value) {
        for (TaskType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }
}
