package examplefuncsplayer2;

import battlecode.common.*;

public class Collector {

    public static void run(RobotController rc) throws GameActionException {
        int cheese = rc.getRawCheese();

        if (rc.getRoundNum() % 50 == 0) {
            System.out.println("COL:" + rc.getRoundNum() + ":" + rc.getID() + ":cheese=" + cheese + " threshold=" + Config.DELIVERY_THRESHOLD);
        }

        // Check bytecode budget
        if (Clock.getBytecodesLeft() < rc.getType().getBytecodeLimit() * 0.1) {
            // Emergency mode - just deliver what we have
            if (cheese > 0) {
                MapLocation king = new MapLocation(rc.readSharedArray(0), rc.readSharedArray(1));
                Movement.moveTo(rc, king);
            }
            return;
        }

        if (cheese >= Config.DELIVERY_THRESHOLD) {
            System.out.println("DELIVER_MODE:" + rc.getRoundNum() + ":" + rc.getID());
            deliver(rc);
        } else {
            collect(rc);
        }
    }

    private static void collect(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation();
        int visionRange = rc.getType().getVisionRadiusSquared();

        // Scan area efficiently using senseNearbyMapInfos
        MapInfo[] nearbyInfo = rc.senseNearbyMapInfos(me, visionRange);
        MapLocation nearest = null;
        int nearestDist = Integer.MAX_VALUE;

        for (MapInfo info : nearbyInfo) {
            // Squeak mine locations when found
            if (info.hasCheeseMine()) {
                try {
                    MapLocation mineLoc = info.getMapLocation();
                    int squeak = (3 << 28) | (mineLoc.y << 16) | (mineLoc.x << 4);
                    rc.squeak(squeak);
                } catch (Exception e) {
                    // Squeak failed
                }
            }

            // Find nearest cheese
            if (info.getCheeseAmount() > 0) {
                MapLocation loc = info.getMapLocation();
                int dist = me.distanceSquaredTo(loc);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = loc;
                }
            }
        }

        // READ squeaks with full metadata
        try {
            Message[] squeaks = rc.readSqueaks(-1);
            Message freshestMine = null;
            int newestRound = 0;

            for (Message msg : squeaks) {
                // Skip own squeaks
                if (msg.getSenderID() == rc.getID()) continue;

                // Find freshest mine squeak
                if (msg.getRound() > newestRound) {
                    int bytes = msg.getBytes();
                    int type = (bytes >> 28) & 0xF;
                    if (type == 3) {
                        freshestMine = msg;
                        newestRound = msg.getRound();
                    }
                }
            }

            // Use freshest mine location
            if (freshestMine != null) {
                // Go to source (where ally found mine)
                MapLocation mineSource = freshestMine.getSource();
                int dist = me.distanceSquaredTo(mineSource);
                if (nearest == null || dist < nearestDist) {
                    nearest = mineSource;
                    nearestDist = dist;
                }
            }
        } catch (Exception e) {
            // Squeak reading failed
        }

        // Collect cheese
        if (nearest != null) {
            if (rc.canPickUpCheese(nearest)) {
                rc.pickUpCheese(nearest);
                System.out.println(
                        "PICKUP:" + rc.getRoundNum() + ":" + rc.getID() + ":now=" + rc.getRawCheese());
            } else {
                if (rc.getRoundNum() % 50 == 0) {
                    System.out.println(
                            "MOVE_TO_CHEESE:" + rc.getRoundNum() + ":" + rc.getID() + ":dist=" + nearestDist);
                }
                Movement.moveTo(rc, nearest);
            }
        } else {
            // No cheese - explore
            MapLocation center = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
            Movement.moveTo(rc, center);
        }
    }

    private static void deliver(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation();
        MapLocation king = new MapLocation(rc.readSharedArray(0), rc.readSharedArray(1));

        int dist = me.distanceSquaredTo(king);

        // DEBUG: Always log when delivering
        if (rc.getRoundNum() % 10 == 0) {
            System.out.println("DELIVER:" + rc.getRoundNum() + ":" + rc.getID() + ":dist=" + dist + " cheese=" + rc.getRawCheese());
        }

        // Transfer if in range
        if (dist <= 9) {
            int amt = rc.getRawCheese(); // Get amount BEFORE transfer
            if (rc.canTransferCheese(king, amt)) {
                rc.transferCheese(king, amt);
                System.out.println("TRANSFER:" + rc.getRoundNum() + ":" + rc.getID() + ":amt=" + amt);
                return;
            } else {
                System.out.println("TRANSFER_FAIL:" + rc.getRoundNum() + ":" + rc.getID() + ":canTransfer=false");
            }
        }

        // Move toward king
        Movement.moveTo(rc, king);
    }
}