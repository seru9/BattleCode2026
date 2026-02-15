package examplefuncsplayer;

/**
 * Plik konfiguracyjny - tutaj zmieniaj parametry bota.
 */
public class Config {
    // ================================================================
    // COMBAT CONFIG
    // ================================================================
    // TUNED: More aggressive combat
    public static final int ENHANCED_ATTACK_CHEESE = 16; // Was 8, now 16 for 14 damage
    public static final int ENHANCED_THRESHOLD = 300; // Was 500, now 300 for more enhanced attacks

    // ================================================================
    // POPULATION CONFIG
    // ================================================================
    // TUNED: More combat power
    public static final int INITIAL_SPAWN_COUNT = 15; // Was 12, now 15
    public static final int MAX_SPAWN_COUNT = 25; // Was 20, now 25
    public static final int COLLECTOR_MINIMUM = 5; // Was 4, now 5

    // ================================================================
    // MOVEMENT CONFIG
    // ================================================================
    public static final int POSITION_HISTORY_SIZE = 5; // RANGE: 3-7
    public static final int FORCED_MOVEMENT_THRESHOLD = 3; // RANGE: 2-5

    // ================================================================
    // ECONOMY CONFIG
    // ================================================================
    // TUNED: Lower threshold = more frequent deliveries
    public static final int DELIVERY_THRESHOLD = 5; // Was 10, now 5 for faster deliveries
    public static final int KING_CHEESE_RESERVE = 100; // RANGE: 50-200
}