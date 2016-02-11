package net.demilich.metastone.game.behaviour.mcts;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.PlayRandomBehaviour;

class INode {

	private GameContext state;
	private List<GameAction> validTransitions;
	private final List<INode> children = new LinkedList<>();
	private final GameAction incomingAction;
	private int visits;
	private int score;
	private final int player;

	public INode(GameAction incomingAction, int player) {
		this.incomingAction = incomingAction;
		this.player = player;
	}

	private boolean canFurtherExpanded() {
		return !validTransitions.isEmpty();
	}

	private INode expand() {
		GameAction action = validTransitions.remove(0);
		GameContext newState = state.clone();

		try {
			newState.getLogic().performGameAction(newState.getActivePlayer().getId(), action);
		} catch (Exception e) {
			System.err.println("Exception on action: " + action + " state decided: " + state.gameDecided());
			e.printStackTrace();
			throw e;
		}

		INode child = new INode(action, getPlayer());
		child.initState(newState, newState.getValidActions());
		children.add(child);
		return child;
	}

	public GameAction getBestAction() {
		GameAction best = null;
		int bestScore = Integer.MIN_VALUE;
		for (INode node : children) {
			if (node.getScore() > bestScore) {
				best = node.incomingAction;
				bestScore = node.getScore();
			}
		}
		return best;
	}

	public List<INode> getChildren() {
		return children;
	}

	public int getPlayer() {
		return player;
	}

	public int getScore() {
		return score;
	}

	public GameContext getState() {
		return state;
	}

	public int getVisits() {
		return visits;
	}

	public void initState(GameContext state, List<GameAction> validActions) {
		this.state = state.clone();
		this.validTransitions = new ArrayList<GameAction>(validActions);
	}

	public boolean isExpandable() {
		if (validTransitions.isEmpty()) {
			return false;
		}
		if (state.gameDecided()) {
			return false;
		}
		return getChildren().size() < validTransitions.size();
	}

	public boolean isLeaf() {
		return children == null || children.isEmpty();
	}

	private boolean isTerminal() {
		return state.gameDecided();
	}

	public void process(ITreePolicy treePolicy) {
		List<INode> visited = new LinkedList<INode>();
		INode current = this;
		INode tmp;
		visited.add(this);
		while (!current.isTerminal()) {
			if (current.canFurtherExpanded()) {
				current = current.expand();
				visited.add(current);
				break;
			} else {
				tmp = treePolicy.select(current);
				if (tmp == null) break;
				current = tmp;
				visited.add(current);
			}
		}

		int value = rollOut(current);
		for (INode node : visited) {
			node.updateStats(value);
		}
	}

	public int rollOut(INode node) {
		if (node.getState().gameDecided()) {
			GameContext state = node.getState();
			return state.getWinningPlayerId() == getPlayer() ? 1 : 0;
		}

		GameContext simulation = node.getState().clone();
		for (Player player : simulation.getPlayers()) {
			player.setBehaviour(new PlayRandomBehaviour());
		}

		simulation.playTurn();

		return simulation.getWinningPlayerId() == getPlayer() ? 1 : 0;
	}

	private void updateStats(int value) {
		visits++;
		score += value;
	}

}
