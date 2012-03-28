package pacman.entries.pacman.graph;

import static pacman.game.Constants.COMMON_LAIR_TIME;
import static pacman.game.Constants.EAT_DISTANCE;
import static pacman.game.Constants.GHOST_EAT_SCORE;
import static pacman.game.Constants.LAIR_REDUCTION;

import java.util.Random;

import pacman.game.Game;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.internal.Ghost;

public class Search {
	private static final boolean log = false;
	/** Absolute maximum value */
	public static final int MAX_VALUE = 100000;
	/** score (for ghosts) when we know for sure pacman will die */
	public static final int PACMAN_DIES_VALUE = MAX_VALUE/2;
	public static final int PACMAN_WILL_DIE = MAX_VALUE/4;
	public static final int MAX_PLY = 1000;
	public static PlyInfo[] plyInfo = new PlyInfo[MAX_PLY];
	/** True if pacman moves at even plies (and ghosts at odd plies) */
	public static boolean pacmanMovesFirst = false;
	/** current ply that is being searched */
	private static int currDepth;
	public static Board b;
	public static JunctionGraph graph;
	public static Game game;
	/** shortcut for graph.nodes */
	public static Node[] nodes;
	/** total nr of nodes searched */
	public static int nodesSearched = 0;
	private static Random rand = new Random();
	/** If we reach this time, we must stop searching immediately */
	private static long emergencyStopTime;
	/** true if the search stopped in the middle due to emergencyStopTime being reached */
	private static boolean emergencyStopped;

	static {
		init();
	}

	private static void init() {
		for (int i = 0; i < plyInfo.length; ++i) {
			plyInfo[i] = new PlyInfo();
		}
	}

	public static void update(Board board, JunctionGraph newGraph, Game newGame) {
		b = board;
		graph = newGraph;
		nodes = graph.nodes;
		game = newGame;
	}

	public static void searchMove(long timeDue) {
		long startTime = System.currentTimeMillis();
		if (timeDue < 0) {
			timeDue = startTime + 20;
		}
		PlyInfo backup = new PlyInfo();
		Search.currDepth = 0;
		Search.nodesSearched = 0;
		PlyInfo p = plyInfo[0];
		p.alpha = -MAX_VALUE;
		p.beta = MAX_VALUE;
		p.score = 0;
		long normalStopTime = (startTime+timeDue)/2;
		emergencyStopTime = startTime + 100000; // we want to search at least 1 ply without emergency stops
		emergencyStopped = false;
		boolean stop = false;
		p.budget = 30;
		while (!stop) {
			search();
			if (!emergencyStopped) {
				// save the search results
				backup.copySearchResult(p);
			}
			emergencyStopTime = timeDue - 6; // now set the real emergency stop time, with a little slack.
			long timeSpent = System.currentTimeMillis() - startTime;
			stop = Math.abs(p.bestValue) >= PACMAN_WILL_DIE
					|| startTime + timeSpent >= normalStopTime;
			p.budget += 10;
		}
		if (emergencyStopped) {
			System.out.println("Search was emergency stopped");
			// search was stopped in the middle of a ply, cannot use search result
			// that currently is present in p.
			p.copySearchResult(backup);
			if (System.currentTimeMillis() >= timeDue) {
				System.err.println("TIME EXCEEDED");
			}
		}
	}

