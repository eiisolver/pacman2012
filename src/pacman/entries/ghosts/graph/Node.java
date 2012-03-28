package pacman.entries.ghosts.graph;

import pacman.game.*;
import static pacman.game.Constants.*;

/**
 * There is one InternalNode for every non-junction Node.
 * @author louis
 *
 */
public class Node {
	//----------------------------------------------------------------------------
	// APPLICABLE TO ALL NODES
	//----------------------------------------------------------------------------
	public JunctionGraph graph;
	/** Index of this node in the current maze */
	public int index;
	/** The number of neighbours that this node has */
	public int nrNeighbours;
	/** Neighbours of this node */
	public int[] neighbours;
	/** 
	 * The move that should be taken to get to a neighbour, i.e.
	 * neighbourMoves[0] takes you to neighbours[0]
	 */
	public MOVE[] neighbourMoves;
	/** The number of ghosts that are on this node */
	public int nrGhosts;
	//----------------------------------------------------------------------------
	// APPLICABLE TO INTERNAL (NON-JUNCTION) NODES
	//----------------------------------------------------------------------------
	/** BigEdge on which this node resides (only applicable for internal nodes) */
	public BigEdge edge;
	/** distToJunction[0] is pacman distance to BigEdge.endpoints[0] (only applicable for internal nodes) */
	public int[] distToJunction;
	/** Index of this node on edge.internalNodes */
	public int edgeIndex;
	MOVE lastMoveIfForward;
	
	//----------------------------------------------------------------------------
	// APPLICABLE TO INTERNAL JUNCTION NODES
	//----------------------------------------------------------------------------
	/** Junction index of this node in JunctionGraph.junctionNodes (only applicable for junction nodes)*/
	public int junctionIndex = -1;
	/** all edges connected to this node (only applicable for junction nodes) */
	public BigEdge[] edges = new BigEdge[4];
	/** The number of edges connected to this node (only applicable for junction nodes) */
	public int nrEdges;
	
	public boolean isJunction() {
		return nrNeighbours != 2;
	}
	
	/**
	 * Returns the next junction, given that we made lastMove to arrive to this node.
	 * @param lastMove
	 * @return
	 */
	public int getNextJunction(MOVE lastMove) {
		int index = 0;
		if (lastMove == lastMoveIfForward) {
			index = 1;
		} 
		return edge.endpoints[index].index;
	}
	
	/**
	 * Returns the distance to the next junction, given that we made lastMove
	 * to arrive to this node.
	 * @param lastMove
	 * @return
	 */
	public int getDistToNextJunction(MOVE lastMove) {
		int index = 0;
		if (lastMove == lastMoveIfForward) {
			index = 1;
		} 
		return distToJunction[index];
	}
	
	/**
	 * Checks if the given node is on the path from this node to
	 * the endpoint of this edge, given that we made lastMove to arrive to
	 * this node.
	 * @param nodeOnEdge
	 * @param lastMove
	 * @return
	 */
	public boolean isOnPath(Node nodeOnEdge, MOVE lastMove) {
		if (lastMove == lastMoveIfForward) {
			return nodeOnEdge.edgeIndex > edgeIndex;
		} else {
			return nodeOnEdge.edgeIndex < edgeIndex;
		}
	}
	
	/**
	 * Returns the pacman distance to the other node that must be on the
	 * same edge as this node (and must be an internal node).
	 * @param nodeOnEdge
	 * @return
	 */
	public int distOnEdge(Node nodeOnEdge) {
		return Math.abs(edgeIndex - nodeOnEdge.edgeIndex);
	}
	
}
