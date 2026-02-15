package TOTAL_AGGRO;

import battlecode.common.*;

public class GuardianRat {

    // Radius constants
    private static final int TRAP_RING_RADIUS = 4; // Distance from king to place traps
    private static final int MAX_CHASE_DIST = 20;   // Don't chase enemies too far from the king
    private static final int TRAP_COST = 30;

    public static void run(RobotController rc) throws GameActionException {
        // 1. Get King's Location from Shared Array
        MapLocation kingLoc = new MapLocation(rc.readSharedArray(0), rc.readSharedArray(1));
        MapLocation me = rc.getLocation();
        int distToKing = me.distanceSquaredTo(kingLoc);

        // 2. SEARCH FOR THREATS
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().getVisionRadiusSquared(), rc.getTeam().opponent());
        RobotInfo closestThreat = null;

        for (RobotInfo enemy : enemies) {
            // Only care about enemies relatively near our King
            if (enemy.getLocation().distanceSquaredTo(kingLoc) < MAX_CHASE_DIST) {
                closestThreat = enemy;
                break; // Found a target
            }
        }

        // 3. COMBAT / CHASE LOGIC
        if (closestThreat != null) {
            MapLocation threatLoc = closestThreat.getLocation();
            if (rc.canAttack(threatLoc)) {
                // Use enhanced damage to protect the king
                int biteCheese = (rc.getGlobalCheese() > 400) ? 20 : 0;
                rc.attack(threatLoc, biteCheese);
                return;
            } else {
                Movement.moveTo(rc, threatLoc);
                return;
            }
        }

        // 4. DEFENSIVE TRAPPING (If no immediate threat)
        // We want to place traps in a circle around the king
        if (rc.isActionReady() && rc.getGlobalCheese() > TRAP_COST + 100) {
            // Check adjacent tiles to see if we can place a defensive trap
            for (Direction dir : Direction.allDirections()) {
                MapLocation potentialTrap = me.add(dir);
                // Check if this tile is at the right distance from the King to be part of the "Trap Ring"
                if (potentialTrap.distanceSquaredTo(kingLoc) <= TRAP_RING_RADIUS + 2) {
                    if (rc.canPlaceRatTrap(potentialTrap)) {
                        rc.placeRatTrap(potentialTrap);
                        return; // Action used
                    }
                }
            }
        }

        // 5. POSITIONING (Stay in the perimeter)
        if (distToKing > 8) {
            // Too far, go back to King
            Movement.moveTo(rc, kingLoc);
        } else if (distToKing < 2) {
            // Too close (don't block spawning), step back
            Direction away = kingLoc.directionTo(me);
            if (rc.canMove(away)) rc.move(away);
        } else {
            // In the "Guard Zone" - Rotate to scan 360 degrees
            if (rc.canTurn()) {
                rc.turn(rc.getDirection().rotateRight().rotateRight());
            }
        }
    }
}