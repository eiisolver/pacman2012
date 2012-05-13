package test;

import java.util.*;

import pacman.entries.ghosts.*;
import pacman.entries.ghosts.graph.Log;
import pacman.game.Game;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

public class AnalyzeGhosts {
	private static final int gameNr = DownloadAndView.gameNr;
	private static int fromMoveNr = 0;
	private static int toMoveNr = 100000;
	private static int thinkTime = 40;

	public static void main(String[] args) throws Exception {
		MyGhosts ghosts = new MyGhosts();
		Game game = new Game();
		List<String> states = DownloadAndView.load(gameNr);
		for (int i = fromMoveNr; i < Math.min(toMoveNr, states.size()); ++i) {
			String state = states.get(i);
			game.setGameState(state);
			// check if any searching is required
			boolean searchRequired = false;
			for(GHOST ghostType : GHOST.values()) {
				if(game.doesGhostRequireAction(ghostType)) {
					searchRequired = true;
				}
			}
			if (searchRequired) {
				EnumMap<GHOST, MOVE> move = ghosts.getMove(game, System.currentTimeMillis() + thinkTime);
				//System.out.println("Ghosts move " + move);
				for(GHOST ghostType : GHOST.values()) {
					if(game.doesGhostRequireAction(ghostType)) {
						Log.println("Ghost move " + move.get(ghostType));
					}
				}
				Log.flush();
			}
		}
	}

}
