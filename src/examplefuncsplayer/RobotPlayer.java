package examplefuncsplayer;

import battlecode.common.*;

/**
 * ratbot5 - Built from scratch using Battlecode javadoc
 *
 * <p>Focus: Intelligent movement (no freeze in late rounds) Built with: Proper API usage, grouped
 * configuration, javadoc as reference
 */
public class RobotPlayer {

    private static int myRole = -1; // 0=attacker, 1=collector

    public static void run(RobotController rc) throws GameActionException {
        while (true) {
            try {
                if (rc.getType().isRatKingType()) {
                    King.run(rc);
                } else {
                    runBabyRat(rc);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }

    private static void runBabyRat(RobotController rc) throws GameActionException {
        int round = rc.getRoundNum();

        // Check if being thrown or carried (can't act)
        if (rc.isBeingThrown() || rc.isBeingCarried()) {
            return; // Wait until released
        }

        // Assign role once
        if (myRole == -1) {
            myRole = rc.getID() % 2; // 0=attacker, 1=collector
        }

        // JAVADOC: Use getHealth() to check if low HP
        int health = rc.getHealth();
        if (health < 30 && myRole == 1) {
            // Low health collector - disintegrate near our king to drop cheese
            MapLocation ourKing = new MapLocation(rc.readSharedArray(0), rc.readSharedArray(1));
            if (rc.getLocation().distanceSquaredTo(ourKing) <= 9) {
                rc.disintegrate(); // Drop cheese for king to collect
                return;
            }
        }

        // Visual debugging (comprehensive)
        rc.setIndicatorString((myRole == 0 ? "ATK" : "COL") + " HP:" + health);
        rc.setIndicatorDot(rc.getLocation(), health > 50 ? 0 : 255, health > 50 ? 255 : 0, 0);

        // Timeline marker on major events
        if (round % 100 == 0) {
            rc.setTimelineMarker("Round " + round, 100, 100, 255);
        }

        // Draw indicator line to target
        if (myRole == 0) {
            MapLocation enemyKing = new MapLocation(rc.readSharedArray(2), rc.readSharedArray(3));
            rc.setIndicatorLine(rc.getLocation(), enemyKing, 255, 0, 0);
        } else if (rc.getRawCheese() >= Config.DELIVERY_THRESHOLD) {
            MapLocation ourKing = new MapLocation(rc.readSharedArray(0), rc.readSharedArray(1));
            rc.setIndicatorLine(rc.getLocation(), ourKing, 0, 255, 0);
        }

        // Check for 2nd king formation
        if (round > 50 && myRole == 1 && rc.canBecomeRatKing()) {
            rc.becomeRatKing();
            return;
        }

        if (myRole == 0) {
            Attacker.run(rc);
        } else {
            Collector.run(rc);
        }
    }
}