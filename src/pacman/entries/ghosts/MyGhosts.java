package pacman.entries.ghosts;

import java.io.File;
import java.util.EnumMap;
import java.util.Random;

import pacman.controllers.Controller;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import pacman.entries.ghosts.graph.*;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getActions() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., game.entries.ghosts.mypackage).
 */
public class MyGhosts extends Controller<EnumMap<GHOST,MOVE>>
{
	private EnumMap<GHOST, MOVE> myMoves=new EnumMap<GHOST, MOVE>(GHOST.class);
	JunctionGraph jgraph = new JunctionGraph();
	Board board = new Board();
	int lastMazeIndex = -1;
	public static final boolean log = Search.log;
	
	static {
		if (log) {
			Log.logFile = new File("ghosts.log");
		}
	}
	
	public EnumMap<GHOST, MOVE> getMove(Game game, long timeDue)
	{
		myMoves.clear();
		// update junction graph when necessary
		if (game.getMazeIndex() != lastMazeIndex) {
			System.out.println("Update junction graph");
			jgraph = new JunctionGraph();
			jgraph.createFromMaze(game);
			board = new Board();
			board.graph = jgraph;
			Search.update(board, jgraph, game);
		}
		lastMazeIndex = game.getMazeIndex();
		//Place your game logic here to play the game as the ghosts
		// check if any searching is required
		boolean searchRequired = false;
		for(GHOST ghostType : GHOST.values()) {
			if(game.doesGhostRequireAction(ghostType)) {
				searchRequired = true;
			}
		}
		if (searchRequired) {
			PlyInfo p = Search.plyInfo[0];
			long startTime = System.currentTimeMillis();
			board.update(game);
			if (log) {
				Log.println("Move: " + game.getCurrentLevelTime());
				jgraph.print(game, board);
			}
			Search.searchMove(timeDue);
			Log.println( "Searched " + Search.nodesSearched + " nodes, value: " + p.bestValue);
			System.out.println("Move: " + game.getCurrentLevelTime() 
					+ ", Ghosts searched " + Search.nodesSearched + " nodes, budget: " + p.budget + ", value: " + p.bestValue);
			int[] bestMove = Search.plyInfo[0].bestGhostMove;
			for (int i = 0; i < bestMove.length; ++i) {
				MyGhost ghost = board.ghosts[i];
				if (game.doesGhostRequireAction(ghost.ghost)) {
					Node n = jgraph.nodes[ghost.currentNodeIndex];
					MOVE move = n.neighbourMoves[bestMove[i]];
					myMoves.put(ghost.ghost, move);
				}
			}
			long endTime = System.currentTimeMillis();
			System.out.println("Time: " + (endTime - startTime) + " ms");
			Log.println("Time: " + (endTime - startTime) + " ms");
			Log.flush();
		}
		/*int pacmanIndex = game.getPacmanCurrentNodeIndex();
		Node pacmanNode = jgraph.nodes[pacmanIndex];
		for(GHOST ghostType : GHOST.values()) {
			if(game.doesGhostRequireAction(ghostType)) {
				myMoves.put(ghostType,allMoves[rnd.nextInt(allMoves.length)]);
			}
			if (game.getGhostLairTime(ghostType) == 0) {
				int ghostIndex = game.getGhostCurrentNodeIndex(ghostType);
				Node ghostNode = jgraph.nodes[ghostIndex];
				if (!ghostNode.isJunction() 
						&& ghostNode.edge == pacmanNode.edge) {
					MOVE lastMove = game.getGhostLastMoveMade(ghostType);
					if (ghostNode.isOnPath(pacmanNode, lastMove)) {
						System.out.println("" + ghostType + " ATTACKS PACMAN!");
					} else {
						System.out.println("Pacman and " + ghostType + " are on the same edge, lairtime=" + game.getGhostLairTime(ghostType));
					}
				}
			}
		}*/
		return myMoves;
	}
}