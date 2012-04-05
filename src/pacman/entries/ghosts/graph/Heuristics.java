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
	
	public int getPowerPillScore(int location) {
		return powerPillScore;
	}
	
	public int getNodeScore(int location) {
		return nodeScore[location];
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
		if ((10*b.nrPillsLeft)/b.nrPills > 4) {
			// discourage eating power pills in the beginning
			powerPillScore = -20000; //-10*Constants.GHOST_EAT_SCORE*POINT_FACTOR;
		} else if (existNonKilling && (10*b.nrPillsLeft)/ b.nrPills > 3) {
			// discourage eating power pills if there are still edible ghosts
			powerPillScore = -20000;//-8*Constants.GHOST_EAT_SCORE*POINT_FACTOR;
		} else {
			powerPillScore = 0;
		}
		//System.out.println("power pill score: " + powerPillScore + ", nrPillsLeft = " + b.nrPillsLeft + "/" + b.nrPills + ", exist: " + existNonKilling);
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
