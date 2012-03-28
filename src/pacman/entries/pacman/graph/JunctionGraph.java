package pacman.entries.pacman.graph;

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
	 * Contains the "pacman" distance between any two junction nodes.
	 */
	public int[][] pacmanDist;
	
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
		calcPacmanDist();
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
		while (!game.isJunction(nextIndex)) {
			nodes[nextIndex].edge = edge;
			nodeList.add(nodes[nextIndex]);
			nodes[nextIndex].lastMoveIfForward = lastMoveMade;
			MOVE[] possibleMoves = game.getPossibleMoves(nextIndex, lastMoveMade);
			lastIndex = nextIndex;
			lastMoveMade = possibleMoves[0];
			nextIndex = game.getNeighbour(nextIndex, lastMoveMade);
			
		}
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
	
	private void calcPacmanDist() {
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
					if (i != j) {
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
				s = "G";
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
		for (int i = 0; i <= maxX; ++i) {
			String row = String.format("%3d ", i);
			Log.print(row);
			for (int j = 0; j <= maxY; ++j) {
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
