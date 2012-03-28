package pacman.entries.ghosts.graph;

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

	// ############################################################################
	// SAVED PARAMETERS
	// ############################################################################

	/** Board as it was at the beginning of the ply */
	Board savedBoard;
	/** Number of possible moves */
	int nrPossibleMoves;
	/** Current move nr; ranges from 0 (first move searched) to nrPossibleMoves */
	int currMoveNr;
	int pacmanMoveIndex;
	int[] ghostMoveIndex;
	
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

	public void movePacman(Board b) {
		savedBoard.copyFrom(b);
		if (pacmanMoveIndex >= 0) {
			Node n = b.graph.nodes[savedBoard.pacmanLocation];
			b.pacmanLocation = n.neighbours[pacmanMoveIndex];
			b.pacmanLastMove = n.neighbourMoves[pacmanMoveIndex];
		}
	}

	public void unmovePacman(Board b) {
		if (pacmanMoveIndex >= 0) {
			b.copyFrom(savedBoard);
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
}
