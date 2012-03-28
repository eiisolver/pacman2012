package pacman.entries.ghosts.graph;

import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

/**
 * Copy of the Ghost class, but now made visible.
 * @author louis
 *
 */
public class MyGhost {
	public int currentNodeIndex, edibleTime, lairTime;	
	public MOVE lastMoveMade;
	public GHOST ghost;
	
	public void copyFrom(MyGhost src) {
		currentNodeIndex = src.currentNodeIndex;
		edibleTime = src.edibleTime;
		lairTime = src.lairTime;
		lastMoveMade = src.lastMoveMade;
	}
}
