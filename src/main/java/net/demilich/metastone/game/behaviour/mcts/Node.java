package net.demilich.metastone.game.behaviour.mcts;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.mcts.utils.ListUtils;

import java.util.LinkedList;
import java.util.List;

class Node {

    //private final static Logger logger = LoggerFactory.getLogger(Node.class);

    private final GameAction action;
    private final Node parent;
    private final int playerId;
    private List<Node> childNodes = new LinkedList<>();
    private int wins = 0;
    private int visits = 0;
    private List<GameAction> untriedMoves;
    private GameContext state;
    //private ITreePolicy policy;

    private Node(GameAction action, Node parent, GameContext state) {
        this.action = action;
        this.parent = parent;
        this.state = state;
        this.playerId = state.getActivePlayerId();
        this.untriedMoves = state.getValidActions();
    }

    public Node(GameContext state, List<GameAction> validActions) {
        this.action = null;
        this.parent = null;
        this.state = state;
        this.playerId = state.getActivePlayerId();
        this.untriedMoves = validActions;
    }

    public Node UTCSelectChild() {
        Node bestChild = null;
        double ucb = Double.MIN_VALUE;
        for (Node node : this.childNodes) {
            double tmp = this.UCB1(node);
            if (tmp > ucb || bestChild == null) {
                bestChild = node;
                ucb = tmp;
            }
        }
        return bestChild;
    }

    public Node AddChild(GameAction a, GameContext s) {
        Node node = new Node(a, this, s);
        this.untriedMoves.remove(a);
        this.childNodes.add(node);
        return node;
    }

    public void Update(int result) {
        this.visits++;
        this.wins += result;
    }

    private double UCB1(Node c) {
        return (double) c.wins/c.visits + Math.sqrt(2*Math.log(this.visits))/c.visits;
    }

    public boolean isFullyExpanded() {
        return this.untriedMoves.size() == 0;
    }

    public boolean isTerminal() {
        return this.childNodes.size() == 0;
    }

    public GameAction getRandomUntriedAction() {
        return ListUtils.getRandomElement(this.untriedMoves);
    }

    public GameAction getAction() {
        return action;
    }

    public Node getParent() {
        return parent;
    }

    public int getPlayerId() {
        return playerId;
    }

    /**
     * Select the action that was most visited
     * @return The most visited action
     */
    public GameAction getBestChildAction() {
        GameAction best = null;
        int visits = -1;
        for (Node c: this.childNodes) {
            if (c.visits > visits) {
                visits = c.visits;
                best = c.action;
            }
        }
        return best;
    }

    public GameContext getState() {
        return state.clone();
    }
}
