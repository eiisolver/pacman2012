package pacman.entries.ghosts.graph;

import pacman.game.*;

public class Heuristics {
	public static final int POINT_FACTOR = 10;
	private Game game;
	private Board b;
	private JunctionGraph graph;
	/** Power pill score */
	private int powerPillScore;
	private int[] nodeScore;
	
	/**
	 * Update heuristic parameters at beginning of a new move.
	 */
	public void updateForNewMove(Game game, Board board) {
		this.game = game;
		this.b = board;
		graph = b.graph;
		nodeScore = new int[board.graph.nodes.length];
		setPowerPillScore();
		setNodeScore();
	}
	
	public int getPillScore(int location) {
		return Constants.PILL*POINT_FACTOR;
	}
	
	public int getPowerPillScore() {
		return powerPillScore;
	}
	
	public int getNodeScore(int location) {
		return nodeScore[location];
	}
	
	public boolean hasManyLivesLeft() {
		return game.getPacmanNumberOfLivesRemaining() > 2;
	}
	
	public boolean assumeWeakPacman() {
		boolean result = game.getCurrentLevel() == 0 && b.nrPowerPillsOnBoard > 2 
				&& (2000 - 400*game.getPacmanNumberOfLivesRemaining()) > game.getTotalTime();
		return result;
	}
	
	private void setNodeScore() {
		if (powerPillScore < 0) {
			// give negative node scores for nodes on
			// same edge as a power pill
			for (int i = 0; i < b.nrPowerPills; ++i) {
				int p = b.powerPillLocation[i];
				if (b.containsPowerPill[p] && !graph.nodes[p].isJunction()) {
					for (Node n : graph.nodes[p].edge.internalNodes) {
						nodeScore[n.index] = -3*Constants.PILL*POINT_FACTOR/2;
					}
				}
			}
		}
	}

	private void setPowerPillScore() {
		boolean existNonKilling = existNonKillingGhosts();
		if ((100*b.nrPillsOnBoard)/b.nrPills > 30 + 5*b.nrPowerPillsOnBoard && game.getCurrentLevelTime() < 2600-250*b.nrPowerPillsOnBoard) {
			// discourage eating power pills in the beginning
			if (Search.pacmanEvaluation) {
				powerPillScore = -12000; //-10*Constants.GHOST_EAT_SCORE*POINT_FACTOR;
			} else {
				powerPillScore = -2*Constants.GHOST_EAT_SCORE*POINT_FACTOR;
			}
		} else if (existNonKilling && (100*b.nrPillsOnBoard)/ b.nrPills > 10) {
			// discourage eating power pills if there are still edible ghosts
			powerPillScore = -15000;//-8*Constants.GHOST_EAT_SCORE*POINT_FACTOR;
		} else if (game.getCurrentLevelTime() > 2850 - 100*b.nrPowerPillsOnBoard) {
			powerPillScore = Constants.GHOST_EAT_SCORE;
		} else {
			powerPillScore = -500;
		}
		//System.out.println("power pill score: " + powerPillScore + ", nrPillsOnboard = " + b.nrPillsOnBoard + "/" + b.nrPills
		//		+ ", exist: " + existNonKilling + ", time: " + game.getCurrentLevelTime() + ", powerpillsOnBoard: " + b.nrPowerPillsOnBoard);
	}

	private boolean existNonKillingGhosts() {
		for (MyGhost ghost : b.ghosts) {
			if (!ghost.canKill()) {
				return true;
			}
		}
		return false;
	}
	
}
