package examplefuncsplayer;

import battlecode.common.*;

public class Movement {

    // Priority order for checking directions relative to the target:
    // 0: Target, 1: 45° Right, 2: 45° Left, 3: 90° Right, 4: 90° Left...
    private static final int[] ROTATION_OFFSETS = {0, 1, 7, 2, 6, 3, 5, 4};

    public static void moveTo(RobotController rc, MapLocation target) throws GameActionException {
        if (!rc.isMovementReady()) return;

        MapLocation me = rc.getLocation();
        Direction targetDir = me.directionTo(target);

        if (targetDir == Direction.CENTER) return;

        // 1. OBSTACLE REMOVAL (Dirt & Traps)
        // If the very first tile we want to enter is blocked by dirt, dig it.
        MapLocation nextTile = me.add(targetDir);
        if (rc.canSenseLocation(nextTile)) {
            if (rc.senseMapInfo(nextTile).isDirt() && rc.canRemoveDirt(nextTile)) {
                rc.removeDirt(nextTile);
                return;
            }
        }

        // 2. PRIORITY SCAN (The "Clockwise/Counter-Clockwise" Fix)
        Direction bestDir = null;
        for (int offset : ROTATION_OFFSETS) {
            Direction candidate = rotate(targetDir, offset);
            
            // canMove(Direction) checks if the tile is on map, passable, and unoccupied
            if (rc.canMove(candidate)) {
                // Safety check: Don't walk into enemy/allied RAT traps if sensed
                if (rc.canSenseLocation(me.add(candidate))) {
                    if (rc.senseMapInfo(me.add(candidate)).getTrap() != TrapType.RAT_TRAP) {
                        bestDir = candidate;
                        break;
                    }
                } else {
                    bestDir = candidate;
                    break;
                }
            }
        }

        // 3. EXECUTE MOVE
        if (bestDir != null) {
            // Battlecode Tip: Moving forward (CD 10) is cheaper than strafing (CD 18)
            // We turn to face the best direction first, then move.
            if (rc.getDirection() != bestDir) {
                if (rc.canTurn()) {
                    rc.turn(bestDir);
                }
            } else {
                if (rc.canMoveForward()) {
                    rc.moveForward();
                }
            }
        } else {
            // TOTAL BLOCKAGE: If we can't move anywhere, spin 90 degrees to refresh vision
            if (rc.canTurn()) {
                rc.turn(rc.getDirection().rotateRight().rotateRight());
            }
        }
    }

    /**
     * Helper to rotate a direction by a specific number of 45-degree steps.
     * 1 = rotateRight, 7 = rotateLeft (which is 7 rights)
     */
    private static Direction rotate(Direction start, int steps) {
        Direction res = start;
        for (int i = 0; i < steps; i++) {
            res = res.rotateRight();
        }
        return res;
    }
}