package pacman.entries.pacman.graph;

import java.util.*;
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
	public static Random rand = new Random();
	/** If we reach this time, we must stop searching immediately */
	private static long emergencyStopTime;
	/** true if the search stopped in the middle due to emergencyStopTime being reached */
	private static boolean emergencyStopped;
	private static StaticEvaluator staticEvaluator = new StaticEvaluator();
	// helper variables used when calculating shortest path to all edible ghosts
	private static int[] edibleGhosts = new int[4];
	private static boolean[] edibleVisited = new boolean[4];
	private static int nrEdibleGhosts = 0;
	/** pacmanKillerMoves[node] contains indices into nodes[node].neighbours. */
	public static int[][] pacmanKillerMoves;
	/** ghostKillerMoves[node][move] contains indices into nodes[node].neighbours. */
	public static int[][][] ghostKillerMoves;
	/** Used in debugging: see where pacman moved during a search */
	public static boolean[] pacmanVisited;
	/** helper variable */
	private static boolean pacmanCanGetToPowerPill;


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
		pacmanKillerMoves = new int[nodes.length][4];
		for (int n = 0; n < nodes.length; ++n) {
			for (int i = 0; i < nodes[n].nrNeighbours; ++i) {
				pacmanKillerMoves[n][i] = i;
			}
		}
		ghostKillerMoves = new int[nodes.length][MOVE.values().length][4];
		for (int n = 0; n < nodes.length; ++n) {
			for (MOVE m : MOVE.values()) {
				for (int i = 0; i < nodes[n].nrNeighbours; ++i) {
					ghostKillerMoves[n][m.ordinal()][i] = i;
				}
			}
		}
		pacmanVisited = new boolean[nodes.length];
		staticEval2.update();
	}
	
	public static void searchMove(Game newGame, long timeDue) {
		game = newGame;
		long startTime = System.currentTimeMillis();
		if (timeDue < 0) {
			timeDue = startTime + 20;
		}
		// just for performance measurement purposes
		for (int i = 0; i < plyInfo.length; ++i) {
			plyInfo[i].alpha = plyInfo[i].beta = 0;
		}
		Arrays.fill(pacmanVisited, false);
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
		p.budget = 20;
		heuristics.updateForNewMove(game, b);
		TransposTable.toggleMoveMask();
		boolean haveBackup = false;
		while (!stop) {
			p.budget += 10;
			if(log)log("searchMove, budget = " + p.budget);
			search();
			if (!emergencyStopped) {
				if (p.bestValue > -PACMAN_WILL_DIE) {
					// save the search results
					backup.copySearchResult(p);
					haveBackup = true;
				}
			}
			emergencyStopTime = timeDue - 10; // now set the real emergency stop time, with a little slack.
			long timeSpent = System.currentTimeMillis() - startTime;
			stop = Math.abs(p.bestValue) >= PACMAN_WILL_DIE
					|| startTime + timeSpent >= normalStopTime
					/*|| log*/;
			// reduce max time if everything looks ok
		    if (!stop && pacmanEvaluation && game.getCurrentLevel() > 1) {
		    	if (game.getPacmanNumberOfLivesRemaining() > 2) {
		    		stop = timeSpent > 5;
		    	} else {
		    		stop = timeSpent > 10;
		    	}
		    }
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
		if (nodesSearched % 200 == 0) {
			emergencyStopped = System.currentTimeMillis() >= emergencyStopTime;
		}
		++nodesSearched;
		PlyInfo p = plyInfo[currDepth];
		if (log) log("search " + currDepth + ", nodes = " + nodesSearched + " [" + p.alpha + ", " + p.beta + "]");
		p.bestValue = -MAX_VALUE;
		boolean evenPly = (currDepth & 1) == 0;
		boolean movePacman = evenPly == pacmanMovesFirst;
		// if we are lucky we can skip searching.
		if (TransposTable.retrieve(b, p, movePacman)) {
			if (log) {
				if (movePacman) {
					log("From transpos: pacman move, " + nodes[b.pacmanLocation].neighbourMoves[p.bestPacmanMove] + ", value: " + p.bestValue);
				} else {
					log("From transpos: ghost move , value: " + p.bestValue);
				}
			}
			return;
		}
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
			Node pacmanNode = nodes[b.pacmanLocation];
			skipOpposite = currDepth >= 2 && plyInfo[currDepth-1].nrPossibleMoves == 1
					/*&& !plyInfo[currDepth-2].pillValue*/ && !plyInfo[currDepth-2].powerPillValue
					&& !plyInfo[currDepth & ~1].ghostKilled && !pacmanNode.isJunction()
					&& pacmanNode.distToClosestJunction < 2
					&& (pacmanNode.edgeIndex & 15) != 4;
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
			pacmanVisited[b.pacmanLocation] = true;
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
		if (p.nrPossibleMoves > 1 && !emergencyStopped) {
			updateKillerMoves(movePacman);
			TransposTable.store(b, p, movePacman);
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
		/*int nrJunctionsClosestToPacman = 0;
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
		int graphBonus = 20*nrJunctionsClosestToPacman;*/
		int graphBonus = 400;
		if (!pacmanCanGetToPowerPill || heuristics.getPowerPillScore() < 0) {
			calcBorderEdges();
			if (staticEval2.borders.size() < 8 && staticEval2.nrPacmanNodes < 8) {
				graphBonus = 20*staticEval2.borders.size() + 20*staticEval2.nrPacmanNodes - 30*staticEval2.getNrInvolvedGhosts();
				if (staticEval2.hasCircles) {
					graphBonus += 80;
				} else if (staticEval2.borders.size() - staticEval2.getNrInvolvedGhosts() <= 1) {
					graphBonus -= 100;
				}
				if (staticEval2.match()) {
					graphBonus -= 1000;
				}
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
				//int dist = graph.getGhostDistToJunction(ghost.currentNodeIndex, ghost.lastMoveMade, nextPacmanJunction.index, nextPacmanJunction.neighbourMoves[0]);
				int dist = game.getShortestPathDistance(b.pacmanLocation, ghost.currentNodeIndex);
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
					if (dist > 40) {
						farAwayBonus += 5*(dist - 40);
					}
				}
			}
		}
		// calculate shortest path to eat all edible ghosts, assuming pacman is greedy;
		// first moves to closest ghost, then to next, etc.
		int edibleBonus = 0;
		int longestDist = 0;
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
					if (dist > longestDist) {
						longestDist = dist;
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
			if (heuristics.hasManyLivesLeft()) {
				// many lives left, we can take a risk and try to optimize score
				if (graphBonus < 0) {
					value += 4*graphBonus;
				} 
			} else {
				value += 4*graphBonus;
			}
			value += rand.nextInt(4);
		} else {
			value = graphBonus + 5*closestDist + farAwayBonus + rand.nextInt(5)
					+ (p.score + edibleBonus)/80;
			if (heuristics.assumeWeakPacman()) {
				// keep some ghosts far away
				value -= 3*(100-longestDist);
			}
			log("value = " + value);
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
			log("eval: value = " + p.bestValue + ", graphBonus: " + graphBonus
					+ ", far: " + farAwayBonus + ", close: " + closestDist
					+ (edibleBonus != 0 ? "" : ", edible: " + edibleBonus)
					+ ", score: " + p.score);
		}
	}
	
	
	public static int checkPacmanHealth() {
		Node pacmanNode = graph.nodes[b.pacmanLocation];
		// check if pacman can get safely to a power pill
		pacmanCanGetToPowerPill = false;
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
					pacmanCanGetToPowerPill = true;
					return 0;
				}
			}
		}
		boolean pacmanDies = false;
		int difficulty = 0;
		if (pacmanNode.isJunction()) {
			difficulty = 10;
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
			difficulty = 0;
			BigEdge pacmanEdge = pacmanNode.edge;
			pacmanDies = staticEvaluator.checkPacmanHealth(pacmanEdge.endpoints[0].index, pacmanEdge, 
					pacmanEdge.endpoints[1].index, pacmanEdge, "direct pacman edge");
			if (!pacmanDies) {
				difficulty = 40;
				pacmanDies = checkPacmanEdgeJunction(pacmanEdge.endpoints[0].index);
			}
			if (!pacmanDies) {
				pacmanDies = checkPacmanEdgeJunction(pacmanEdge.endpoints[1].index);
			}
			if (!pacmanDies && pacmanEdge.endpoints[0].nrNeighbours + pacmanEdge.endpoints[1].nrNeighbours <= 6) {
				// pacman will die if ghosts are closer to the 4 junctions that are 2 junctions away from pacman
				difficulty = 60;
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
		return pacmanDies ? PACMAN_WILL_DIE + 2000 - difficulty - currDepth : 0;
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
		if (score != PACMAN_DIES_VALUE && b.nrPillsOnBoard == 0 && b.nrPowerPillsOnBoard == 0) {
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

	private static void updateKillerMoves(boolean movePacman) {
		PlyInfo p = plyInfo[currDepth];
		if (p.nrPossibleMoves <= 1) {
			return;
		}
		if (movePacman) {
			int[] moveIndices = pacmanKillerMoves[b.pacmanLocation];
			int j;
			for (j = 0; moveIndices[j] != p.bestPacmanMove; ++j) {
			}
			for (; j > 0; --j) {
				moveIndices[j] = moveIndices[j-1];
			}
			moveIndices[0] = p.bestPacmanMove;
		} else {
			for (int g = 0; g < b.ghosts.length; ++g) {
				MyGhost ghost = b.ghosts[g];
				if (p.bestGhostMove[g] >= 0) {
					int[] moveIndices = ghostKillerMoves[ghost.currentNodeIndex][ghost.lastMoveMade.ordinal()];
					int j;
					for (j = 0; moveIndices[j] != p.bestGhostMove[g]; ++j) {
					}
					for (; j > 0; --j) {
						moveIndices[j] = moveIndices[j-1];
					}
					moveIndices[0] = p.bestGhostMove[g];
				}
			}
		}
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
	
	public static StaticEvaluator2 staticEval2 = new StaticEvaluator2();
	
	public static void calcBorderEdges() {
		if(log)log("calcBorderEdges");
		staticEval2.expand();
	}
	private static class BorderEdge {
		BigEdge edge;
		Node pacmanJunction;
		/** the junction that is closer to the ghosts */
		Node ghostJunction;
		/** First move from ghostJunction towards pacman */
		MOVE firstMoveFromGhost;
		/** pacman distance to ghost junction */
		int pacmanDist;
		int[] ghostDist = new int[4];
		/** bit mask of all ghosts that are closer than pacman to ghost junction */
		int closerGhosts;
		
		public String toString() {
			return "[" + pacmanJunction + "-" + ghostJunction + "], pacmanDist: " + pacmanDist + ", closerGhosts: " + closerGhosts;
		}
	}
	
	private static class StaticEvaluator2 {
		private static final boolean statLog = log && false;
		List<BorderEdge> borders = new ArrayList<BorderEdge>();
		Node[] pacmanNodes = new Node[100];
		int nrPacmanNodes;
		int[] pacmanDistances;
		BorderEdge[] borderEdgeCache;
		List<BorderEdge> added = new ArrayList<BorderEdge>();
		List<BorderEdge> newAdded = new ArrayList<BorderEdge>();
		public boolean hasCircles;
		
		public void update() {
			pacmanDistances = new int[graph.junctionNodes.length];
			// initialize border edge cache
			borderEdgeCache = new BorderEdge[2*graph.edges.size()];
			for (int i = 0; i < graph.edges.size(); ++i) {
				BigEdge edge = graph.edges.get(i);
				for (int endpoint = 0; endpoint < 2; ++endpoint) {
					BorderEdge border = new BorderEdge();
					border.edge = edge;
					border.pacmanJunction = edge.endpoints[endpoint];
					border.ghostJunction = edge.endpoints[1-endpoint];
					MOVE firstMoveFromOther = edge.getFirstMove(border.ghostJunction);
					border.firstMoveFromGhost = firstMoveFromOther;
					borderEdgeCache[2*i+endpoint] = border;
				}
			}
		}
		
		public void expand() {
			Arrays.fill(pacmanDistances, 10000);
			borders.clear();
			nrPacmanNodes = 0;
			added.clear();
			hasCircles = false;
			Node n = nodes[b.pacmanLocation];
			if (n.isJunction()) {
				addPacman(n, 0);
				for (int i = 0; i < n.nrNeighbours; ++i) {
					BorderEdge borderEdge = createEdge(n, n.edges[i], n.edges[i].length);
					if (borderEdge != null) {
						added.add(borderEdge);
					}
				}
			} else {
				for (int j = 0; j < 2; ++j) {
					BorderEdge borderEdge = createEdge(n.edge.endpoints[1-j], n.edge, n.edge.getDistanceToJunction(n, n.edge.endpoints[j]));
					if (borderEdge != null) {
						added.add(borderEdge);
					}
				}
			}
			while (added.size() > 0 && nrPacmanNodes < 10 && borders.size() < 9) {
				expandOneMore(true);
			}
			if(log)logState();
		}
		
		private void expandOneMore(boolean goDeeper) {
			newAdded.clear();
			for (BorderEdge borderEdge : added) {
				Node n = borderEdge.ghostJunction;
				if (borderEdge.closerGhosts == 0) {
					// this is not border edge;both junctions belong to pacman
					// check if the other junction was already a pacman node
					Node existingNode = findPacmanNode(borderEdge.ghostJunction);
					boolean expansionNeeded = false;
					if (existingNode == null) {
						// new node
						addPacman(n, borderEdge.pacmanDist);
						expansionNeeded = true;
					} else {
						hasCircles = true;
						if (pacmanDistances[existingNode.junctionIndex] > borderEdge.pacmanDist) {
							// node was already present, but now we found a shorter way to it
							pacmanDistances[existingNode.junctionIndex] = borderEdge.pacmanDist;
							expansionNeeded = true; // need to recalculate distances
						}
					}
					if (goDeeper && expansionNeeded) {
						for (int i = 0; i < n.nrNeighbours; ++i) {
							BigEdge edge = n.edges[i];
							if (edge != borderEdge.edge) {
								int distToJunction = borderEdge.pacmanDist+n.edges[i].length;
								BorderEdge existingEdge = find(edge);
								if (existingEdge == null) {
									BorderEdge newEdge = createEdge(n, n.edges[i], distToJunction);
									if (newEdge != null) {
										newAdded.add(newEdge);
									}
								} else if (distToJunction < existingEdge.pacmanDist) {
									existingEdge.pacmanDist = distToJunction;
									// recalculate which ghosts are closer
									existingEdge.closerGhosts = 0;
									for (int j = 0; j < existingEdge.ghostDist.length; ++j) {
										if (existingEdge.ghostDist[j] < distToJunction) {
											existingEdge.closerGhosts |= 1 << j;
										}
									}
									// if new result is that no ghosts are closer, then this is not a border edge anymore
									if (existingEdge.closerGhosts == 0) {
										borders.remove(existingEdge);
									}
									// recalculate sub-graph 
									newAdded.add(existingEdge);
								}
							}
						}
					}
				} else {
					borders.add(borderEdge);
					if(statLog)log("add border: " + borderEdge);
				}
			}
			// swap newAdded/added
			List<BorderEdge> help = added;
			added = newAdded;
			newAdded = help;
		}
		
		public int getNrInvolvedGhosts() {
			int ghosts = 0;
			for (BorderEdge borderEdge : borders) {
				ghosts |= borderEdge.closerGhosts;
			}
			int nrGhosts = 0;
			for (int i = 0; i < 4; ++i) {
				if ((ghosts & (1<<i)) != 0) {
					++nrGhosts;
				}
			}
			return nrGhosts;
		}
		
		public boolean match() {
			if (borders.size() > 4) {
				return false;
			}
			int nrGhosts = getNrInvolvedGhosts();
			if (nrGhosts < borders.size()) {
				return false;
			}
			if(log)log("graph: match!");
			return true;
		}
		
		private BorderEdge createEdge(Node pacmanJunction, BigEdge edge, int dist) {
			if (statLog)log("createEdge, pacmanJunc " + pacmanJunction + ", edge " + edge + ", dist" + dist);
			int endpoint = edge.endpoints[0] == pacmanJunction ? 0 : 1;
			BorderEdge borderEdge = borderEdgeCache[2*edge.id + endpoint];
			Node otherJunction = borderEdge.ghostJunction;
			int pacmanDist = dist;
			if (pacmanDistances[otherJunction.junctionIndex] <= pacmanDist) {
				// already been here with same distance or less
				return null;
			}
			borderEdge.pacmanDist = pacmanDist;
			borderEdge.closerGhosts = 0;
			if(statLog)log(otherJunction + ", pacmanDist: " + pacmanDist);
			for (int g = 0; g < b.ghosts.length; ++g) {
				MyGhost ghost = b.ghosts[g];
				if (ghost.lairTime == 0) {
					Node ghostNode = nodes[ghost.currentNodeIndex];
					if (ghostNode.edge == edge && ghostNode.getNextJunction(ghost.lastMoveMade) != otherJunction.index) {
						// ghost is already on the edge, on the move to pacman. Pacman cannot escape via this junction
						// unless ghost is on same edge as pacman and already past pacman.
						Node pacmanNode = nodes[b.pacmanLocation];
						if (pacmanNode.edge != ghostNode.edge || ghostNode.isOnPath(pacmanNode, ghost.lastMoveMade)) {
							borderEdge.closerGhosts |= 1 << g;
							borderEdge.ghostDist[g] = 0;
							if (statLog)log(otherJunction +": ghost on same edge, " + ghostNode);
						}
					} else {
						int d = graph.getGhostDistToJunction(ghost.currentNodeIndex, ghost.lastMoveMade, 
								otherJunction.index, borderEdge.firstMoveFromGhost);
						if (log) {
							int shortestDist = game.getShortestPathDistance(ghost.currentNodeIndex, otherJunction.index);
							if (d < shortestDist) {
								throw new RuntimeException("Internal error in ghostDist, ghostDist = " + d + ", shortest dist = " + shortestDist);
							}
						}
						int ghostDist = ghostDist(ghost, d);
						borderEdge.ghostDist[g] = ghostDist;
						if (ghostDist - EAT_DISTANCE <= pacmanDist) {
							borderEdge.closerGhosts |= 1 << g;
							if (statLog)log(otherJunction + ": closer ghost: " + ghostNode + ", dist: " + ghostDist);
						} else {
							if(statLog)log(otherJunction + ": longer away: ghost " + ghostNode + ", dist: " + ghostDist);
						}
					}
				} else {
					int ghostDist = ghost.lairTime + graph.getGhostDistToJunction(Search.game.getGhostInitialNodeIndex(), MOVE.NEUTRAL, 
							otherJunction.index, borderEdge.firstMoveFromGhost);
					borderEdge.ghostDist[g] = ghostDist;
					if (ghostDist - EAT_DISTANCE <= pacmanDist) {
						borderEdge.closerGhosts |= 1 << g;
						if (statLog)log(otherJunction + ": closer ghost: lair ghost " + g + ", dist: " + ghostDist);
					} else {
						if(statLog)log(otherJunction + ": longer away: ghost lair ghost " + g + ", dist: " + ghostDist);
					}
				}
			}
			if(statLog)log("created edge " + borderEdge);
			return borderEdge;
		}
		
		private void addPacman(Node node, int dist) {
			if(statLog)log("addPacmanNode " + node + ", dist " + dist);
			pacmanNodes[nrPacmanNodes] = node;
			++nrPacmanNodes;
			pacmanDistances[node.junctionIndex] = dist;
		}
		
		private Node findPacmanNode(Node node) {
			for (int i = 0; i < nrPacmanNodes; ++i) {
				if (pacmanNodes[i] == node) {
					return pacmanNodes[i];
				}
			}
			return null;
		}
		
		private BorderEdge find(BigEdge edge) {
			for (BorderEdge borderEdge : borders) {
				if (borderEdge.edge == edge) {
					return borderEdge;
				}
			}
			return null;
		}
		
		private void logState() {
			log("internal graph:");
			StringBuilder buf = new StringBuilder();
			for (int i = 0; i < nrPacmanNodes; ++i) {
				buf.append(pacmanNodes[i] + ", dist: " + pacmanDistances[pacmanNodes[i].junctionIndex] + " ");
			}
			log("pacmanNodes, size = " + nrPacmanNodes + ", nodes: " + buf);
			log("borders, size = " + borders.size());
			for (BorderEdge borderEdge : borders) {
				log(""+borderEdge);
			}
		}
		
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
