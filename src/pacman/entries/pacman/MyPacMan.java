package pacman.entries.pacman;

import pacman.controllers.Controller;
import pacman.entries.ghosts.graph.*;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getAction() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., game.entries.pacman.mypackage).
 */
public class MyPacMan extends Controller<MOVE>
{
	JunctionGraph jgraph = new JunctionGraph();
	Board board = new Board();
	int lastMazeIndex = -1;
	private MOVE myMove=MOVE.NEUTRAL;
	
	public MOVE getMove(Game game, long timeDue) 
	{
		//Place your game logic here to play the game as the ghosts
		// update junction graph when necessary
		if (game.getMazeIndex() != lastMazeIndex) {
			Search.pacmanMovesFirst = true;
			System.out.println("Update junction graph");
			jgraph = new JunctionGraph();
			jgraph.createFromMaze(game);
			board = new Board();
			board.graph = jgraph;
			Search.update(board, jgraph, game);
		}
		lastMazeIndex = game.getMazeIndex();
		long startTime = System.currentTimeMillis();
		board.update(game);
		Search.searchMove();
		PlyInfo p = Search.plyInfo[0];
		System.out.println("Searched " + Search.nodesSearched + " nodes, value: " + p.bestValue);
		int bestMove = p.bestPacmanMove;
		Node n = jgraph.nodes[board.pacmanLocation];
		myMove = n.neighbourMoves[bestMove];
		long endTime = System.currentTimeMillis();
		System.out.println("Time: " + (endTime - startTime) + " ms");
		
		return myMove;
	}
}