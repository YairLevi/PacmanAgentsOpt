package Agents.MonteCarlo;

import pacman.controllers.Controller;
import pacman.controllers.examples.RandomPacMan;
import pacman.controllers.examples.StarterGhosts;
import pacman.game.Constants;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;

import static Agents.MonteCarlo.MctsConstants.*;


public class MonteCarloAgent extends Controller<MOVE> {

    public static Controller<EnumMap<Constants.GHOST, Constants.MOVE>> ghosts = new StarterGhosts();
    public static int tree_depth = 0;

    @Override
    public MOVE getMove(Game game, long timeDue) {

        //Hunt edible ghosts if not far away
        for (GHOST ghost : GHOST.values()) {
            if (game.getGhostEdibleTime(ghost) > 0) {
                if (game.getShortestPathDistance(game.getPacmanCurrentNodeIndex(), game.getGhostCurrentNodeIndex(ghost)) < hunt_dist) {
                    return game.getNextMoveTowardsTarget(game.getPacmanCurrentNodeIndex(), game.getGhostCurrentNodeIndex(ghost), DM.PATH);
                }
            }
        }

        // run Mcts when in a junction to get next move (next move is based on next junction)
        if (pacmanInJunction(game)) {
            tree_depth = 0;
            return MctsSearch(game);
        }

        // follow path until chosen junction is met.
        return FollowPath(game.getPacmanLastMoveMade(), game);
    }


    public MOVE FollowPath(MOVE dir, Game state) {
        int pacman = state.getPacmanCurrentNodeIndex();
        GHOST[] ghosts = GHOST.values();
        MOVE[] possibleMoves = state.getPossibleMoves(pacman);
        ArrayList<MOVE> moves = new ArrayList<>(Arrays.asList(possibleMoves));

        //EVADE GHOSTS DURING PATH
        ArrayList<MOVE> awayMoves = new ArrayList<>();
        ArrayList<Integer> ghostsNear = new ArrayList<>();

        for (int i = 0; i < ghosts.length; i++) {
            GHOST ghost = ghosts[i];
            int edibleTime = state.getGhostEdibleTime(ghost);
            int lairTime = state.getGhostLairTime(ghost);

            if (edibleTime == 0 && lairTime == 0) {
                int ghostIdx = state.getGhostCurrentNodeIndex(ghost);
                int ghostDist = state.getShortestPathDistance(pacman, ghostIdx);

                if (ghostDist < ghost_dist) {
                    MOVE nextAway = state.getNextMoveAwayFromTarget(pacman, ghostIdx, DM.PATH);

                    awayMoves.add(nextAway);
                    ghostsNear.add(ghostDist);
                }
            }
        }

        int moveIdx = ghostsNear.indexOf(Collections.min(ghostsNear));

        if (awayMoves.size() > 0) return awayMoves.get(moveIdx);
        if (moves.contains(dir)) return dir;

        moves.remove(state.getPacmanLastMoveMade().opposite());
        assert moves.size() == 1; // along a path there is only one possible way remaining
        return moves.get(0);
    }

    private boolean pacmanInJunction(Game game) {
        int pacman = game.getPacmanCurrentNodeIndex();
        return game.isJunction(pacman);
    }

    public MOVE MctsSearch(Game game) {

        //get the current time
        long start = new Date().getTime();

        //create root node with state0
        Node root = new Node(null, game, game.getPacmanCurrentNodeIndex());

        // while we are allowed to keep searching
        while (new Date().getTime() < start + SEARCH_TIME_LIMIT && tree_depth <= TREE_LIMIT) {
            Node selected = SelectionPolicy(root);

            // if not defined, default move
            if (selected == null) return MOVE.DOWN;

            float reward = SimulationPolicy(selected);
            Backpropagation(selected, reward);
        }

        // get the best child
        Node bestChild = BestChild(root, 0);

        // if we have a best child
        if (bestChild != null) return bestChild.actionMove;

        // if we don't
        return new RandomPacMan().getMove(game, -1);
    }


    public Node SelectionPolicy(Node nd) {
        // check in case
        if (nd == null) {
            return null;
        }

        while (!nd.isTerminalGameState()) {
            if (!nd.isFullyExpanded()) return nd.Expand();
            nd = SelectionPolicy(BestChild(nd, C));

            // if null end loop
            if (nd == null) break;
        }

        return nd;
    }


    public float SimulationPolicy(Node nd) {

        // Check null, no reward
        if (nd == null) return 0;

        // If died on the way to the junction
        if (nd.deltaReward == 0.0f) return 0;

        int steps = 0;
        Controller<MOVE> pacManController = new RandomPacMan();
        Controller<EnumMap<GHOST, MOVE>> ghostController = ghosts;

        Game state = nd.game.copy();
        int pillsBefore = state.getNumberOfActivePills();
        int livesBefore = state.getPacmanNumberOfLivesRemaining();

        // simulate
        while (!state.gameOver()) {
            //advance game
            MOVE pacmanMove = pacManController.getMove(state, System.currentTimeMillis());
            EnumMap<GHOST, MOVE> ghostsMoves = ghostController.getMove(state, System.currentTimeMillis());
            state.advanceGame(pacmanMove, ghostsMoves);
            steps++;

            if (steps >= SIMULATION_STEPS) break;
        }

        // DEATH CONDITION
        int livesAfter = state.getPacmanNumberOfLivesRemaining();
        if (livesAfter < livesBefore) {
            return 0.0f;
        }

        // Maze level completed
        if (state.getNumberOfActivePills() == 0) {
            return 1.0f;
        }

        //reward based on pills eaten
        return 1.0f - ((float) state.getNumberOfActivePills() / ((float) pillsBefore));
    }

    public Node BestChild(Node nd, double C) {
        Node bestChild = null;
        double bestValue = -1.0f;

        for (int i = 0; i < nd.children.size(); i++) {

            Node node = nd.children.get(i);
            double uctValue = UCTvalue(node, C);

            if (uctValue >= bestValue) {
                bestValue = uctValue;
                bestChild = node;
            }
        }
        return bestChild;
    }

    private double UCTvalue(Node nd, double C) {
        return (float) ((nd.deltaReward / nd.timesVisited) + C * Math.sqrt(2 * Math.log(nd.parent.timesVisited) / nd.timesVisited));
    }

    private void Backpropagation(Node currentNode, double reward) {
        while (currentNode != null) {
            currentNode.timesVisited++;
            currentNode.deltaReward += reward;
            currentNode = currentNode.parent;
        }
    }
}
