package test;

import pacman.game.*;
import pacman.game.Constants.MOVE;
import pacman.entries.pacman.*;
import pacman.entries.pacman.graph.Log;

public class TestPacmanSearch {
	private static String state = "0,1684,6060,1684,0,212,RIGHT,3,false,357,0,0,LEFT,522,0,0,LEFT,220,0,0,LEFT,375,0,0,LEFT,1111111111111100000000110011100011111111111000000000000000110000110000110000111100000000000010100000000000000010100000000000000000000000110000000000000000000000100000100000100000000000000000000000000000000000000000000000,1000";

	public static void main(String[] args) {
		MyPacMan pacman = new MyPacMan();
		Game game = new Game();
		game.setGameState(state);
		MOVE move = pacman.getMove(game, System.currentTimeMillis() + 300);
		System.out.println("Pacman returned " + move);
		Log.flush();
	}
}
