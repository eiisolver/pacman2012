package pacman.entries.ghosts.graph;

import java.util.*;

import pacman.game.Constants.MOVE;
import pacman.game.Game;

/**
 * Graph containing only junctions (>2 edges).
 * There may be multiple edges between 2 nodes of this graph.
 * There may be edges from a node to itself.
 * @author louis
 *
 */
public class JunctionGraph {
	/** underlying maze */
	public Game game;
	/** Dense array containing all junction nodes */
	public Node[] junctionNodes;
	/** Contains all nodes in the current maze */
	public Node[] nodes;
	public List<BigEdge> edges = new ArrayList<BigEdge>();
	/**
	 * Contains the ghost distance between any two junction nodes.
	 * ghostDist[move1][index1][move2][index2] gives the distance 
	 * between junction nodes index1 and index 2 given that
	 * move1 was the move leading to index1, move2 will be the
	 * move made from index2
	 */
	public int[][][][] ghostDist;
	private static final MOVE[] intToMove = new MOVE[] {
		MOVE.UP, MOVE.RIGHT, MOVE.DOWN, MOVE.LEFT
	};
	
	/**
	 * Returns the ghost distance from junction index1 to junction index2
	 * @param index1 index of source node(normal index, not junction index) of from node
	 * @param lastMoveMade last move made that led to index1
	 * @param index2 index of destination
	 * @param moveFromIndex2 move to be made from index2
	 * @return
	 */
	public int getGhostDistBetweenJunctions(int index1, MOVE lastMoveMade, int index2, MOVE moveFromIndex2) {
		int j1 = nodes[index1].junctionIndex;
		int j2 = nodes[index2].junctionIndex;
		return ghostDist[lastMoveMade.ordinal()][j1][moveFromIndex2.ordinal()][j2];
	}
	
	/**
	 * Returns the ghost distance from a node (junction or not) to a junction
	 * @param index1 index of source node(normal index, not junction index) of from node
	 * @param lastMoveMade last move made that led to index1
	 * @param index2 index of destination
	 * @param moveFromIndex2 move to be made from index2
	 * @return
	 */
	public int getGhostDistToJunction(int index1, MOVE lastMoveMade, int index2, MOVE moveFromIndex2) {
		int junc1;
		MOVE lastMove;
		int distToJunc1;
		Node n1 = nodes[index1];
		if (n1.isJunction()) {
			junc1 = index1;
			lastMove = lastMoveMade;
			distToJunc1 = 0;
		} else {
			junc1 = n1.getNextJunction(lastMoveMade);
			lastMove = n1.getLastMoveToNextJunction(lastMoveMade);
			distToJunc1 = n1.getDistToNextJunction(lastMoveMade);
		}
		return distToJunc1 + getGhostDistBetweenJunctions(junc1, lastMove, index2, moveFromIndex2);
	}
	
	/**
	 * Constructs the junction graph from the given maze.
	 * @param maze
	 */
	public void createFromMaze(Game game) {
		long start = System.currentTimeMillis();
		this.game = game;
		// create internal nodes
		nodes = new Node[game.getNumberOfNodes()];
		for (int i = 0; i < nodes.length; ++i) {
			Node n = new Node();
			n.index = i;
			nodes[i] = n;
			n.neighbourMoves = game.getPossibleMoves(i);
			n.neighbours = new int[n.neighbourMoves.length];
			n.nrNeighbours = n.neighbours.length;
			n.x = game.getNodeXCood(i);
			n.y = game.getNodeYCood(i);
			for (int m = 0; m < n.neighbourMoves.length; ++m) {
				n.neighbours[m] = game.getNeighbour(i, n.neighbourMoves[m]);
			}
		}
		// create junction nodes
		junctionNodes = new Node[game.getJunctionIndices().length];
		for (int i = 0; i < junctionNodes.length; ++i) {
			junctionNodes[i] = nodes[game.getJunctionIndices()[i]];
			junctionNodes[i].junctionIndex = i;
		}
		// create edges
		for (int i = 0; i < junctionNodes.length; ++i) {
			Node n = junctionNodes[i];
			for (MOVE move : game.getPossibleMoves(n.index)) {
				addEdge(n, move);
			}
		}
		calcGhostDist();
		long now = System.currentTimeMillis();
		System.out.println("Junction graph: #junctions = " + junctionNodes.length + ", #edges = " + edges.size());
		System.out.println("nr millis: " + (now - start));
	}
	
