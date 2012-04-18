package test;

import java.io.*;

import pacman.entries.ghosts.graph.*;
import pacman.game.Game;

/**
 * Tests static evaluator used in search.
 * @author louis
 *
 */
public class TestStaticEval {
	private LoadPosition loader = new LoadPosition();
	File testDir = new File("testdata");

	private void runFile(String fileName, boolean pacmanSurvives) throws Exception {
		Board b = new Board();
		Game game = new Game();
		loader.loadPosition(b, game, new File(testDir, fileName));
		Log.println("Test " + fileName);
		b.logBoard(game);
		//Search.checkPacmanHealth();
		long start = System.currentTimeMillis();
		for (int i = 0; i < 1; ++i) {
		Search.calcBorderEdges();
		}
		long duration = System.currentTimeMillis() - start;
		System.out.println("duration: " + duration);
	}
	
	public void runTests() throws Exception {
		Log.logFile = new File("static_eval.log");
		runFile("test.pos", true);
		runFile("test2.pos", true);
		runFile("test3.pos", true);
		runFile("test4.pos", true);
		runFile("test5.pos", true);
		runFile("test6.pos", true);
		Log.flush();
	}
	public static void main(String[] args) throws Exception {
		new TestStaticEval().runTests();
	}
}
