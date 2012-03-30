package pacman.entries.ghosts.graph;

import pacman.game.Constants;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

/**
 * Contains information specific at a certain ply during
 * searching.
 */
public class PlyInfo {
	// ############################################################################
	// IN-PARAMETERS TO THE SEARCH
	// ############################################################################

	public int alpha;
	public int beta;
	/** value when this ply was entered */
	public int currValue;
	/** "budget"; 1 move costs normally 10 */
	public int budget;
	/** score (the points of the pacman) */
	public int score;

	// ############################################################################
	// SAVED PARAMETERS
	// ############################################################################

	/** Board as it was at the beginning of the ply */
	public Board savedBoard;
	/** Number of possible moves */
	int nrPossibleMoves;
	/** Current move nr; ranges from 0 (first move searched) to nrPossibleMoves */
	int currMoveNr;
	int pacmanMoveIndex;
	int[] ghostMoveIndex;
	/** Points scored at this move */
	int moveScore;
	/** if a pill was eaten at this move */
	boolean pillValue;
	/** if a power pill was eaten at this move */
	boolean powerPillValue;
	
	// ############################################################################
	// OUTPUT PARAMETERS
	// ############################################################################

	/** The value of the best move */
	public int bestValue;
	public int[] bestGhostMove;
	public int bestPacmanMove;

	public PlyInfo() {
		savedBoard = new Board();
		ghostMoveIndex = new int[GHOST.values().length];
		bestGhostMove = new int[GHOST.values().length];
	}
	
	/**
	 * Initializes move generation. Updates nrPossibleMoves
	 * @param movePacman
	 */
	public void initMove(boolean movePacman) {
		currMoveNr = 0;
		if (movePacman) {
			pacmanMoveIndex = -1;
			nrPossibleMoves = Search.nodes[Search.b.pacmanLocation].nrNeighbours;
		} else {
			nrPossibleMoves = 1;
			for (int i = 0; i < ghostMoveIndex.length; ++i) {
				MyGhost ghost = Search.b.ghosts[i];
				if (ghost.lairTime > 0) {
					ghostMoveIndex[i] = -2;
				} else {
					Node n = Search.nodes[ghost.currentNodeIndex];
					nrPossibleMoves *= n.nrNeighbours-1;
					for (int e = 0; e < n.nrNeighbours; ++e) {
						if (n.neighbourMoves[e] != ghost.lastMoveMade.opposite()) {
							ghostMoveIndex[i] = e;
						}
					}
				}
			}
		}
	}
	
	/**
	 * Sets next move
	 * @param movePacman
	 * @return false if there were no more moves
	 */
	public boolean nextMove(boolean movePacman) {
		if (movePacman) {
			++pacmanMoveIndex;
		} else {
			if (currMoveNr > 0) {
				nextGhostMove(0);
			}
		}
		++currMoveNr;
		return currMoveNr <= nrPossibleMoves;
	}
	
	public void saveBestMove(boolean movePacman) {
		if (movePacman) {
			bestPacmanMove = pacmanMoveIndex;
		} else {
			for (int i = 0; i < bestGhostMove.length; ++i) {
				bestGhostMove[i] = ghostMoveIndex[i];
			}
		}
	}
	
	private void nextGhostMove(int index) {
		if (index >= ghostMoveIndex.length) {
			return;
		}
		if (ghostMoveIndex[index] == -2) {
			nextGhostMove(index+1);
			return;
		}
		MyGhost ghost = Search.b.ghosts[index];
		Node n = Search.nodes[ghost.currentNodeIndex];
		if (!n.isJunction()) {
			nextGhostMove(index+1);
			return;
		}
		boolean wrapped = false;
		while (true) {
			++ghostMoveIndex[index];
			if (ghostMoveIndex[index] >= n.nrNeighbours) {
				ghostMoveIndex[index] = 0;
				wrapped = true;
			}
			if (n.neighbourMoves[ghostMoveIndex[index]] != ghost.lastMoveMade.opposite()) {
				if (wrapped) {
					nextGhostMove(index+1);
				}
				return;
			}
		}
	}
	
