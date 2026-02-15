package TOTAL_AGGRO;

import battlecode.common.*;

public class Collector {

    public static void run(RobotController rc) throws GameActionException {
        int cheese = rc.getRawCheese();

        // 1. DISTRESS SIGNAL CHECK (New Militia Logic)
        // Check for King's 911 SOS before doing anything else
        Message[] squeaks = rc.readSqueaks(-1);
        MapLocation sosLoc = null;
        for (Message s : squeaks) {
            // Check for the 911 emergency code
            // Depending on your API, use getBytes() or a custom content check
            if (s.getBytes() == 911) { 
                sosLoc = s.getSource();
                break;
            }
        }

        // If King is in trouble and we aren't slow/heavy with cheese, go help!
        if (sosLoc != null && cheese < 15) {
             System.out.println("MILITIA_MODE: Defending King at " + sosLoc);
             // Priority: Attack enemies near King, otherwise move to King
             RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().getVisionRadiusSquared(), rc.getTeam().opponent());
             if (enemies.length > 0 && rc.canAttack(enemies[0].getLocation())) {
                 rc.attack(enemies[0].getLocation());
             } else {
                 Movement.moveTo(rc, sosLoc);
             }
             return; // Stop collection to defend
        }

        // 2. BYTECODE BUDGET CHECK (Original logic)
        if (Clock.getBytecodesLeft() < rc.getType().getBytecodeLimit() * 0.1) {
            if (cheese > 0) {
                MapLocation king = new MapLocation(rc.readSharedArray(0), rc.readSharedArray(1));
                Movement.moveTo(rc, king);
            }
            return;
        }

        // 3. NORMAL OPERATION
        if (cheese >= Config.DELIVERY_THRESHOLD) {
            deliver(rc);
        } else {
            collect(rc, squeaks); // Pass squeaks to avoid reading twice
        }
    }

    private static void collect(RobotController rc, Message[] squeaks) throws GameActionException {
        MapLocation me = rc.getLocation();
        int visionRange = rc.getType().getVisionRadiusSquared();

        MapInfo[] nearbyInfo = rc.senseNearbyMapInfos(me, visionRange);
        MapLocation nearest = null;
        int nearestDist = Integer.MAX_VALUE;

        for (MapInfo info : nearbyInfo) {
            // Squeak mine locations
            if (info.hasCheeseMine()) {
                MapLocation mineLoc = info.getMapLocation();
                // Using Type 3 for mines as per your original logic
                int squeakCode = (3 << 28) | (mineLoc.y << 16) | (mineLoc.x << 4);
                rc.squeak(squeakCode);
            }

            if (info.getCheeseAmount() > 0) {
                MapLocation loc = info.getMapLocation();
                int dist = me.distanceSquaredTo(loc);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = loc;
                }
            }
        }

        // Use freshest mine location from squeaks
        Message freshestMine = null;
        int newestRound = 0;
        for (Message msg : squeaks) {
            if (msg.getSenderID() == rc.getID()) continue;
            if (msg.getRound() > newestRound) {
                int type = (msg.getBytes() >> 28) & 0xF;
                if (type == 3) {
                    freshestMine = msg;
                    newestRound = msg.getRound();
                }
            }
        }

        if (freshestMine != null) {
            MapLocation mineSource = freshestMine.getSource();
            int dist = me.distanceSquaredTo(mineSource);
            if (nearest == null || dist < nearestDist) {
                nearest = mineSource;
            }
        }

        if (nearest != null) {
            if (rc.canPickUpCheese(nearest)) {
                rc.pickUpCheese(nearest);
            } else {
                Movement.moveTo(rc, nearest);
            }
        } else {
            // Search rotation: Always turn to scan if nothing found
            if (rc.canTurn()) rc.turn(rc.getDirection().rotateRight());
        }
    }

    private static void deliver(RobotController rc) throws GameActionException {
        MapLocation king = new MapLocation(rc.readSharedArray(0), rc.readSharedArray(1));
        int dist = rc.getLocation().distanceSquaredTo(king);

        if (dist <= 9) {
            int amt = rc.getRawCheese();
            if (rc.canTransferCheese(king, amt)) {
                rc.transferCheese(king, amt);
                return;
            }
        }
        Movement.moveTo(rc, king);
    }
}