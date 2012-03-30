package pacman.entries.ghosts.graph;

import pacman.game.Game;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

public class Board {
	public JunctionGraph graph;
	public MyGhost[] ghosts;
	public int pacmanLocation;
	public MOVE pacmanLastMove;
	public boolean[] containsPill;
	public boolean[] containsPowerPill;
	/** Locations of the power pills */
	public int[] powerPillLocation = new int[20];
	public int nrPowerPills;

	public Board() {
		ghosts = new MyGhost[GHOST.values().length];
		for (int i = 0; i < ghosts.length; ++i) {
			ghosts[i] = new MyGhost();
			ghosts[i].ghost = GHOST.values()[i];
		}
	}
	
	public void update(Game game) {
		containsPill = new boolean[graph.nodes.length];
		for (int index = 0; index < containsPill.length; ++index) {
			int pillIndex = game.getPillIndex(index);
			containsPill[index] = pillIndex >= 0 && game.isPillStillAvailable(pillIndex);
		}
		for (BigEdge edge : graph.edges) {
			edge.containsPowerPill = false;
		}
		containsPowerPill = new boolean[graph.nodes.length];
		nrPowerPills = 0;
		for (int index = 0; index < containsPowerPill.length; ++index) {
			int powerPillIndex = game.getPowerPillIndex(index);
			containsPowerPill[index] = powerPillIndex >= 0 && game.isPowerPillStillAvailable(powerPillIndex);
			if (containsPowerPill[index] && !graph.nodes[index].isJunction()) {
				graph.nodes[index].edge.containsPowerPill = true;
				powerPillLocation[nrPowerPills] = index;
				++nrPowerPills;
			}
		}
		pacmanLocation = game.getPacmanCurrentNodeIndex();
		pacmanLastMove = game.getPacmanLastMoveMade();
		for (int i = 0; i < ghosts.length; ++i) {
			ghosts[i].currentNodeIndex = game.getGhostCurrentNodeIndex(ghosts[i].ghost);
			ghosts[i].lastMoveMade = game.getGhostLastMoveMade(ghosts[i].ghost);
			ghosts[i].edibleTime = game.getGhostEdibleTime(ghosts[i].ghost);
			ghosts[i].lairTime = game.getGhostLairTime(ghosts[i].ghost);
		}
		for (Node n : graph.nodes) {
			n.nrGhosts = 0;
		}
		for (MyGhost ghost : ghosts) {
			++graph.nodes[ghost.currentNodeIndex].nrGhosts;
		}
	}
	
	public void copyFrom(Board src) {
		pacmanLocation = src.pacmanLocation;
		pacmanLastMove = src.pacmanLastMove;
		for (int i = 0; i < ghosts.length; ++i) {
			ghosts[i].copyFrom(src.ghosts[i]);
		}
	}
}