	public void move(boolean movePacman) {
		if (movePacman) {
			movePacman(Search.b);
		} else {
			moveGhosts(Search.b);
		}
	}
	
	public void unmove(boolean movePacman) {
		if (movePacman) {
			unmovePacman(Search.b);
		} else {
			unmoveGhosts(Search.b);
		}
	}
	
	public String moveToString(boolean movePacman) {
		if (movePacman) {
			if (pacmanMoveIndex >= 0) {
				return Search.b.graph.nodes[Search.b.pacmanLocation].neighbourMoves[pacmanMoveIndex].toString();
			} else {
				return "NEUTRAL";
			}
		} else {
			StringBuilder buf = new StringBuilder();
			for (int i = 0; i < ghostMoveIndex.length; ++i) {
				Node n = Search.b.graph.nodes[Search.b.ghosts[i].currentNodeIndex];
				buf.append("(" + n.y + "," + n.x + ",");
				int moveIndex = ghostMoveIndex[i];
				if (moveIndex >= 0) {
					buf.append(n.neighbourMoves[moveIndex].toString());
				} else {
					buf.append("NEUTRAL");
				}
				buf.append(") ");
			}
			return buf.toString();
		}
	}

	public void movePacman(Board b) {
		savedBoard.copyFrom(b);
		if (pacmanMoveIndex >= 0) {
			Node n = b.graph.nodes[b.pacmanLocation];
			b.pacmanLocation = n.neighbours[pacmanMoveIndex];
			b.pacmanLastMove = n.neighbourMoves[pacmanMoveIndex];
			pillValue = b.containsPill[b.pacmanLocation];
			if (pillValue) {
				moveScore = Constants.PILL;
			}
			b.containsPill[b.pacmanLocation] = false;
			powerPillValue = b.containsPowerPill[b.pacmanLocation];
			if (powerPillValue) {
				moveScore = Constants.POWER_PILL;
			}
			b.containsPowerPill[b.pacmanLocation] = false;
		}
	}

	public void unmovePacman(Board b) {
		if (pacmanMoveIndex >= 0) {
			b.copyFrom(savedBoard);
			b.containsPill[b.pacmanLocation] = pillValue;
			b.containsPowerPill[b.pacmanLocation] = powerPillValue;
		}
	}

	public void moveGhosts(Board b) {
		savedBoard.copyFrom(b);
		for (int i = 0; i < ghostMoveIndex.length; ++i) {
			int moveIndex = ghostMoveIndex[i];
			if (moveIndex >= 0) {
				Node n = b.graph.nodes[b.ghosts[i].currentNodeIndex];
				--n.nrGhosts;
				++b.graph.nodes[n.neighbours[moveIndex]].nrGhosts;
				b.ghosts[i].currentNodeIndex = n.neighbours[moveIndex];
				b.ghosts[i].lastMoveMade = n.neighbourMoves[moveIndex];
			}
		}
	}

	public void unmoveGhosts(Board b) {
		for (int i = 0; i < ghostMoveIndex.length; ++i) {
			if (ghostMoveIndex[i] >= 0) {
				--b.graph.nodes[b.ghosts[i].currentNodeIndex].nrGhosts;
				++b.graph.nodes[b.ghosts[i].currentNodeIndex].nrGhosts;
			}
		}
		b.copyFrom(savedBoard);
	}
	
	/**
	 * Copies the results of a search (best move, best value) from the given source.
	 * @param src
	 */
	public void copySearchResult(PlyInfo src) {
		budget = src.budget;
		bestValue = src.bestValue;
		for (int i = 0; i < bestGhostMove.length; ++i) {
			bestGhostMove[i] = src.bestGhostMove[i];
		}
		bestPacmanMove = src.bestPacmanMove;
	}
}
