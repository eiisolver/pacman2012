package pacman.entries.pacman.graph;

import static pacman.game.Constants.COMMON_LAIR_TIME;
import static pacman.game.Constants.EAT_DISTANCE;
import static pacman.game.Constants.GHOST_EAT_SCORE;
import static pacman.game.Constants.LAIR_REDUCTION;

import java.util.Random;

import pacman.game.Game;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

public class Search {
	public static final boolean log = false;
	/**
	 * If true, we use pacman evaluation function.
	 */
	public static boolean pacmanEvaluation = true;
	public static EvaluationExtra evaluationExtra;
	/** Absolute maximum value */
	public static final int MAX_VALUE = 100000;
	/** score (for ghosts) when we know for sure pacman will die */
	public static final int PACMAN_DIES_VALUE = MAX_VALUE/2;
	public static final int PACMAN_WILL_DIE = MAX_VALUE/4;
	public static final int MAX_PLY = 700;
	public static PlyInfo[] plyInfo = new PlyInfo[MAX_PLY];
	/** True if pacman moves at even plies (and ghosts at odd plies) */
	public static boolean pacmanMovesFirst = false;
	/** current ply that is being searched */
	private static int currDepth;
	public static Board b;
	public static JunctionGraph graph;
	public static Game game;
	public static Heuristics heuristics = new Heuristics();
	/** shortcut for graph.nodes */
	public static Node[] nodes;
	/** total nr of nodes searched */
	public static int nodesSearched = 0;
	private static Random rand = new Random();
	/** If we reach this time, we must stop searching immediately */
	private static long emergencyStopTime;
	/** true if the search stopped in the middle due to emergencyStopTime being reached */
	private static boolean emergencyStopped;
	private static StaticEvaluator staticEvaluator = new StaticEvaluator();
	// helper variables used when calculating shortest path to all edible ghosts
	private static int[] edibleGhosts = new int[4];
	private static boolean[] edibleVisited = new boolean[4];
	private static int nrEdibleGhosts = 0;

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
		// just for performance measurement purposes
		for (int i = 0; i < plyInfo.length; ++i) {
			plyInfo[i].alpha = plyInfo[i].beta = 0;
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
		heuristics.updateForNewMove(game, b);
		boolean haveBackup = false;
		while (!stop) {
			search();
			if (!emergencyStopped) {
				if (p.bestValue > -PACMAN_WILL_DIE) {
					// save the search results
					backup.copySearchResult(p);
					haveBackup = true;
				}
			}
			emergencyStopTime = timeDue - 6; // now set the real emergency stop time, with a little slack.
			long timeSpent = System.currentTimeMillis() - startTime;
			stop = Math.abs(p.bestValue) >= PACMAN_WILL_DIE
					|| startTime + timeSpent >= normalStopTime
					|| log;
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
		} else if (p.bestValue <= -PACMAN_WILL_DIE && haveBackup) {
			// in our backup we did not loose, but in the last search we discovered that we
			// will loose anyway. We try to make it as hard as possible for the opponent,
			// so we choose the best move from the backup
			p.copySearchResult(backup);
			System.out.println("I will loose, select best move from backup");
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
		if (log) log("search " + currDepth + ", nodes = " + nodesSearched + " [" + p.alpha + ", " + p.beta + "]");
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
			p.score += value;
			if (currDepth < 2 || plyInfo[currDepth-2].nrPossibleMoves > 1 || plyInfo[currDepth-1].nrPossibleMoves > 1) {
				// static check if pacman is in danger
				value = checkPacmanHealth();
				if (value >= PACMAN_WILL_DIE && currDepth > 0) {
					p.bestValue = movePacman ? -value : value;
					return;
				}
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
		boolean skipOpposite = false;
		if (movePacman) {
			skipOpposite = currDepth >= 2 && plyInfo[currDepth-1].nrPossibleMoves == 1
					/*&& !plyInfo[currDepth-2].pillValue*/&& (currDepth & 7) != 0 && !plyInfo[currDepth-2].powerPillValue
					&& !plyInfo[currDepth & ~1].ghostKilled && !nodes[b.pacmanLocation].isJunction();
			if (log && skipOpposite) log("Skip opposite");
		}
		p.initMove(movePacman, skipOpposite);
		PlyInfo nextP = plyInfo[currDepth + 1];
		// loop through all moves
		while (!cutoff && p.nextMove(movePacman)) {
			if (log) {
				String onlyMove = p.nrPossibleMoves == 1 ? "; only move" : "";
				if (movePacman) {
					log("pacman move, " + p.moveToString(movePacman) +", p.score: " + p.score + onlyMove);
				} else {
					log("ghost move " + p.moveToString(movePacman) + onlyMove);
				}
			}
			int value = 0;
			p.move(movePacman);
			if (log&&movePacman&&p.moveScore != 0)log("moveScore: "+ p.moveScore);
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
					if (log) log("cutoff!");
				}
			} else if (log) {
				if(log)log("search returned " + value + ", best value = " + p.bestValue);
			}
		}
	}

