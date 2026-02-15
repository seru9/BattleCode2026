package TOTAL_AGGRO;

import battlecode.common.*;

public class King {

    private static int spawnCount = 0;
    private static int trapCount = 0;
    private static int lastHealth = 500;

    public static void run(RobotController rc) throws GameActionException {
        int round = rc.getRoundNum();
        int cheese = rc.getGlobalCheese();
        MapLocation me = rc.getLocation();
        int kingHP = rc.getHealth();

        // 1. UPDATE SHARED ARRAY (Global Info)
        rc.writeSharedArray(0, me.x);
        rc.writeSharedArray(1, me.y);

        if (round == 1) {
            rc.writeSharedArray(2, rc.getMapWidth() - 1 - me.x);
            rc.writeSharedArray(3, rc.getMapHeight() - 1 - me.y);
            lastHealth = kingHP;
        }

        // 2. SENSE THREATS & SOS (360 Degree Radar)
        // Sense enemies and cats within vision radius
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(rc.getType().getVisionRadiusSquared(), rc.getTeam().opponent());
        RobotInfo[] nearbyCats = rc.senseNearbyRobots(rc.getType().getVisionRadiusSquared(), Team.NEUTRAL);

        // Check if we are being damaged or see something dangerous
        boolean underAttack = (nearbyEnemies.length > 0 || nearbyCats.length > 0 || kingHP < lastHealth);
        lastHealth = kingHP; // Update for next round

        if (underAttack) {
            // Write "SOS" to Radar indices for long-distance Guardians
            rc.writeSharedArray(4, me.x);
            rc.writeSharedArray(5, me.y);
            rc.writeSharedArray(6, round); 

            // Squeak "911" for immediate nearby Guardians
            
            rc.squeak(911);
            

            // Defensive Action: Attack back if possible
            if (nearbyEnemies.length > 0 && rc.canAttack(nearbyEnemies[0].getLocation())) {
                rc.attack(nearbyEnemies[0].getLocation());
            }
        }

        // 3. POPULATION MANAGEMENT
        int cost = rc.getCurrentRatCost();
        if (spawnCount < Config.MAX_SPAWN_COUNT && cheese > cost + Config.KING_CHEESE_RESERVE) {
            // Always try to keep at least 4 collectors alive
            int collectors = countCollectors(rc);
            if (spawnCount < Config.INITIAL_SPAWN_COUNT || collectors < Config.COLLECTOR_MINIMUM) {
                if (rc.isActionReady()) {
                    spawnRat(rc);
                }
            }
        }

        // 4. DEFENSIVE PERIMETER (Traps)
        // Place traps in a 3x3-ish ring around the 3x3 king
        if (cheese > 400 && rc.isActionReady()) {
            for (Direction dir : Direction.allDirections()) {
                MapLocation loc = me.add(dir).add(dir); // Offset to be outside the king's 3x3 body
                
                // Prioritize Rat Traps for enemy backstabbers
                if (trapCount < 10 && rc.canPlaceRatTrap(loc)) {
                    rc.placeRatTrap(loc);
                    trapCount++;
                    break;
                }
                // Cat traps for the pouncing cat
                if (trapCount < 20 && rc.canPlaceCatTrap(loc)) {
                    rc.placeCatTrap(loc);
                    trapCount++;
                    break;
                }
            }
        }

        // 5. MOVEMENT (Harder to hit, but stays near home)
        // Only move if there are no immediate threats to allow guardians to gather
        if (!underAttack && round % 20 == 0) {
            Direction moveDir = Direction.allDirections()[round % 8];
            if (rc.canMove(moveDir)) {
                rc.move(moveDir);
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
            // Logic: ID % 2 == 1 identifies collectors in our setup
            if (r.getType() == UnitType.BABY_RAT && r.getID() % 2 == 1) {
                count++;
            }
        }
        return count;
    }
}