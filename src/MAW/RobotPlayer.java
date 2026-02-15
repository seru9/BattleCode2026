package MAW;

import battlecode.common.*;

public class RobotPlayer {

    // ========== CONFIGURATION ==========
    private static final int ENHANCED_ATTACK_CHEESE = 16;
    private static final int ENHANCED_THRESHOLD = 800;
    private static final int KING_CHEESE_RESERVE = 150;
    
    // Message Content Codes
    private static final int CODE_CAT_FOUND = 1; 

    // Shared Array Indices
    private static final int KING_X = 0;
    private static final int KING_Y = 1;
    private static final int CAT_RADAR_X = 4;
    private static final int CAT_RADAR_Y = 5;
    private static final int CAT_RADAR_ROUND = 6;

    public static void run(RobotController rc) throws GameActionException {
        while (true) {
            try {
                if (rc.getType() == UnitType.RAT_KING) {
                    runKing(rc);
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

    private static void runKing(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation();
        rc.writeSharedArray(KING_X, me.x);
        rc.writeSharedArray(KING_Y, me.y);

        // 360 RADAR - Only Kings can see all around
        RobotInfo[] enemies = rc.senseNearbyRobots();
        for (RobotInfo e : enemies) {
            if (e.getType() == UnitType.CAT) {
                rc.writeSharedArray(CAT_RADAR_X, e.getLocation().x);
                rc.writeSharedArray(CAT_RADAR_Y, e.getLocation().y);
                rc.writeSharedArray(CAT_RADAR_ROUND, rc.getRoundNum());
                break;
            }
        }

        // SPAWNING
        if (rc.isActionReady() && rc.getGlobalCheese() > rc.getCurrentRatCost() + KING_CHEESE_RESERVE) {
            for (Direction dir : Direction.allDirections()) {
                MapLocation spawnLoc = me.add(dir).add(dir); 
                if (rc.canBuildRat(spawnLoc)) {
                    rc.buildRat(spawnLoc);
                    break;
                }
            }
        }
    }

    private static void runBabyRat(RobotController rc) throws GameActionException {
        // 1. LISTEN: Use getSource() to find where the cat is
        int roundnum = rc.getRoundNum();
        Message[] squeaks = rc.readSqueaks(roundnum);
        MapLocation catPing = null;
        for (Message s : squeaks) {
            // Note: Use s.getContent() or check the first byte if using getBytes()
            // Using s.getContent() based on standard Battlecode int squeaks
            if (s.getBytes() == CODE_CAT_FOUND) {
                catPing = s.getSource(); // This is the X,Y of the squeak!
                break;
            }
        }

        // 2. SENSE
        RobotInfo[] visible = rc.senseNearbyRobots();
        RobotInfo localCat = null;
        for (RobotInfo r : visible) {
            if (r.getType() == UnitType.CAT) {
                localCat = r;
                rc.squeak(CODE_CAT_FOUND);
                break;
            }
        }

        // 3. DECIDE
        if (localCat != null) {
            attackCat(rc, localCat);
        } else if (catPing != null) {
            moveTo(rc, catPing); // Rush to the sound of the fight!
        } else {
            if (rc.getID() % 2 == 1) runCollector(rc);
            else runAttacker(rc);
        }
    }

    private static void attackCat(RobotController rc, RobotInfo cat) throws GameActionException {
        MapLocation catLoc = cat.getLocation();
        Direction toCat = rc.getLocation().directionTo(catLoc);
        MapLocation adjTile = rc.getLocation().add(toCat);

        if (rc.canPlaceCatTrap(adjTile)) {
            rc.placeCatTrap(adjTile);
        } else if (rc.canAttack(catLoc)) {
            int extra = (rc.getGlobalCheese() > ENHANCED_THRESHOLD) ? ENHANCED_ATTACK_CHEESE : 0;
            rc.attack(catLoc, extra);
        } else {
            moveTo(rc, catLoc);
        }
    }

    private static void runAttacker(RobotController rc) throws GameActionException {
        int lastSeen = rc.readSharedArray(CAT_RADAR_ROUND);
        if (rc.getRoundNum() - lastSeen < 15) {
            moveTo(rc, new MapLocation(rc.readSharedArray(CAT_RADAR_X), rc.readSharedArray(CAT_RADAR_Y)));
        } else if (rc.canTurn()) {
            rc.turn(rc.getDirection().rotateRight());
        }
    }

    private static void runCollector(RobotController rc) throws GameActionException {
        if (rc.getRawCheese() >= 10) {
            moveTo(rc, new MapLocation(rc.readSharedArray(KING_X), rc.readSharedArray(KING_Y)));
            // Actual transfer call
            MapLocation kingLoc = new MapLocation(rc.readSharedArray(KING_X), rc.readSharedArray(KING_Y));
            if (rc.getLocation().isWithinDistanceSquared(kingLoc, 2) && rc.canTransferCheese(kingLoc, rc.getRawCheese())) {
                rc.transferCheese(kingLoc, rc.getRawCheese());
            }
        } else {
            MapInfo[] tiles = rc.senseNearbyMapInfos();
            MapLocation cheeseLoc = null;
            for (MapInfo t : tiles) {
                if (t.getCheeseAmount() > 0) {
                    cheeseLoc = t.getMapLocation();
                    break;
                }
            }
            if (cheeseLoc != null) {
                if (rc.canPickUpCheese(cheeseLoc)) rc.pickUpCheese(cheeseLoc);
                else moveTo(rc, cheeseLoc);
            } else if (rc.canTurn()) {
                rc.turn(rc.getDirection().rotateRight());
            }
        }
    }

    private static void moveTo(RobotController rc, MapLocation target) throws GameActionException {
        if (!rc.isMovementReady()) return;
        Direction dir = rc.getLocation().directionTo(target);
        if (dir == Direction.CENTER) return;

        if (rc.getDirection() == dir) {
            if (rc.canMoveForward()) rc.moveForward();
            else if (rc.canTurn()) rc.turn(dir.rotateLeft());
        } else if (rc.canTurn()) {
            rc.turn(dir);
        }
    }
}