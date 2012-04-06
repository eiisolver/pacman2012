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
	/** contains the moves that pacman can make at this ply; indices into pacmanLocation.neighbours */
	int[] pacmanMoves = new int[4];
	int[] ghostMoveIndex;
	/** Points scored at this move */
	int moveScore;
	/** if a pill was eaten at this move */
	boolean pillValue;
	/** if a power pill was eaten at this move */
	boolean powerPillValue;
	/** if a ghost was killed at this move */
	boolean ghostKilled;
	
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
	public void initMove(boolean movePacman, boolean skipOpposite) {
		currMoveNr = 0;
		if (movePacman) {
			Node n = Search.nodes[Search.b.pacmanLocation];
			nrPossibleMoves = 0;
			int[] killerMoves = Search.pacmanKillerMoves[Search.b.pacmanLocation];
			for (int e = 0; e < n.nrNeighbours; ++e) {
				int index = killerMoves[e];
				if (!Search.skipMoveTowardsGhost(n.neighbours[index])) {
					pacmanMoves[nrPossibleMoves] = index;
					++nrPossibleMoves;
				} else if (Search.log)Search.log("Skip move towards ghost: " + n.neighbourMoves[e]);
			}
			if (nrPossibleMoves == 0) {
				// all moves are towards a ghost; just pick a random one
				pacmanMoves[0] = 0;
				nrPossibleMoves = 1;
			} else if (nrPossibleMoves == n.nrNeighbours && skipOpposite) {
				nrPossibleMoves = 0;
				// no moves were skipped due to moving to ghost; do move generation once more; skip opposite move
				for (int e = 0; e < n.nrNeighbours; ++e) {
					int index = killerMoves[e];
					if (n.neighbourMoves[index] != Search.b.pacmanLastMove.opposite()) {
						pacmanMoves[nrPossibleMoves] = index;
						++nrPossibleMoves;
					}
				}
			}
			pacmanMoveIndex = -1;
		} else {
			nrPossibleMoves = 1;
			for (int i = 0; i < ghostMoveIndex.length; ++i) {
				MyGhost ghost = Search.b.ghosts[i];
				if (ghost.lairTime > 0 || (ghost.edibleTime > 0 && (ghost.edibleTime & 1) == 0)) {
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
			bestPacmanMove = pacmanMoves[pacmanMoveIndex];
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
				return Search.b.graph.nodes[Search.b.pacmanLocation].neighbourMoves[pacmanMoves[pacmanMoveIndex]].toString();
			} else {
				return "NEUTRAL";
			}
		} else {
			StringBuilder buf = new StringBuilder();
			for (int i = 0; i < ghostMoveIndex.length; ++i) {
				int index = Search.b.ghosts[i].currentNodeIndex;
				if (index >= 0) {
					Node n = Search.b.graph.nodes[index];
					buf.append("(" + n.y + "," + n.x + ",");
					int moveIndex = ghostMoveIndex[i];
					if (moveIndex >= 0) {
						buf.append(n.neighbourMoves[moveIndex].toString());
					} else {
						buf.append("NEUTRAL");
					}
					buf.append(") ");
				}
			}
			return buf.toString();
		}
	}

	public void movePacman(Board b) {
		moveScore = 0;
		savedBoard.copyFrom(b);
		if (pacmanMoveIndex >= 0) {
			moveScore -= Search.heuristics.getNodeScore(b.pacmanLocation);
			Node n = b.graph.nodes[b.pacmanLocation];
			int index = pacmanMoves[pacmanMoveIndex];
			b.pacmanLocation = n.neighbours[index];
			b.pacmanLastMove = n.neighbourMoves[index];
			moveScore += Search.heuristics.getNodeScore(b.pacmanLocation);
			pillValue = b.containsPill[b.pacmanLocation];
			if (pillValue) {
				--b.nrPillsLeft;
				moveScore += Search.heuristics.getPillScore(b.pacmanLocation);
			}
			b.containsPill[b.pacmanLocation] = false;
			powerPillValue = b.containsPowerPill[b.pacmanLocation];
			if (powerPillValue) {
				--b.nrPowerPillsLeft;
				moveScore += Search.heuristics.getPowerPillScore(b.pacmanLocation);
				for (int i = 0; i < ghostMoveIndex.length; ++i) {
					MyGhost ghost = b.ghosts[i];
					ghost.edibleTime = Search.b.currentEdibleTime;
					ghost.lastMoveMade = ghost.lastMoveMade.opposite();
				}
			}
			b.containsPowerPill[b.pacmanLocation] = false;
		}
	}

	public void unmovePacman(Board b) {
		if (pillValue) {
			++b.nrPillsLeft;
		}
		b.containsPill[b.pacmanLocation] = pillValue;
		if (powerPillValue) {
			++b.nrPowerPillsLeft;
		}
		b.containsPowerPill[b.pacmanLocation] = powerPillValue;
		if (pacmanMoveIndex >= 0) {
			b.copyFrom(savedBoard);
		}
	}

	public void moveGhosts(Board b) {
		savedBoard.copyFrom(b);
		for (int i = 0; i < ghostMoveIndex.length; ++i) {
			MyGhost ghost = b.ghosts[i];
			int moveIndex = ghostMoveIndex[i];
			if (moveIndex >= 0) {
				Node n = b.graph.nodes[b.ghosts[i].currentNodeIndex];
				ghost.currentNodeIndex = n.neighbours[moveIndex];
				ghost.lastMoveMade = n.neighbourMoves[moveIndex];
			}
			if (ghost.edibleTime > 0) {
				--ghost.edibleTime;
			}
			if (ghost.lairTime > 0) {
				--ghost.lairTime;
				if (ghost.lairTime == 0) {
					ghost.currentNodeIndex = Search.game.getGhostInitialNodeIndex();
					ghost.edibleTime = 0;
					ghost.lastMoveMade = MOVE.NEUTRAL;
				}
			}
		}
	}

	public void unmoveGhosts(Board b) {
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
