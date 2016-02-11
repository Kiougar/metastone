package net.demilich.metastone.game.behaviour.mcts;

interface ITreePolicy {

	INode select(INode parent);
}