	/**
	 * Performs an alpha-beta search. Search depth is variable, continues until
	 * p.budget < 0.
	 */
	public static void search() {
		if (emergencyStopped) {
			log("Emergency stop");
			return;
		}
		if (nodesSearched % 500 == 0) {
			emergencyStopped = System.currentTimeMillis() >= emergencyStopTime;
		}
		++nodesSearched;
		PlyInfo p = plyInfo[currDepth];
		if (log) {
			log("search " + currDepth + ", nodes = "
					+ nodesSearched + " [" + p.alpha + ", " + p.beta + "]");
		}
		p.bestValue = -MAX_VALUE;
		boolean evenPly = (currDepth & 1) == 0;
		boolean movePacman = evenPly == pacmanMovesFirst;
		// on even plies, check pacman/ghost alive status
		if (evenPly) {
			// check for dead pacman or ghosts
			int value = _feast(movePacman);
			if (Math.abs(value) >= PACMAN_DIES_VALUE) {
				p.bestValue = value;
				return;
			}
			p.score += Math.abs(value);
			if (value != 0) System.out.println("p.score is now " + p.score);
			// static check if pacman is in danger
			value = checkPacmanHealth();
			if (value >= PACMAN_WILL_DIE && currDepth > 0) {
				p.bestValue = movePacman ? -value : value;
				return;
			}
		}
		p.moveScore = 0;
		// if no budget left we do a static analysis and return.
		// But only on even plies.
		if (evenPly && p.budget <= 0) {
			evaluate(p, movePacman);
			return;
		}
		boolean cutoff = false;
		p.initMove(movePacman);
		PlyInfo nextP = plyInfo[currDepth + 1];
		// loop through all moves
		while (!cutoff && p.nextMove(movePacman)) {
			if (log) {
				String onlyMove = p.nrPossibleMoves == 1 ? "; only move" : "";
				log((movePacman ? "pacman move" : "ghost move") + onlyMove);
			}
			int value = 0;
			p.move(movePacman);
			nextP.alpha = -p.beta;
			nextP.beta = -((p.alpha) > (p.bestValue) ? (p.alpha)
					: (p.bestValue));
			// determine cost for this ply
			int cost = 10;
			if (p.nrPossibleMoves == 1) {
				cost = 0;
			}
			nextP.budget = p.budget - cost;
			nextP.score = p.score + p.moveScore;
			++currDepth;
			search();
			--currDepth;
			value = -nextP.bestValue;
			p.unmove(movePacman);
			if (value > p.bestValue) {
				if (log) {
					log("New best value: " + value);
				}
				p.bestValue = value;
				p.saveBestMove(movePacman);
				if (value >= p.beta) {
					cutoff = true;
					if (log) {
						log("cutoff!");
					}
				}
			} else if (log) {
				log("search returned " + value);
			}
		}
	}

	/**
	 * Performs a static evaluation, and sets p.bestValue
	 * 
	 * @param p
	 * @param movePacman
	 */
	private static void evaluate(PlyInfo p, boolean movePacman) {
		// calculate how many junctions are closer to pacman than to any ghost
		int nrJunctionsClosestToPacman = 0;
		for (Node n : graph.junctionNodes) {
			int pacmanDist = game.getShortestPathDistance(b.pacmanLocation, n.index);
			boolean pacmanIsClosest = true;
			for (MyGhost ghost : b.ghosts) {
				if (ghost.canKill()) {
					int ghostDist = game.getShortestPathDistance(ghost.currentNodeIndex, n.index);
					if (ghostDist < pacmanDist) {
						pacmanIsClosest = false;
						break;
					}
				}
			}
			if (pacmanIsClosest) {
				++nrJunctionsClosestToPacman;
			}
		}
		// calculate distance of ghost that is nearest to pacman
		// + try to maximize distance to pacman for eadible ghosts
		int closestDist = 10000;
		int edibleDistSum = 0;
		for (MyGhost ghost : b.ghosts) {
			int dist = game.getShortestPathDistance(b.pacmanLocation,
					ghost.currentNodeIndex);
			if (ghost.edibleTime > 0) {
				edibleDistSum += dist;
			} else if (ghost.canKill()) {
				if (dist < closestDist) {
					closestDist = dist;
				}
			}
		}
		// being on a long big edge is potentially dangerous
		int edgeLength;
		Node pacmanNode = graph.nodes[b.pacmanLocation];
		if (pacmanNode.isJunction()) {
			// junction: edge length is minimum length of connected edges
			edgeLength = pacmanNode.edges[0].length;
			for (int i = 1; i < pacmanNode.nrEdges; ++i) {
				if (pacmanNode.edges[i].length < edgeLength) {
					edgeLength = pacmanNode.edges[i].length;
				}
			}
		} else {
			edgeLength = pacmanNode.edge.length;
		}
		//if (p.score > 0)System.out.println("evaluate: score = " + p.score);
		// value is relative to ghosts (positive value is bad for pacman)
		int value = 30*nrJunctionsClosestToPacman + 5*closestDist - 10*edibleDistSum 
				+ 3*edgeLength - p.score + rand.nextInt(20);
		if (movePacman) {
			p.bestValue = value;
		} else {
			p.bestValue = -value;
		}
		if (log) {
			log("eval: value = " + p.bestValue);
		}
	}
	
