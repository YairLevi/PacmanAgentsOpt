package Agents;

import java.util.*;

import pacman.controllers.Controller;
import pacman.controllers.examples.StarterGhosts;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;


public class HeuristicAgent extends Controller<MOVE> {

    public static Controller<EnumMap<GHOST, MOVE>> ghosts = new StarterGhosts();

    public static int hasFood(Game game) {
        return game.wasPillEaten() ? 1 : 0;
    }

    public static int hasScaredGhost(Game game) {
        return game.getNumGhostsEaten() > 0 ? 1 : 0;
    }

    public static int hasCapsule(Game game) {
        return game.wasPowerPillEaten() ? 1 : 0;
    }

    public static double getGameScore(Game state) {
        if (state.wasPacManEaten()) return Double.NEGATIVE_INFINITY;

        double foodScore = 100 * hasFood(state);
        double activeGhostScore = 0;
        double scaredGhostScore = 50 * hasScaredGhost(state);
        double capsuleScore = 100 * hasCapsule(state);

        int pacmanIndex = state.getPacmanCurrentNodeIndex();

        ArrayList<Integer> distanceToFood = new ArrayList<>();
        for (int i : state.getActivePillsIndices()) {
            distanceToFood.add(state.getShortestPathDistance(pacmanIndex, i));
        }

        ArrayList<Integer> distanceToCapsule = new ArrayList<>();
        for (int i : state.getActivePowerPillsIndices()) {
            distanceToCapsule.add(state.getShortestPathDistance(pacmanIndex, i));
        }

        ArrayList<Integer> distancesToScaredGhosts = new ArrayList<>();
        ArrayList<Integer> distancesToActiveGhosts = new ArrayList<>();
        for (GHOST g : state.getGhosts()) {
            int ghostIndex = state.getGhostCurrentNodeIndex(g);
            int d = state.getShortestPathDistance(pacmanIndex, ghostIndex);
            if (state.getGhostEdibleTime(g) > 0) {
                distancesToScaredGhosts.add(d);
            } else {
                distancesToActiveGhosts.add(d);
            }
        }

        if (distanceToFood.size() > 0) {
            int closestFood = Collections.min(distanceToFood);
            foodScore -= 0.1 * closestFood;
        }

        if (distancesToActiveGhosts.size() > 0) {
            int closestActiveGhost = Collections.min(distancesToActiveGhosts);
            if (closestActiveGhost < 5) {
                activeGhostScore += -2000 * (double) (1 / closestActiveGhost);
            } else if (closestActiveGhost < 10) {
                activeGhostScore += -0 * (double) (1 / closestActiveGhost);
            } else {
                activeGhostScore += -0 * (double) (5 - closestActiveGhost);
            }
        }

        if (distancesToScaredGhosts.size() > 0) {
            int closestScaredGhost = Collections.min(distancesToScaredGhosts);
            scaredGhostScore += 0 * (double) (1 / closestScaredGhost);
        }

        if (distanceToCapsule.size() > 0 && distancesToScaredGhosts.size() == 0) {
            int closestCapsule = Collections.min(distanceToCapsule);
            capsuleScore += 10 * (double) (1 / closestCapsule);
        }


        return foodScore + activeGhostScore + scaredGhostScore + capsuleScore;
    }

    public static double getScoreOfMove(Game game, MOVE m) {
        Game state = game.copy();
        state.advanceGame(m, ghosts.getMove(state, System.currentTimeMillis()));
        double neutralPenalty = m == MOVE.NEUTRAL ? 10 : 0;
        return getGameScore(state) - neutralPenalty;
    }

    @Override
    public MOVE getMove(Game game, long timeDue) {

        MOVE[] moves = game.getPossibleMoves(game.getPacmanCurrentNodeIndex());
        Map<MOVE, Double> scores = new HashMap<>();
        for (MOVE m : moves) {
            scores.put(m, getScoreOfMove(game, m));
        }

        Map.Entry<MOVE, Double> maxEntry = null;

        for (Map.Entry<MOVE, Double> entry : scores.entrySet()) {
            if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
                maxEntry = entry;
        }

        if (maxEntry == null) return MOVE.DOWN;
        return maxEntry.getKey();
    }
}