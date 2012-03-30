package pacman.entries.ghosts.graph;

/**
 * Hook to add ugly evaluation code.
 * @author louis
 *
 */
public interface EvaluationExtra {

	/**
	 * 
	 * @param p
	 * @return
	 */
	public void evaluateExtra(PlyInfo p);
}
