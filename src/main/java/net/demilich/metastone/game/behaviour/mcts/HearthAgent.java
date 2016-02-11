package net.demilich.metastone.game.behaviour.mcts;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.TurnState;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.Behaviour;
import net.demilich.metastone.game.behaviour.PlayRandomBehaviour;
import net.demilich.metastone.game.cards.Card;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Georgios on 09/02/2016.
 */
public class HearthAgent extends Behaviour {

    private final static Logger logger = LoggerFactory.getLogger(HearthAgent.class);

    private static final int ITERATIONS = 500;
    private final int iterations;

    public HearthAgent() {
        this.iterations = ITERATIONS;
    }

    public HearthAgent(int iterations) {
        this.iterations = iterations;
    }

    @Override
    public String getName() {
        return "HearthAgent";
    }

    @Override
    public List<Card> mulligan(GameContext context, Player player, List<Card> cards) {
        // discard over 4 manacost
        return cards.stream().filter(card -> card.getBaseManaCost() >= 4).collect(Collectors.toList());
    }

    @Override
    public GameAction requestAction(GameContext context, Player player, List<GameAction> validActions) {
        logger.debug("Requesting action for player {} and turn {}", player, context.getTurn());
        if (validActions.size() == 1) {
            logger.debug("valid actions size is 1, returning action {}", validActions.get(0));
            return validActions.get(0);
        } else {
            logger.debug("Entering UCT...");
            return this.UCT(context, iterations, validActions);
        }
    }

    private GameAction UCT(GameContext rootstate, int itermax, List<GameAction> validActions) {
        Node rootNode = new Node(rootstate, validActions);
        Node node;
        GameContext state;
        GameAction action;
        //GameAction random_action;
        int winning, result;


        logger.debug("Starting iterations... for rootstate {} and turn {} and mana {}",
                rootstate.hashCode(), rootstate.getTurn(), rootstate.getActivePlayer().getMana(), rootstate);
        for (int i=0; i < itermax; i++) {
            logger.debug("Iteration {}", i);
            node = rootNode;
            state = rootstate.clone();

            // Select
            while (node.isFullyExpanded() && !node.isTerminal()) {
                node = node.UTCSelectChild();
                logger.debug("Selected node with action {} and parent action {} and parent == rootNode {}",
                        node.getAction(), node.getParent().getAction(), node.getParent() == rootNode);
                // get state from node, else we might end up with different cards in hand due to random draw after end turn
                state = node.getState();
                logger.debug("Selected state is now \n {}", state);
                //playAction(state, node.getAction());
            }

            // Expand
            if (!node.isFullyExpanded()) { // if we can expand (i.e. state/node is non-terminal
                action = node.getRandomUntriedAction();
                logger.debug("Expanded random action {}", action);
                playAction(state, action);
                node = node.AddChild(action, state); // add child and descent tree
                logger.debug("Added child with action: {} and state: {}", action, state.hashCode());
            }


            logger.debug("Staring roll out on {} with cloned state", state.hashCode());
            state = state.clone();
            // Roll out - play the game randomly till the game ends
            for (Player p : state.getPlayers()) {
                p.setBehaviour(new PlayRandomBehaviour());
            }
            while (!state.gameDecided()) {
                if (!state.gameDecided() && (state.getTurnState() == TurnState.TURN_ENDED)) {
                    state.startTurn();
                }
                while (state.playTurn());
            }

            // Back propagate
            winning = state.getWinningPlayerId();
            state.dispose();
            logger.debug("Roll out of {} has ended with result {} (winning player: {})", state.hashCode(), winning, state.getWinningPlayerId());
            while(node != null) {
                result = winning == node.getPlayerId() ? 1 : 0;
                node.Update(result);
                node = node.getParent();
            }
        }
        logger.debug("Ended iterations!");

        return rootNode.getBestChildAction();
    }

    private void playAction(GameContext state, GameAction action) {
        //if (!state.gameDecided() && (state.getTurnState() == TurnState.TURN_ENDED)) state.startTurn();
        boolean ret = state.playAction(action);

        if (!state.gameDecided() && !ret) state.startTurn();
        logger.debug("Played action {} to state \n {}", action, state);
    }
}