	private void addEdge(Node n, MOVE move) {
		MOVE lastMoveMade = move;
		int lastIndex = n.index;
		int nextIndex = game.getNeighbour(lastIndex, lastMoveMade);
		if (nodes[nextIndex].edge != null) {
			// edge has already been added
			return;
		}
		if (game.isJunction(nextIndex) && nextIndex < lastIndex) {
			// edge without internal nodes, but it has already been added
			return;
		}
		// this is a new edge
		BigEdge edge = new BigEdge();
		edges.add(edge);
		List<Node> nodeList = new ArrayList<Node>();
		edge.firstMoveToOtherEnd[0] = lastMoveMade;
		while (!game.isJunction(nextIndex)) {
			nodes[nextIndex].edge = edge;
			nodeList.add(nodes[nextIndex]);
			nodes[nextIndex].lastMoveIfForward = lastMoveMade;
			nodes[nextIndex].moveToPrevNode = lastMoveMade.opposite();
			MOVE[] possibleMoves = game.getPossibleMoves(nextIndex, lastMoveMade);
			lastIndex = nextIndex;
			lastMoveMade = possibleMoves[0];
			nodes[nextIndex].moveToNextNode = lastMoveMade;
			nextIndex = game.getNeighbour(nextIndex, lastMoveMade);
		}
		edge.firstMoveToOtherEnd[1] = lastMoveMade.opposite();
		Node n2 = nodes[nextIndex];
		edge.endpoints[0] = n;
		edge.endpoints[1] = n2;
		edge.internalNodes = nodeList.toArray(new Node[0]);
		edge.length = edge.internalNodes.length + 1;
		n.edges[n.nrEdges] = edge;
		++n.nrEdges;
		n2.edges[n2.nrEdges] = edge;
		++n2.nrEdges;
		for (int i = 0; i < edge.internalNodes.length; ++i) {
			Node node = edge.internalNodes[i];
			node.distToJunction = new int[2];
			node.distToJunction[0] = i+1;
			node.distToJunction[1] = edge.internalNodes.length - i;
			node.edgeIndex = i;
		}
	}
	
	/*private void calcPacmanDist() {
		int N = junctionNodes.length;
		pacmanDist = new int[N][N];
		for (int i = 0; i < N; ++i) {
			for (int j = 0; j < N; ++j) {
				pacmanDist[i][j] = 100000;
			}
		}
		for (int i = 0; i < N; ++i) {
			pacmanDist[i][i] = 0;
			for (int e = 0; e < junctionNodes[i].nrEdges; ++e) {
				BigEdge edge = junctionNodes[i].edges[e];
				if (edge.endpoints[0] != edge.endpoints[1]) {
					int j = edge.endpoints[0].junctionIndex;
					if (i == j) {
						j = edge.endpoints[1].junctionIndex;
					}
					if (edge.length < pacmanDist[i][j]) {
						pacmanDist[i][j] = edge.length;
						pacmanDist[j][i] = pacmanDist[i][j];
					}
				}
			}
		}
		for (int i = 0; i < N; ++i) {
			for (int j = 0; j < N; ++j) {
				for (int k = 0; k < N; ++k) {
					int sum = pacmanDist[i][j] + pacmanDist[j][k];
					if (sum < pacmanDist[i][k]) {
						pacmanDist[i][k] = sum;
					}
				}
			}
		}
	}*/
	
