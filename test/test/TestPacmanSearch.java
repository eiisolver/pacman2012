package test;

import pacman.game.*;
import pacman.game.Constants.MOVE;
import pacman.entries.pacman.*;
import pacman.entries.pacman.graph.Log;

public class TestPacmanSearch {
	private static String state = "0,1418,5160,1418,0,1251,RIGHT,1,false,1247,0,0,RIGHT,1206,0,0,RIGHT,973,0,0,RIGHT,516,0,0,RIGHT,1111110000000000111111110111101111111111111111111000000011111100111100111100111111111111000010101010101010101010100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000,1100";

	public static void main(String[] args) {
		MyPacMan pacman = new MyPacMan();
		Game game = new Game();
		game.setGameState(state);
		MOVE move = pacman.getMove(game, System.currentTimeMillis() + 300);
		System.out.println("Pacman returned " + move);
		Log.flush();
	}
}