	/**
	 * (only for performance measurement)
	 * @return
	 */
	public static int deepestSearchedPly() {
		for (int i = 0; i < plyInfo.length; ++i) {
			if (plyInfo[i].alpha == 0 && plyInfo[i].beta == 0) {
				return i-1;
			}
		}
		return -1;
	}
	
	/**
	 * Returns the time it will take the given ghost to travel dist.
	 * Edible ghosts take longer time, and if they are still edible after
	 * having travelled dist, a long time will be returned.
	 * @param ghost
	 * @param dist
	 * @return
	 */
	private static int ghostDist(MyGhost ghost, int dist) {
		if (ghost.edibleTime == 0) {
			return dist;
		}
		if (ghost.edibleTime > dist+dist) {
			return 1000; 
		}
		return dist + ghost.edibleTime/2;
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
				if (ghost.lairTime == 0) {
					int ghostDist = ghostDist(ghost, game.getShortestPathDistance(ghost.currentNodeIndex, n.index));
					if (ghostDist + EAT_DISTANCE < pacmanDist) {
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
		int closestDist = 400;
		int farAwayBonus = 0;
		nrEdibleGhosts = 0;
		int maxEdibleTime = 0;
		for (MyGhost ghost : b.ghosts) {
			if (ghost.lairTime == 0) {
				int dist = game.getShortestPathDistance(b.pacmanLocation,
						ghost.currentNodeIndex);
				if (ghost.edibleTime > 0) {
					edibleGhosts[nrEdibleGhosts] = ghost.currentNodeIndex;
					edibleVisited[nrEdibleGhosts] = false;
					++nrEdibleGhosts;
					if (ghost.edibleTime > maxEdibleTime) {
						maxEdibleTime = ghost.edibleTime;
					}
				} else if (ghost.canKill()) {
					if (dist < closestDist) {
						closestDist = dist;
					}
					if (dist > 60) {
						farAwayBonus += 4*(dist - 60);
					}
				}
			}
		}
		// calculate shortest path to eat all edible ghosts, assuming pacman is greedy;
		// first moves to closest ghost, then to next, etc.
		int edibleBonus = 0;
		if (nrEdibleGhosts > 0) {
			if(log)log("nrEdible = " + nrEdibleGhosts);
			if (pacmanEvaluation) {
				// pacman will move to closest edible ghost
				int shortestDist = 10000;
				int nearestGhost = -1;
				for (int g = 0; g < nrEdibleGhosts; ++g) {
					int dist = game.getShortestPathDistance(b.pacmanLocation, edibleGhosts[g]);
					dist += dist/3; // assume ghost moves away
					if (dist < shortestDist) {
						shortestDist = dist;
						nearestGhost = g;
					}
				}
				if(log)log("nearest ghost: " + shortestDist + ", maxEdible = " + maxEdibleTime);
				if (shortestDist < maxEdibleTime) {
					boolean killingGhostIsCloser = false;
					for (MyGhost ghost : b.ghosts) {
						if (ghost.canKill()) {
							if (game.getShortestPathDistance(ghost.currentNodeIndex, edibleGhosts[nearestGhost]) < shortestDist) {
								if(log)log("killing ghost is closer ");
								killingGhostIsCloser = true;
								break;
							}
						}
					}
					if (!killingGhostIsCloser) {
						if(log)log("edible bonus: " + edibleBonus);
						edibleBonus = 10*GHOST_EAT_SCORE-10*shortestDist;
					}
				}
				
			} else {
				// ghosts will try to spread out
				int pathLength = 0; // will contain the length of the path that eats all ghosts
				int currNode = b.pacmanLocation;
				for (int i = 0; i < nrEdibleGhosts; ++i) {
					int shortestDist = 10000;
					int nearestGhost = -1;
					for (int g = 0; g < nrEdibleGhosts; ++g) {
						if (!edibleVisited[g]) {
							int dist = game.getShortestPathDistance(currNode, edibleGhosts[g]);
							if (dist < shortestDist) {
								shortestDist = dist;
								nearestGhost = g;
							}
						}
					}
					pathLength += shortestDist;
					currNode = edibleGhosts[nearestGhost];
					edibleVisited[nearestGhost] = true;
					if (pathLength > maxEdibleTime) {
						break;
					}
				}
				edibleBonus = -40*pathLength;
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
		// value is relative to pacman (positive value is good for pacman)
		int value = p.score + edibleBonus;
		if (pacmanEvaluation) {
			value += nrJunctionsClosestToPacman + rand.nextInt(4);
		} else {
			value = 20*nrJunctionsClosestToPacman + 5*closestDist + farAwayBonus + rand.nextInt(20)
					+ (p.score + edibleBonus)/80;
		}
		p.bestValue = value;
		// hook to add some ugly extra evaluation stuff
		if (evaluationExtra != null) {
			//evaluationExtra.evaluateExtra(p);
		}
		if (!movePacman) {
			p.bestValue = -value;
		}
		if (log) {
			log("eval: value = " + p.bestValue + ", junctions to pacman: " + nrJunctionsClosestToPacman
					+ (edibleBonus != 0 ? "" : ", edible: " + edibleBonus)
					+ ", score: " + p.score);
		}
	}
	
	
	public static int checkPacmanHealth() {
		Node pacmanNode = graph.nodes[b.pacmanLocation];
		// check if pacman can get safely to a power pill
		for (int i = 0; i < b.nrPowerPills; ++i) {
			int powerPill = b.powerPillLocation[i];
			if (b.containsPowerPill[powerPill]) {
				int pacmanDist = game.getShortestPathDistance(b.pacmanLocation, powerPill);
				int ghostDist = 100000;
				for (MyGhost ghost : b.ghosts) {
					if (ghost.lairTime == 0) {
						// TODO: not really accurate to use getShortestPathDistance
						int dist = ghostDist(ghost, game.getShortestPathDistance(ghost.currentNodeIndex, powerPill));
						if (dist < ghostDist) {
							ghostDist = dist;
						}
					}
				}
				if (pacmanDist + EAT_DISTANCE < ghostDist) {
					// we have found a power pill that is closer to pacman than to any ghost.
					// pacman is safe.
					return 0;
				}
			}
		}
		boolean pacmanDies = false;
		if (pacmanNode.isJunction()) {
			staticEvaluator.nrJunctions = pacmanNode.nrNeighbours;
			for (int i = 0; i < pacmanNode.nrNeighbours; ++i) {
				BigEdge edge = nodes[pacmanNode.neighbours[i]].edge;
				Node otherJunction = edge.getOtherJunction(pacmanNode);
				staticEvaluator.edges[i] = edge;
				staticEvaluator.junctions[i] = otherJunction.index;
				staticEvaluator.viaJunctions[i] = otherJunction.index;
			}
			pacmanDies = staticEvaluator.checkPacmanHealth("pacman on junction");
		} else {
			// check if there are two other ghosts on the same edge as pacman, attacking from 
			// both sides
			BigEdge pacmanEdge = pacmanNode.edge;
			pacmanDies = staticEvaluator.checkPacmanHealth(pacmanEdge.endpoints[0].index, pacmanEdge, 
					pacmanEdge.endpoints[1].index, pacmanEdge, "direct pacman edge");
			if (!pacmanDies) {
				pacmanDies = checkPacmanEdgeJunction(pacmanEdge.endpoints[0].index);
			}
			if (!pacmanDies) {
				pacmanDies = checkPacmanEdgeJunction(pacmanEdge.endpoints[1].index);
			}
			if (!pacmanDies && pacmanEdge.endpoints[0].nrNeighbours + pacmanEdge.endpoints[1].nrNeighbours <= 6) {
				// pacman will die if ghosts are closer to the 4 junctions that are 2 junctions away from pacman
				int nrJunctions = 0;
				for (int e = 0; e < 2; ++e) {
					Node junction = pacmanEdge.endpoints[e];
					for (int i = 0; i < junction.nrNeighbours; ++i) {
						BigEdge edge = junction.edges[i];
						if (edge != pacmanEdge) {
							// otherJunction lies 2 junctions away
							Node otherJunction = edge.getOtherJunction(pacmanEdge.endpoints[e]);
							staticEvaluator.edges[nrJunctions] = edge;
							staticEvaluator.junctions[nrJunctions] = otherJunction.index;
							staticEvaluator.viaJunctions[nrJunctions] = junction.index;
							++nrJunctions;
						}
					}
				}
				staticEvaluator.nrJunctions = nrJunctions;
				pacmanDies = staticEvaluator.checkPacmanHealth("pacman 2 junctions away");
			}
		}
		return pacmanDies ? PACMAN_WILL_DIE : 0;
	}
	
	private static boolean checkPacmanEdgeJunction(int junction) {
		Node pacmanNode = graph.nodes[b.pacmanLocation];
		BigEdge pacmanEdge = pacmanNode.edge;
		Node junction2 = pacmanEdge.getOtherJunction(nodes[junction]);
		staticEvaluator.edges[0] = pacmanEdge;
		staticEvaluator.junctions[0] = junction;
		staticEvaluator.viaJunctions[0] = junction;
		int nrJunctions = 1;
		for (int i = 0; i < junction2.nrNeighbours; ++i) {
			BigEdge edge = nodes[junction2.neighbours[i]].edge;
			if (edge != pacmanEdge) {
				Node otherJunction = edge.getOtherJunction(junction2);
				staticEvaluator.edges[nrJunctions] = edge;
				staticEvaluator.junctions[nrJunctions] = otherJunction.index;
				staticEvaluator.viaJunctions[nrJunctions] = junction2.index;
				staticEvaluator.firstMoveFromJunctions[nrJunctions] = edge.getFirstMove(otherJunction);
				++nrJunctions;
			}
		}
		staticEvaluator.nrJunctions = nrJunctions;
		boolean pacmanDies = staticEvaluator.checkPacmanHealth("pacman edge/junctions");
		return pacmanDies;
	}
	
	/**
	 * Checks for dead pacman or dead ghosts.
	 * Returns score points for killed ghosts. (stolen from Game._feast)
	 * @param movePacman
	 * @return
	 */
	private static int _feast(boolean movePacman) {
		int score = 0;
		plyInfo[currDepth].ghostKilled = false;
		for (MyGhost ghost : b.ghosts) {
			if (ghost.lairTime == 0) {
				int ghostEatMultiplier = 1;
				int distance = game.getShortestPathDistance(b.pacmanLocation,
						ghost.currentNodeIndex);
	
				if (distance <= EAT_DISTANCE && distance != -1) {
					if (ghost.edibleTime > 0) {
						// pac-man eats ghost
						if(log)log("ghost dies, within eat distance");
						score -= 10*GHOST_EAT_SCORE * ghostEatMultiplier;
						ghostEatMultiplier *= 2;
						ghost.edibleTime = 0;
						ghost.lairTime = b.currentEdibleTime;
						ghost.currentNodeIndex = -1;// currentMaze.lairNodeIndex;
						ghost.lastMoveMade = MOVE.NEUTRAL;
						plyInfo[currDepth].ghostKilled = true;
					} else {
						// ghost eats pac-man
						score = PACMAN_DIES_VALUE;
						if(log)log("pacman dies, within eat distance");
					}
				}
			}
		}
		if (score != PACMAN_DIES_VALUE && b.nrPillsLeft == 0 && b.nrPowerPillsLeft == 0) {
			// pacman has eaten all pills, to next level
			score -= PACMAN_DIES_VALUE;
		}
		if (movePacman) {
			score = -score;
		}
		return score;
	}

	public static boolean skipMoveTowardsGhost(int destLocation) {
		if (currDepth == 0) {
			return false;
		}
		Node destNode = nodes[destLocation];
		boolean skip = false;
		if (!destNode.isJunction()) {
			BigEdge edge = destNode.edge;
			for (int g = 0; g < b.ghosts.length; ++g) {
				MyGhost ghost = b.ghosts[g];
				if (ghost.canKill()) {
					Node ghostNode = nodes[ghost.currentNodeIndex];
					if (ghostNode.edge == edge && ghostNode.isOnPath(destNode, ghost.lastMoveMade)) {
						int pacmanDist = game.getShortestPathDistance(b.pacmanLocation, ghost.currentNodeIndex);
						int newDist = Math.abs(destNode.edgeIndex - ghostNode.edgeIndex);
						if (newDist < pacmanDist) {
							// the move is towards the ghost; check if there is something interesting on the
							// path halfway to the ghost
							int middle = (destNode.edgeIndex + ghostNode.edgeIndex)/2;
							int step = middle >= destNode.edgeIndex ? 1 : -1;
							for (int i = destNode.edgeIndex; i != middle; i += step) {
								Node n = edge.internalNodes[i];
								if (b.containsPill[n.index] || b.containsPowerPill[n.index]) {
									return false;
								}
							}
							skip = true; // no, nothing interesting
						}
					}
				} else if (ghost.lairTime == 0 && nodes[ghost.currentNodeIndex].edge == edge) {
					// edible ghost is on the path; don't skip under any condition
					return false;
				}
			}
		}
		return skip;
	}

	public static void log(String msg) {
		if (!log) {
			return;
		}
		for (int i = 0; i < currDepth; ++i) {
			Log.print("   ");
		}
		Log.print(currDepth + " ");
		Log.println(msg);
	}
	
	// ############################################################################
	// StaticEvaluator
	// ############################################################################

	/** 
	 * Helper class used to check statically if pacman can escape via a set
	 * of provided junctions.
	 * @author louis
	 *
	 */
	private static class StaticEvaluator {
		/** junctions of interest */
		public int[] junctions = new int[4];
		/** via which junction */
		public int[] viaJunctions = new int[4];
		/** edges of interest */
		public BigEdge[] edges = new BigEdge[4];
		public int nrJunctions;
		/** The first move from the junction to go towards pacman */
		private MOVE[] firstMoveFromJunctions = new MOVE[4];
		//boolean[] includedGhosts = new boolean[GHOST.values().length];
		private int[] pacmanDist = new int[4];
		/**
		 * ghostIsCloser[ghost] contains bitmap, if bit set is true -> ghost is closer to that junction than pacman
		 */
		private int[] ghostIsCloser;
		
		public boolean checkPacmanHealth(int junction1, BigEdge edge1, int junction2, BigEdge edge2, String logMsg) {
			nrJunctions = 2;
			junctions[0] = junction1;
			viaJunctions[0] = junction1;
			junctions[1] = junction2;
			viaJunctions[1] = junction2;
			edges[0] = edge1;
			edges[1] = edge2;
			return checkPacmanHealth(logMsg);
		}

		/**
		 * Returns true if pacman will die.
		 * @param logMsg
		 * @return
		 */
		public boolean checkPacmanHealth(String logMsg) {
			Node pacmanNode = nodes[b.pacmanLocation];
			if (log)log("checkPacmanHealth " + logMsg + ", pacman at " + pacmanNode);
			for (int i = 0; i < nrJunctions; ++i) {
				if (pacmanNode.isJunction()) {
					pacmanDist[i] = game.getShortestPathDistance(b.pacmanLocation, junctions[i])+ EAT_DISTANCE;
				} else {
					pacmanDist[i] = pacmanNode.edge.getDistanceToJunction(pacmanNode, nodes[viaJunctions[i]]) + EAT_DISTANCE;
					if (viaJunctions[i] != junctions[i]) {
						pacmanDist[i] += game.getShortestPathDistance(viaJunctions[i], junctions[i]);
					}
				}
				firstMoveFromJunctions[i] = edges[i].getFirstMove(nodes[junctions[i]]);
				if (log)log(i + ": " + nodes[junctions[i]] + ", pacman dist " + pacmanDist[i]);
			}
			ghostIsCloser = new int[GHOST.values().length];
			for (int j = 0; j < nrJunctions; ++j) {
				boolean someGhostIsCloser = false;
				for (int g = 0; g < b.ghosts.length; ++g) {
					MyGhost ghost = b.ghosts[g];
					if (ghost.lairTime == 0) {
						Node ghostNode = nodes[ghost.currentNodeIndex];
						if (ghostNode.edge == edges[j] && ghostNode.getNextJunction(ghost.lastMoveMade) != junctions[j]) {
							// ghost is already on the edge, on the move to pacman. Pacman cannot escape via this junction
							// unless ghost is on same edge as pacman and already past pacman.
							if (pacmanNode.edge != ghostNode.edge || ghostNode.isOnPath(pacmanNode, ghost.lastMoveMade)) {
								ghostIsCloser[g] |= 1 << j;
								someGhostIsCloser = true;
								if (log)log(j + ": ghost on same edge, " + ghostNode);
							}
						} else {
							int dist1 = ghostDist(ghost, game.getShortestPathDistance(ghost.currentNodeIndex, junctions[j]));
							if (dist1 <= pacmanDist[j]) {
								// ghost is closer, but can it also move to the junction
								int dist = ghostDist(ghost, graph.getGhostDistToJunction(ghost.currentNodeIndex, ghost.lastMoveMade, 
										junctions[j], firstMoveFromJunctions[j]));
								if (dist <= pacmanDist[j]) {
									ghostIsCloser[g] |= 1 << j;
									someGhostIsCloser = true;
									if (log)log(j + ": closer ghost: " + ghostNode + ", dist: " + dist);
								} else {
									if (log)log(j + ": ghost " + ghostNode + " shortest dist = " + dist1 + ", real dist: " + dist);
								}
							}
						}
					}
				}
				if (!someGhostIsCloser) {
					return false; // pacman can escape safely to junction j
				}
			}
			if (log) log("ghosts are closer; check for match");
			// we have some ghost closer to every junction. But it must be distinct ghosts! (one ghost can only cover 1 junction)
			if (match(ghostIsCloser, nrJunctions)) {
				if (log) log("Pacman will die: " + logMsg);
				return true;
			}
			if (log) log("no match, pacman survives");
			return false;
		}
	}
	
	private static class StaticEvaluator2 {
		
	}
	
	/**
	 * check that every bit 0..n-1 is covered at least once by different ghosts
	 * @param arr every element contains bit mask for a ghost, bit set means ghost is closer to junction than pacman
	 * @param n
	 * @return
	 */
    private static boolean match(int[] arr, int n) {
        if (n == 0) {
            return true;
        }
        for (int i = 0; i < arr.length; ++i) {
            for (int j = 0; j < n; ++j) {
                if ((arr[i] & (1 <<j)) != 0) {
                    int mask = (1 << j) - 1;
                    int[] arr2 = new int[arr.length];
                    for (int k = 0; k < arr.length; ++k) {
                        if (k == i) {
                            arr2[k] = 0;
                        } else {
                            arr2[k] = (arr[k] & mask)
                                    | ((arr[k] >> 1) & ~mask);
                        }
                    }
                    if (match(arr2, n-1)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
