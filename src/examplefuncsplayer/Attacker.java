package examplefuncsplayer;

import battlecode.common.*;

public class Attacker {

    public static void run(RobotController rc) throws GameActionException {
        // DEBUG: First line of runAttacker
        if (rc.getRoundNum() % 50 == 0) {
            System.out.println("ATK_START:" + rc.getRoundNum() + ":" + rc.getID());
        }

        if (!rc.isActionReady()) {
            if (rc.getRoundNum() % 50 == 0) {
                System.out.println("ATK_NOT_READY:" + rc.getRoundNum() + ":" + rc.getID());
            }
            return;
        }

        // Bytecode check
        if (Clock.getBytecodesLeft() < rc.getType().getBytecodeLimit() * 0.1) {
            if (rc.getRoundNum() % 50 == 0) {
                System.out.println("ATK_LOW_BYTECODE:" + rc.getRoundNum() + ":" + rc.getID());
            }
            MapLocation enemyKing = new MapLocation(rc.readSharedArray(2), rc.readSharedArray(3));
            Movement.moveTo(rc, enemyKing);
            return;
        }

        MapLocation me = rc.getLocation();

        // Ratnapping - throw if carrying
        RobotInfo carrying = rc.getCarrying();
        if (carrying != null) {
            // JAVADOC: Check if we can drop rat instead of throwing
            MapLocation ourKing = new MapLocation(rc.readSharedArray(0), rc.readSharedArray(1));
            int distToKing = me.distanceSquaredTo(ourKing);

            // If at our king, drop rat (they take damage when dropped)
            if (distToKing <= 4) {
                Direction toKing = me.directionTo(ourKing);
                if (rc.canDropRat(toKing)) {
                    rc.dropRat(toKing);
                    return;
                }
            }

            // Otherwise throw toward king
            if (rc.canThrowRat()) {
                Direction toKing = me.directionTo(ourKing);
                if (rc.getDirection() != toKing && rc.canTurn()) {
                    rc.turn(toKing);
                } else {
                    rc.throwRat();
                }
                return;
            }
        }
        int defenseSignal = rc.readSharedArray(4);
    
        if (defenseSignal == 1) {
            // Odczytaj pozycję naszego króla (indeksy 0 i 1)
            MapLocation ourKing = new MapLocation(rc.readSharedArray(0), rc.readSharedArray(1));
            
            // Jeśli jesteśmy daleko, wracamy bronić
            if (rc.getLocation().distanceSquaredTo(ourKing) > 16) {
                Movement.moveTo(rc, ourKing);
                rc.setIndicatorString("DEFENDING KING");
                return; // Przerywamy resztę logiki ataku, żeby bronić
            }
        }
        int visionRange = rc.getType().getVisionRadiusSquared();
        RobotInfo[] enemies = rc.senseNearbyRobots(visionRange, rc.getTeam().opponent());

        if (rc.getRoundNum() % 50 == 0) {
            System.out.println(
                    "ATK_ENEMIES:" + rc.getRoundNum() + ":" + rc.getID() + ":count=" + enemies.length);
        }

        // Find targets: wounded to ratnap, best to attack
        RobotInfo bestTarget = null;
        RobotInfo ratnap = null;
        int maxCheese = 0;

        for (RobotInfo enemy : enemies) {
            if (enemy.getType().isBabyRatType()) {
                if (enemy.getHealth() < 50 && ratnap == null) {
                    ratnap = enemy;
                }

                int cheese = enemy.getRawCheeseAmount();
                if (cheese > maxCheese || bestTarget == null) {
                    maxCheese = cheese;
                    bestTarget = enemy;
                }
            }
        }

        // Ratnap wounded
        if (ratnap != null && carrying == null && rc.canCarryRat(ratnap.getLocation())) {
            rc.carryRat(ratnap.getLocation());
            return;
        }

        // Attack with game mode adaptation
        boolean coop = rc.isCooperation();

        if (bestTarget != null) {
            if (rc.getRoundNum() % 50 == 0) {
                System.out.println(
                        "ATK_TARGET:"
                                + rc.getRoundNum()
                                + ":"
                                + rc.getID()
                                + ":canAttack="
                                + rc.canAttack(bestTarget.getLocation()));
            }

            if (rc.canAttack(bestTarget.getLocation())) {
                if (!coop && rc.getGlobalCheese() > Config.ENHANCED_THRESHOLD) {
                    rc.attack(bestTarget.getLocation(), Config.ENHANCED_ATTACK_CHEESE);
                } else {
                    rc.attack(bestTarget.getLocation());
                }
                System.out.println("ATTACK:" + rc.getRoundNum() + ":" + rc.getID());
                return;
            }
        } else {
            if (rc.getRoundNum() % 50 == 0) {
                System.out.println("ATK_NO_TARGET:" + rc.getRoundNum() + ":" + rc.getID());
            }
        }

        // Attack king with proper distance AND squeak location
        for (RobotInfo enemy : enemies) {
            if (enemy.getType().isRatKingType()) {
                MapLocation kingCenter = enemy.getLocation();

                // Attack all 9 king tiles manually
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        MapLocation tile = new MapLocation(kingCenter.x + dx, kingCenter.y + dy);
                        if (rc.canAttack(tile)) {
                            rc.attack(tile);
                            return;
                        }
                    }
                }

                // Squeak king location
                try {
                    int squeak = (1 << 28) | (kingCenter.y << 16) | (kingCenter.x << 4);
                    rc.squeak(squeak);
                } catch (Exception e) {
                    // Squeak failed
                }
            }
        }

        // No enemies visible - check squeaks for enemy king (COORDINATION!)
        try {
            Message[] squeaks = rc.readSqueaks(-1);
            for (Message msg : squeaks) {
                if (msg.getSenderID() != rc.getID()) {
                    int bytes = msg.getBytes();
                    int type = (bytes >> 28) & 0xF;
                    if (type == 1) { // Enemy king squeak
                        int x = (bytes >> 4) & 0xFFF;
                        int y = (bytes >> 16) & 0xFFF;
                        MapLocation squeakedKing = new MapLocation(x, y);
                        Movement.moveTo(rc, squeakedKing);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            // Squeak failed
        }

        // Rush enemy king from shared array
        MapLocation enemyKing = new MapLocation(rc.readSharedArray(2), rc.readSharedArray(3));
        Movement.moveTo(rc, enemyKing);
    }
}