package pacman.entries.ghosts.graph;

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
	private static final boolean log = true;
	/** Absolute maximum value */
	public static final int MAX_VALUE = 100000;
	/** score (for ghosts) when we know for sure pacman will die */
	public static final int PACMAN_DIES_VALUE = MAX_VALUE/2;
	public static final int PACMAN_WILL_DIE = MAX_VALUE/4;
	public static final int MAX_PLY = 40;
	public static PlyInfo[] plyInfo = new PlyInfo[MAX_PLY];
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

	public static void searchMove() {
		Search.currDepth = 0;
		Search.nodesSearched = 0;
		PlyInfo p = plyInfo[0];
		p.alpha = -MAX_VALUE;
		p.beta = MAX_VALUE;
		long startTime = System.currentTimeMillis();
		boolean stop = false;
		p.budget = 80;
		while (!stop) {
			search();
			long timeSpent = System.currentTimeMillis() - startTime;
			stop = Math.abs(p.bestValue) >= PACMAN_WILL_DIE
					|| timeSpent >= 4;
			p.budget += 10;
		}
	}

	/**
	 * Performs an alpha-beta search. Search depth is variable, continues until
	 * p.budget < 0.
	 */
	public static void search() {
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
			// static check if pacman is in danger
			value = checkPacmanHealth();
			if (value >= PACMAN_WILL_DIE && currDepth > 0) {
				p.bestValue = movePacman ? -value : value;
				return;
			}
		}
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
		int distSum = 0;
		int edibleDistSum = 0;
		int[] targets = new int[2];
		boolean used[] = new boolean[b.ghosts.length];
		Node n = nodes[b.pacmanLocation];
		targets[0] = b.pacmanLocation;
		if (n.isJunction()) {
			for (int i = 0; i < targets.length - 1; ++i) {
				targets[i+1] = n.neighbours[i];
			}
		} else {
			targets[1] = n.edge.endpoints[1].index;
			/*targets[2] = nodes[targets[0]].neighbours[0];
			if (targets[2] == targets[0]) {
				targets[2] = nodes[targets[0]].neighbours[1];
			}
			targets[3] = nodes[targets[1]].neighbours[0];
			if (targets[3] == targets[1]) {
				targets[3] = nodes[targets[1]].neighbours[1];
			}*/
		}
		for (int t = 0; t < targets.length; ++t) {
			int closestG = 0;
			int shortestDist = 10000;
			for (int g = 0; g < b.ghosts.length; ++g) {
				if (!used[g]) {
					MyGhost ghost = b.ghosts[g];
					int dist = game.getShortestPathDistance(targets[t], ghost.currentNodeIndex);
					if (dist < shortestDist) {
						shortestDist = dist;
						closestG = g;
					}
				}
			}
			distSum += shortestDist;
			used[closestG] = true;
		}
		for (MyGhost ghost : b.ghosts) {
			int dist = game.getShortestPathDistance(b.pacmanLocation,
					ghost.currentNodeIndex);
			if (ghost.edibleTime > 0) {
				edibleDistSum += dist;
			} 
		}
		
		int value = distSum - 10*edibleDistSum + rand.nextInt(50);
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
				if (!ghostNode.isJunction() && ghostNode.edge == pacmanNode.edge) {
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
