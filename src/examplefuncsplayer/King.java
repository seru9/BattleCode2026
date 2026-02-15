package examplefuncsplayer;

import battlecode.common.*;

public class King {

    private static int spawnCount = 0;
    private static int trapCount = 0;
    private static int lastHealth = -1;
    public static void run(RobotController rc) throws GameActionException {
        int round = rc.getRoundNum();
        int cheese = rc.getGlobalCheese();
        MapLocation me = rc.getLocation();

        // JAVADOC: Use getHealth() to check king HP
        int kingHP = rc.getHealth();

        // Emergency: If king HP critical and no hope, resign
        if (kingHP < 50 && cheese < 100 && spawnCount < 5) {
            // something here
        }

        if (round % 50 == 0) {
            System.out.println(
                    "KING:" + round + ":cheese=" + cheese + " HP=" + kingHP + " spawned=" + spawnCount);
        }

        // Write position to shared array
        rc.writeSharedArray(0, me.x);
        rc.writeSharedArray(1, me.y);

        // Calculate enemy king (round 1 only)
        if (round == 1) {
            rc.writeSharedArray(2, rc.getMapWidth() - 1 - me.x);
            rc.writeSharedArray(3, rc.getMapHeight() - 1 - me.y);
        }

        // Spawn initial rats
        if (spawnCount < Config.INITIAL_SPAWN_COUNT) {
            int cost = rc.getCurrentRatCost();
            if (cheese > cost + Config.KING_CHEESE_RESERVE) {
                spawnRat(rc);
                System.out.println("SPAWN:" + round + ":rat#" + spawnCount);
            }
        }
        // Instant replacement if low collectors
        else if (spawnCount < Config.MAX_SPAWN_COUNT) {
            int collectors = countCollectors(rc);
            if (collectors < Config.COLLECTOR_MINIMUM) {
                int cost = rc.getCurrentRatCost();
                if (cheese > cost + Config.KING_CHEESE_RESERVE) {
                    spawnRat(rc);
                    System.out.println("REPLACE:" + round + ":rat#" + spawnCount);
                }
            }
        }
            
        // Place defensive traps and dirt walls
        if (spawnCount >= 10 && cheese > 300) {
            for (Direction dir : Direction.allDirections()) {
                MapLocation loc = me.add(dir).add(dir);

                // JAVADOC: Place dirt walls first (4 walls max)
                if (rc.getDirt() < 4 && rc.canPlaceDirt(loc)) {
                    rc.placeDirt(loc);
                    return;
                }

                // Rat traps (anti-rat, 5 max)
                if (trapCount < 5 && rc.canPlaceRatTrap(loc)) {
                    rc.placeRatTrap(loc);
                    trapCount++;
                    return;
                }

                // Cat traps (anti-cat, 5 more)
                if (trapCount < 10 && rc.canPlaceCatTrap(loc)) {
                    rc.placeCatTrap(loc);
                    trapCount++;
                    return;
                }
            }
        }

        // King movement - mobile king harder to kill
        RobotInfo[] threats = rc.senseNearbyRobots(25, Team.NEUTRAL);
        RobotInfo[] enemies = rc.senseNearbyRobots(25, rc.getTeam().opponent());

        // Only move if safe
        if (threats.length == 0 && enemies.length == 0) {
            MapLocation ahead = rc.adjacentLocation(rc.getDirection());

            // Clear obstacles
            if (rc.canRemoveDirt(ahead)) {
                rc.removeDirt(ahead);
            }

            // Move slowly
            if (round % 2 == 0 && rc.canMoveForward()) {
                rc.moveForward();
            } else if (rc.canTurn() && round % 10 == 0) {
                Direction newDir = Direction.allDirections()[(round / 10) % 8];
                rc.turn(newDir);
            }
        }
    }

    private static void spawnRat(RobotController rc) throws GameActionException {
    Direction facing = rc.getDirection();
    
    // Priority order: Front, then the sides
    Direction[] scanOrder = {
        facing, 
        facing.rotateLeft(), 
        facing.rotateRight(),
        facing.rotateLeft().rotateLeft(),
        facing.rotateRight().rotateRight()
    };

    boolean spawned = false;
    for (Direction dir : scanOrder) {
        MapLocation loc = rc.getLocation().add(dir).add(dir);
        if (rc.canBuildRat(loc)) {
            rc.buildRat(loc);
            spawnCount++;
            spawned = true;
            break;
        }
    }

    // IF FRONT IS BLOCKED: Move Backward to create space
    if (!spawned && rc.isMovementReady()) {
        Direction backward = facing.opposite();
        
        // Try to move backward, or 45 degrees off-backward to find a "corner"
        Direction[] retreatDirs = {backward, backward.rotateLeft(), backward.rotateRight()};
        
        for (Direction retreatDir : retreatDirs) {
            if (rc.canMove(retreatDir)) {
                rc.move(retreatDir);
                System.out.println("KING_RETREAT: Front blocked, moving " + retreatDir);
                break;
            }
        }
    }
}

    private static int countCollectors(RobotController rc) throws GameActionException {
        int count = 0;
        RobotInfo[] team = rc.senseNearbyRobots(rc.getType().getVisionRadiusSquared(), rc.getTeam());
        for (RobotInfo r : team) {
            if (r.getType().isBabyRatType() && r.getID() % 2 == 1) {
                count++;
            }
        }
        return count;
    }
    private static void isAttacked(RobotController rc) throws GameActionException {
        int currentHealth = rc.getHealth();
        
        // Inicjalizacja przy pierwszym uruchomieniu
        if (lastHealth == -1) {
            lastHealth = currentHealth;
            return;
        }

        boolean damageTaken = currentHealth < lastHealth;
        lastHealth = currentHealth;

        // Wykrywanie wrogów w zasięgu wzroku
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().getVisionRadiusSquared(), rc.getTeam().opponent());

        // LOGIKA ALARMU:
        // Włączamy alarm (1), jeśli:
        // a) Otrzymaliśmy obrażenia (damageTaken)
        // b) Widzimy wrogów bardzo blisko (dystans <= 16 jednostek kwadratowych)
        boolean underAttack = damageTaken;
        
        if (!underAttack) {
            for (RobotInfo enemy : enemies) {
                // Jeśli wróg jest blisko (np. dystans 4 pola), wezwij pomoc
                if (rc.getLocation().distanceSquaredTo(enemy.getLocation()) <= 16) {
                    underAttack = true;
                    break;
                }
            }
        }

        // Zapis do SharedArray [Indeks 4]
        // 1 = POTRZEBNA POMOC
        // 0 = SYTUACJA STABILNA
        if (underAttack) {
            rc.writeSharedArray(4, 1);
            rc.setIndicatorString("HELP ME! Writing 1 to idx 4"); // Debug wizualny
        } else {
            rc.writeSharedArray(4, 0); // Resetujemy flagę, gdy zagrożenie minie
        }
    }
}