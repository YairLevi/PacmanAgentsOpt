package Agents;

import pacman.controllers.Controller;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import static Agents.MinimaxAgent.evaluationFunction;

import java.util.ArrayList;
import java.util.EnumMap;

public class AlphaBetaAgent extends Controller<MOVE> {

    public int treeDepth;

    public AlphaBetaAgent(int d) {
        this.treeDepth = d;
    }

    public boolean compare(int a, int b, boolean isGreater) {
        return isGreater == (a > b);
    }

    public MoveScorePair<MOVE, Integer> alphaBeta(Game game, int agentIndex, int depth, double alpha, double beta) {
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
        double v;

        if (agentIndex == 0) {
            moves = game.getPossibleMoves(game.getPacmanCurrentNodeIndex());
            v = Double.NEGATIVE_INFINITY;

        } else {
            currentGhost = game.getGhosts().get(agentIndex - 1);
            moves = game.getPossibleMoves(game.getGhostCurrentNodeIndex(currentGhost));
            v = Double.POSITIVE_INFINITY;
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
            MoveScorePair<MOVE, Integer> pair = alphaBeta(state, agentIndex + 1, depth, alpha, beta);
            int value = pair.score;
            actionsValues.add(new MoveScorePair<>(m, value));

            if (agentIndex == 0) {
                if (v > beta) {
                    break;
                }
                alpha = Math.max(alpha, v);

            } else {
                if (v < alpha) {
                    break;
                }
                beta = Math.min(beta, v);

            }
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
        double alpha = Double.NEGATIVE_INFINITY;
        double beta = Double.POSITIVE_INFINITY;
        return alphaBeta(game, 0, this.treeDepth, alpha, beta).move;
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