	private static int checkPacmanHealth() {
		Node pacmanNode = graph.nodes[b.pacmanLocation];
		if (pacmanNode.isJunction()) {
			return 0;
		}
		int nextAttackedJunction = -1;
		for (MyGhost ghost : b.ghosts) {
			if (ghost.lairTime == 0 && ghost.edibleTime == 0) {
				Node ghostNode = graph.nodes[ghost.currentNodeIndex];
				if (!ghostNode.isJunction() && ghostNode.edge == pacmanNode.edge && !pacmanNode.edge.containsPowerPill) {
					if (ghostNode.isOnPath(pacmanNode, ghost.lastMoveMade)) {
						// the ghost attacks pacman
						if (log) {
							log(ghost.ghost + " attacks pacman");
						}
						int nextJunction = ghostNode.getNextJunction(ghost.lastMoveMade);
						if (nextAttackedJunction == -1) {
							nextAttackedJunction = nextJunction;
						} else if (nextAttackedJunction != nextJunction) {
							// pacman attacked from 2 sides, he will die
							if (log) log("Pacman will die");
							return PACMAN_WILL_DIE;
						}
					}
				}
			}
		}
		if (nextAttackedJunction != -1) {
			// check if a ghost is closer to the junction than pacman
			int pacmanDist = game.getShortestPathDistance(b.pacmanLocation, nextAttackedJunction);
			for (MyGhost ghost : b.ghosts) {
				if (ghost.lairTime == 0 && ghost.edibleTime == 0) {
					Node ghostNode = graph.nodes[ghost.currentNodeIndex];
					int dist = game.getShortestPathDistance(ghost.currentNodeIndex, nextAttackedJunction);
					if (dist <= pacmanDist) {
						// looks dangerous
						//return PACMAN_WILL_DIE;
					}
				}
			}
		}
		return 0;
	}

	/**
	 * Checks for dead pacman or dead ghosts.
	 * Returns score points for killed ghosts.
	 * @param movePacman
	 * @return
	 */
	private static int _feast(boolean movePacman) {
		int score = 0;
		for (MyGhost ghost : b.ghosts) {
			int ghostEatMultiplier = 1;
			int distance = game.getShortestPathDistance(b.pacmanLocation,
					ghost.currentNodeIndex);

			if (distance <= EAT_DISTANCE && distance != -1) {
				if (ghost.edibleTime > 0) {
					// pac-man eats ghost
					log("ghost dies");
					score -= GHOST_EAT_SCORE * ghostEatMultiplier;
					ghostEatMultiplier *= 2;
					/*ghost.edibleTime = 0;
					ghost.lairTime = 1000;// (int)(COMMON_LAIR_TIME*(Math.pow(LAIR_REDUCTION,levelCount)));
					ghost.currentNodeIndex = -1;// currentMaze.lairNodeIndex;
					ghost.lastMoveMade = MOVE.NEUTRAL;*/
				} else {
					// ghost eats pac-man
					score = PACMAN_DIES_VALUE;
					log("pacman dies");
				}
			}
		}
		if (movePacman) {
			score = -score;
		}
		return score;
	}

	private static void log(String msg) {
		if (!log) {
			return;
		}
		for (int i = 0; i < currDepth; ++i) {
			Log.print("   ");
		}
		Log.print(currDepth + " ");
		Log.println(msg);
	}
}