	private void calcGhostDist() {
		int N = junctionNodes.length;
		ghostDist = new int[5][N][5][N];
		for (int m = 0; m < 4; ++m) {
			for (int i = 0; i < N; ++i) {
				for (int m2 = 0; m2 < 4; ++m2) {
					Arrays.fill(ghostDist[m][i][m2], 100000);
				}
			}
		}
		// set 0-distance between junction and itself 
		for (int i = 0; i < N; ++i) {
			for (MOVE m1 : MOVE.values()) {
				for (MOVE m2 : MOVE.values()) {
					if (m1.opposite() != m2) {
						ghostDist[m1.ordinal()][i][m2.ordinal()][i] = 0;
					}
				}
			}
		}
		// calc distances between neighbour junctions
		/*for (BigEdge edge : edges) {
			Node start = edge.endpoints[0];
			Node end = edge.endpoints[1];
			int secondNodeIndex = edge.length == 1 ? end.index : edge.internalNodes[1].index;
			int secondLastNodeIndex = edge.length == 1 ? start.index : edge.internalNodes[edge.internalNodes.length-1].index;
			for (int i = 0; i < start.nrNeighbours; ++i) {
				if (start.neighbours[i] != secondNodeIndex) {
					MOVE moveToStart = start.neighbourMoves[i].opposite();
					for (int j = 0; j < end.nrNeighbours; ++j) {
						if (end.neighbours[j] != secondLastNodeIndex) {
							int m1 = moveToStart.ordinal();
							int m2 = end.neighbourMoves[j].ordinal();
							int[] arr = ghostDist[m1][start.junctionIndex][m2];
							if (arr[end.junctionIndex] > edge.length) {
								arr[end.junctionIndex] = edge.length;
								ghostDist[m2][end.junctionIndex][m1][start.junctionIndex] = edge.length;
							}
						}
					}
				}
			}
		}*/
		boolean[] visited = new boolean[junctionNodes.length];
		long startTime = System.currentTimeMillis();
		for (int maxDepth = 1; maxDepth < 20 && System.currentTimeMillis() - startTime < 200; ++maxDepth) {
			System.out.println("walk, maxDepth = " + maxDepth);
			nrVisit = 0;
			for (int i = 0; i < junctionNodes.length; ++i) {
				Arrays.fill(visited, false);
				visited[i] = true;
				Node start = junctionNodes[i];
				for (int e = 0; e < start.nrEdges; ++e) {
					BigEdge edge = start.edges[e];
					MOVE startMove = edge.getFirstMove(start);
					Node nextNode = edge.getOtherJunction(start);
					MOVE nextLastMove = edge.getFirstMove(nextNode).opposite();
					walk(visited, start, startMove, nextNode, nextLastMove, edge.length, maxDepth);
				}
			}
			System.out.println("visits = " + nrVisit + ", time: " + (System.currentTimeMillis()-startTime));
		}
	}
	
	static long nrVisit = 0;
	
	private void walk(boolean[] visited, Node start, MOVE startMove, Node end, MOVE lastMove, int currDist, int depth) {
		++nrVisit;
		// update distances
		boolean improvedDistance = false;
		for (int i = 0; i < start.nrNeighbours; ++i) {
			if (start.neighbourMoves[i] != startMove) {
				MOVE moveToStart = start.neighbourMoves[i].opposite();
				for (int j = 0; j < end.nrNeighbours; ++j) {
					if (end.neighbourMoves[j] != lastMove.opposite()) {
						int m1 = moveToStart.ordinal();
						int m2 = end.neighbourMoves[j].ordinal();
						int[] arr = ghostDist[m1][start.junctionIndex][m2];
						if (arr[end.junctionIndex] > currDist) {
							arr[end.junctionIndex] = currDist;
							ghostDist[m2][end.junctionIndex][m1][start.junctionIndex] = currDist;
							improvedDistance = true;
						}
					}
				}
			}
		}
		if (visited[end.junctionIndex] && !improvedDistance) {
			return;
		}
		visited[end.junctionIndex] = true;
		if (depth > 0) {
			for (int i = 0; i < end.nrEdges; ++i) {
				BigEdge edge = end.edges[i];
				Node nextNode = edge.getOtherJunction(end);
				MOVE nextLastMove = edge.getFirstMove(nextNode).opposite();
				walk(visited, start, startMove, nextNode, nextLastMove, currDist + edge.length, depth - 1);
			}
		}
	}
	
	public void print(Game game, Board b) {
		String[][] repr = new String[200][200];
		int maxX = 0;
		int maxY = 0;
		for (int i = 0; i < game.getNumberOfNodes(); ++i) {
			int x = game.getNodeXCood(i);
			if (x > maxX) {
				maxX = x;
			}
			int y = game.getNodeYCood(i);
			if (y > maxY) {
				maxY = y;
			}
			Node n = nodes[i];
			String s = ".";
			if (n.index == game.getPacmanCurrentNodeIndex()) {
				s = "P";
			} else if (n.nrGhosts > 0) {
				for (int g = 0; g < b.ghosts.length; ++g) {
					if (b.ghosts[g].currentNodeIndex == n.index) {
						s = "" + b.ghosts[g].lastMoveMade.toString().charAt(0);
						if (b.ghosts[g].edibleTime > 0) {
							s = s.toLowerCase();
						}
						break;
					}
				}
				//s = "G";
			} else if (b.containsPowerPill[i]) {
				s = "X";
			} else if (b.containsPill[i]) {
				s = "o";
			}
			repr[x][y] = s;
			/*if (n.isJunction()) {
				repr[x][y] = "J" + n.nrEdges;
			} else {
				repr[x][y] = ""+n.edgeIndex;
			}*/
		}
		for (int j = 0; j <= maxY; ++j) {
			String row = String.format("%3d ", j);
			Log.print(row);
			for (int i = 0; i <= maxX; ++i) {
				String s = repr[i][j];
				if (s == null) {
					s = " ";
				}
				Log.print(s);
			}
			Log.println();
		}
		
	}
}
