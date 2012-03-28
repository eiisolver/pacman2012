package pacman.entries.pacman.graph;

/**
 * An edge between two junction nodes, containing zero or more
 * non-junction nodes.
 * 
 * @author louis
 *
 */
public class BigEdge {
	/** Endpoints of the edge (both are junctions) */
	public Node[] endpoints = new Node[2];
	// the indices of the internal nodes that are on the big edge
	public Node[] internalNodes;
	/** The distance between the end points */
	public int length;
	boolean containsPowerPill;
}
