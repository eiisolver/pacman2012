package test;

import java.util.*;
import pacman.entries.pacman.MyPacMan;
import pacman.entries.pacman.graph.Log;
import pacman.game.Game;
import pacman.game.Constants.MOVE;

public class AnalyzePacman {
	private static final int gameNr = -1;//DownloadAndView.gameNr;
	private static int fromMoveNr = 930;
	private static int toMoveNr = 1000;
	private static int fromLevel = 1;
	private static int toLevel = 1;
	private static int thinkTime = 40;

	public static void main(String[] args) throws Exception {
		MyPacMan pacman = new MyPacMan();
		Game game = new Game();
		List<String> states = DownloadAndView.load(gameNr);
		for (int i = 0; i < states.size(); ++i) {
			String state = states.get(i);
			game.setGameState(state);
			int level = game.getCurrentLevel();
			int moveNr = game.getCurrentLevelTime();
			if (level >= fromLevel && level <= toLevel && moveNr >= fromMoveNr && moveNr <= toMoveNr) {
				MOVE move = pacman.getMove(game, System.currentTimeMillis() + thinkTime);
				System.out.println("Pacman moves " + move);
				Log.println("Pacman moves " + move);
				Log.flush();
			}
		}
	}

}
