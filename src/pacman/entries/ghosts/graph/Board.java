package pacman.entries.ghosts.graph;

import static pacman.game.Constants.COMMON_LAIR_TIME;
import static pacman.game.Constants.EDIBLE_TIME;
import static pacman.game.Constants.EDIBLE_TIME_REDUCTION;
import static pacman.game.Constants.LAIR_REDUCTION;
import pacman.game.Constants;
import pacman.game.Game;
import pacman.game.Constants.*;

public class Board {
	public JunctionGraph graph;
	public MyGhost[] ghosts;
	public int pacmanLocation;
	public MOVE pacmanLastMove;
	public boolean[] containsPill;
	public boolean[] containsPowerPill;
	/** Locations of the power pills */
	public int[] powerPillLocation = new int[20];
	/** total nr of power pills at start of this level */
	public int nrPowerPills;
	/** total nr of pills at start of this level */
	public int nrPills;
	/** number of pills left */
	public int nrPowerPillsLeft;
	public int nrPillsLeft;
	public int currentEdibleTime = 200;
	public int currentLairTime = 200;

	public Board() {
		ghosts = new MyGhost[GHOST.values().length];
		for (int i = 0; i < ghosts.length; ++i) {
			ghosts[i] = new MyGhost();
			ghosts[i].ghost = GHOST.values()[i];
		}
	}
	
	public void update(Game game) {
		nrPills = game.getPillIndices().length;
		nrPillsLeft = 0;
		containsPill = new boolean[graph.nodes.length];
		for (int index = 0; index < containsPill.length; ++index) {
			int pillIndex = game.getPillIndex(index);
			containsPill[index] = pillIndex >= 0 && game.isPillStillAvailable(pillIndex);
			if (containsPill[index]) {
				++nrPillsLeft;
			}
		}
		for (BigEdge edge : graph.edges) {
			edge.containsPowerPill = false;
		}
		containsPowerPill = new boolean[graph.nodes.length];
		nrPowerPills = 0;
		nrPowerPillsLeft = 0;
		for (int index = 0; index < containsPowerPill.length; ++index) {
			int powerPillIndex = game.getPowerPillIndex(index);
			containsPowerPill[index] = powerPillIndex >= 0 && game.isPowerPillStillAvailable(powerPillIndex);
			if (containsPowerPill[index] && !graph.nodes[index].isJunction()) {
				graph.nodes[index].edge.containsPowerPill = true;
				++nrPowerPillsLeft;
			}
			if (powerPillIndex >= 0) {
				powerPillLocation[nrPowerPills] = index;
				++nrPowerPills;
			}
		}
		//game.getPillIndices().length;
		pacmanLocation = game.getPacmanCurrentNodeIndex();
		pacmanLastMove = game.getPacmanLastMoveMade();
		for (int i = 0; i < ghosts.length; ++i) {
			ghosts[i].currentNodeIndex = game.getGhostCurrentNodeIndex(ghosts[i].ghost);
			ghosts[i].lastMoveMade = game.getGhostLastMoveMade(ghosts[i].ghost);
			ghosts[i].edibleTime = game.getGhostEdibleTime(ghosts[i].ghost);
			ghosts[i].lairTime = game.getGhostLairTime(ghosts[i].ghost);
		}
		currentEdibleTime = (int)(EDIBLE_TIME*(Math.pow(EDIBLE_TIME_REDUCTION,game.getCurrentLevel())));
		currentLairTime=(int)(COMMON_LAIR_TIME*(Math.pow(LAIR_REDUCTION,game.getCurrentLevel())));
	}
	
	public void copyFrom(Board src) {
		pacmanLocation = src.pacmanLocation;
		pacmanLastMove = src.pacmanLastMove;
		for (int i = 0; i < ghosts.length; ++i) {
			ghosts[i].copyFrom(src.ghosts[i]);
		}
	}
	
	public void logBoard(Game game) {
		graph.print(game, this);
	}
}
