package pacman.game.internal;

import pacman.game.Constants.MOVE;

/*
 * Data structure to hold all information pertaining to the ghosts.
 */
public final class Ghost
{
	public int currentNodeIndex, edibleTime, lairTime;	
	public MOVE lastMoveMade;

	public Ghost(int currentNodeIndex, int edibleTime, int lairTime, MOVE lastMoveMade)
	{
		this.currentNodeIndex = currentNodeIndex;
		this.edibleTime = edibleTime;
		this.lairTime = lairTime;
		this.lastMoveMade = lastMoveMade;
	}

	public Ghost copy()
	{
		return new Ghost(currentNodeIndex, edibleTime, lairTime, lastMoveMade);		
	}
}