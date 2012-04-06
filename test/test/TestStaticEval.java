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
		Search.calcBorderEdges();
	}
	
	public void runTests() throws Exception {
		Log.logFile = new File("static_eval.log");
		runFile("test.pos", true);
		runFile("test2.pos", true);
		runFile("test3.pos", true);
		runFile("test4.pos", true);
		Log.flush();
	}
	public static void main(String[] args) throws Exception {
		new TestStaticEval().runTests();
	}
}
