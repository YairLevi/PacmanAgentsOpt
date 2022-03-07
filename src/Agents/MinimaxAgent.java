package Agents;

import pacman.controllers.Controller;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;


public class MinimaxAgent extends Controller<MOVE> {

    public int treeDepth;

    public MinimaxAgent(int d) {
        this.treeDepth = d;
    }

    public boolean compare(int a, int b, boolean isGreater) {
        return isGreater == (a > b);
    }

    public static Integer evaluationFunction(Game state) {
        if (state.gameOver()) {
            if (state.wasPacManEaten())
                return Integer.MIN_VALUE;
            else
                return Integer.MAX_VALUE;
        }

        int currentScore = state.getScore();
        int powerPillsLeft = state.getNumberOfActivePowerPills();
        int pillsLeft = state.getNumberOfActivePills();

        int pacmanIndex = state.getPacmanCurrentNodeIndex();


        ArrayList<Integer> distanceToFood = new ArrayList<>();
        for (int i : state.getActivePillsIndices()) {
            distanceToFood.add(state.getShortestPathDistance(pacmanIndex, i));
        }
        int closestFood = Collections.min(distanceToFood);


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

        int closestActiveGhost = Integer.MAX_VALUE, closestScaredGhost = Integer.MAX_VALUE;
        if (distancesToActiveGhosts.size() > 0) {
            closestActiveGhost = Collections.min(distancesToActiveGhosts);
        }

        if (distancesToScaredGhosts.size() > 0) {
            closestScaredGhost = Collections.min(distancesToScaredGhosts);
        }

        return (int) (currentScore +
                -1.5 * closestFood +
                -2 * (1/closestActiveGhost) +
                -2 * closestScaredGhost +
                -20 * powerPillsLeft +
                -4 * pillsLeft);
    }

    public MoveScorePair<MOVE, Integer> minimax(Game game, int agentIndex, int depth) {
        int numOfAgents = game.getGhosts().size() + 1;

        if (agentIndex == numOfAgents) {
            agentIndex = 0;
            depth--;
        }

        if (game.gameOver() || depth == 0) {
            return new MoveScorePair<>(null, evaluationFunction(game));
        }

        ArrayList<MoveScorePair<MOVE, Integer>> actionsValues = new ArrayList<>();
        MOVE[] moves;
        GHOST currentGhost = null;

        if (agentIndex == 0) {
            moves = game.getPossibleMoves(game.getPacmanCurrentNodeIndex());
        } else {
            currentGhost = game.getGhosts().get(agentIndex - 1);
            moves = game.getPossibleMoves(game.getGhostCurrentNodeIndex(currentGhost));
        }

        for (MOVE m : moves) {
            Game state = game.copy();
            MOVE pacmanMove = MOVE.NEUTRAL;
            EnumMap<GHOST, MOVE> ghostMoves = new EnumMap<>(GHOST.class);
            for (GHOST g : game.getGhosts()) {
                ghostMoves.put(g, MOVE.NEUTRAL);
            }
            if (agentIndex == 0) {
                pacmanMove = m;
            } else {
                ghostMoves.put(currentGhost, m);
            }
            state.advanceGame(pacmanMove, ghostMoves);
            MoveScorePair<MOVE, Integer> pair = minimax(state, agentIndex + 1, depth);
            int value = pair.score;
            actionsValues.add(new MoveScorePair<>(m, value));
        }

        boolean isGreater = agentIndex == 0;

        if (actionsValues.size() == 0) {
            return new MoveScorePair<>(MOVE.LEFT, 0);
        }

        MoveScorePair<MOVE, Integer> best = actionsValues.get(0);
        for (MoveScorePair<MOVE, Integer> pair : actionsValues) {
            if (compare(pair.score, best.score, isGreater)) {
                best = pair;
            }
        }

        return best;
    }

    @Override
    public MOVE getMove(Game game, long timeDue) {
        return minimax(game, 0, this.treeDepth).move;
    }

    public static class MoveScorePair<M, S> {
        public M move;
        public S score;

        public MoveScorePair(M m, S s) {
            this.move = m;
            this.score = s;
        }
    }
}
