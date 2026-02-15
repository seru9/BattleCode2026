package TOTAL_AGGRO;

import battlecode.common.*;

public class Attacker {

    public static void run(RobotController rc) throws GameActionException {
        if (!rc.isActionReady()) return;

        MapLocation me = rc.getLocation();
        int visionRange = rc.getType().getVisionRadiusSquared();
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(visionRange);

        RobotInfo targetKing = null;
        RobotInfo bestBabyRat = null;

        // 1. IDENTIFY TARGETS (Priority: Enemy King > Baby Rat)
        for (RobotInfo r : nearbyRobots) {
            if (r.getTeam() != rc.getTeam()) {
                if (r.getType() == UnitType.RAT_KING) {
                    targetKing = r; // Found the big prize
                } else if (bestBabyRat == null || r.getRawCheeseAmount() > bestBabyRat.getRawCheeseAmount()) {
                    bestBabyRat = r; // Target-rich baby rats
                }
            }
        }

        // 2. ATTACK LOGIC: THE KING SLAYER
        if (targetKing != null) {
            MapLocation kingCenter = targetKing.getLocation();
            MapLocation bestTileToHit = null;

            // The King is 3x3. We check all 9 tiles to find one we can reach.
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    MapLocation tile = new MapLocation(kingCenter.x + dx, kingCenter.y + dy);
                    if (rc.canAttack(tile)) {
                        bestTileToHit = tile;
                        break;
                    }
                }
            }

            if (bestTileToHit != null) {
                // If we have surplus cheese, burn the King down fast
                int extraDamage = (rc.getGlobalCheese() > 1000) ? Config.ENHANCED_ATTACK_CHEESE : 0;
                rc.attack(bestTileToHit, extraDamage);
                return;
            } else {
                // We see the king but can't reach him. RUSH!
                Movement.moveTo(rc, kingCenter);
                return;
            }
        }

        // 3. SECONDARY TARGET: BABY RATS
        // If we can't see/reach a king, clear out the defenders
        if (bestBabyRat != null) {
            if (rc.canAttack(bestBabyRat.getLocation())) {
                rc.attack(bestBabyRat.getLocation());
                return;
            } else {
                Movement.moveTo(rc, bestBabyRat.getLocation());
                return;
            }
        }

        // 4. COORDINATION: Use Radar or Shared Array
        // Check King's radar first (Shared Array 4, 5)
        int lastSeenRound = rc.readSharedArray(6);
        if (rc.getRoundNum() - lastSeenRound < 20) {
            MapLocation radarTarget = new MapLocation(rc.readSharedArray(4), rc.readSharedArray(5));
            Movement.moveTo(rc, radarTarget);
        } else {
            // No radar? Head to the predicted enemy king location
            MapLocation enemyKingHome = new MapLocation(rc.readSharedArray(2), rc.readSharedArray(3));
            Movement.moveTo(rc, enemyKingHome);
        }

        // 5. SCANNING: Don't stay blind
        if (nearbyRobots.length == 0 && rc.canTurn()) {
            rc.turn(rc.getDirection().rotateRight());
        }
    }
}