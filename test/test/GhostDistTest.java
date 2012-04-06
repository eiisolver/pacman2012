package test;

import java.io.*;

import pacman.entries.ghosts.graph.*;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

/**
 * Tests static evaluator used in search.
 * @author louis
 *
 */
public class GhostDistTest {
	private LoadPosition loader = new LoadPosition();
	File testDir = new File("testdata");
	Game game;
	Board b;

	private void check(int nodeY, int nodeX, MOVE lastMove, int juncY, int juncX, MOVE firstMoveFromJunc, int expectedDist) {
		Node n = b.graph.find(nodeX, nodeY);
		Node junction = b.graph.find(juncX, juncY);
		int ghostDist = b.graph.getGhostDistToJunction(n.index, lastMove, junction.index, firstMoveFromJunc);
		if (ghostDist != expectedDist) {
			System.out.println("expected dist: " + expectedDist + ", was: " + ghostDist);
			throw new RuntimeException();
		}
	}
	private void runFile(String fileName) throws Exception {
		b = new Board();
		game = new Game();
		loader.loadPosition(b, game, new File(testDir, fileName));
		Log.println("Test " + fileName);
		b.logBoard(game);
		check(20, 72, MOVE.UP, 16, 72, MOVE.UP, 4);
	}
	
	public void runTests() throws Exception {
		Log.logFile = new File("ghost_test.log");
		runFile("dist1.pos");
		Log.flush();
	}
	public static void main(String[] args) throws Exception {
		new GhostDistTest().runTests();
	}
}
