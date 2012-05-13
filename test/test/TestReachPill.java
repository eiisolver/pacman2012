package test;

import java.io.*;

import pacman.entries.ghosts.graph.*;
import pacman.game.Game;

/**
 * Tests static evaluator used in search.
 * @author louis
 *
 */
public class TestReachPill {
	private LoadPosition loader = new LoadPosition();
	File testDir = new File("testdata");

	public void assrt(boolean assertion) {
		if (!assertion) {
			Log.println("Assertion failed");
			Log.flush();
			System.out.println("Assertion failed");
			throw new RuntimeException("Assertion failed");
		}
	}
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
		Search.clearStaticEval();
		int result = Search.checkPacmanHealth();
		Log.println("checkPacmanHealth returns " + result);
		assrt(pacmanSurvives == Search.canReachPowerPill());
		if (pacmanSurvives) {
			assrt(result < 100);
		} else {
			assrt(result > 20000);
		}
	}
	
	public void runTests() throws Exception {
		Log.logFile = new File("static_eval.log");
		runFile("reachpowerpill1.pos", false);
		runFile("reachpowerpill2.pos", true);
		runFile("reachpowerpill3.pos", true);
		runFile("reachpowerpill4.pos", true);
		runFile("reachpowerpill5.pos", false);
		Log.flush();
	}
	public static void main(String[] args) throws Exception {
		new TestReachPill().runTests();
	}
}
