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
		Log.println("Ghost positions:");
		for (int i = 0; i < Search.b.ghosts.length; ++i) {
			MyGhost g = Search.b.ghosts[i];
			Log.println(i + ": " + (g.lairTime > 0 ? "L" : Search.nodes[g.currentNodeIndex]) + " " + g.lastMoveMade);
		}
		int result = Search.checkPacmanHealth();
		Log.println("checkPacmanHealth returns " + result);
	}
	
	public void runTests() throws Exception {
		Log.logFile = new File("static_eval.log");
		runFile("static1.pos", true);
		runFile("static2.pos", false);
		runFile("static3.pos", false);
		Log.flush();
	}
	public static void main(String[] args) throws Exception {
		new TestStaticEval().runTests();
	}
}
