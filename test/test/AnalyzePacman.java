package test;

import java.util.*;
import pacman.entries.pacman.MyPacMan;
import pacman.entries.pacman.graph.Log;
import pacman.game.Game;
import pacman.game.Constants.MOVE;

public class AnalyzePacman {
	private static final int gameNr = DownloadAndView.gameNr;
	private static int fromMoveNr = 0;
	private static int toMoveNr = 100000;
	private static int thinkTime = 40;

	public static void main(String[] args) throws Exception {
		MyPacMan pacman = new MyPacMan();
		Game game = new Game();
		List<String> states = DownloadAndView.load(gameNr);
		for (int i = fromMoveNr; i < Math.min(toMoveNr, states.size()); ++i) {
			String state = states.get(i);
			game.setGameState(state);
			MOVE move = pacman.getMove(game, System.currentTimeMillis() + thinkTime);
			System.out.println("Pacman moves " + move);
			Log.println("Pacman moves " + move);
			Log.flush();
		}
	}

}
