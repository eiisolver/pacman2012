package pacman.entries.pacman;

import java.io.File;

import pacman.controllers.Controller;
import pacman.entries.pacman.graph.*;
import pacman.game.Constants.DM;
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
	public static final boolean log = Search.log;
	private int nodeToClosestPill;
	
	static {
		if (log) {
			Log.logFile = new File("pacman.log");
		}
		Search.pacmanEvaluation = true;
	}
	
	public MOVE getMove(Game game, long timeDue) 
	{
		//Place your game logic here to play the game as the ghosts
		// update junction graph when necessary
		if (game.getMazeIndex() != lastMazeIndex) {
			Search.pacmanMovesFirst = true;
			Search.evaluationExtra = new EvaluationExtra() {
				
				@Override
				public void evaluateExtra(PlyInfo p) {
					if (Search.plyInfo[2].savedBoard.pacmanLocation == nodeToClosestPill) {
						p.bestValue += 15;
					}
				}
			};
			System.out.println("Update junction graph");
			Log.println("Update graph");
			jgraph = new JunctionGraph();
			jgraph.createFromMaze(game);
			board = new Board();
			board.graph = jgraph;
			Search.update(board, jgraph, game);
		}
		lastMazeIndex = game.getMazeIndex();
		long startTime = System.currentTimeMillis();
		board.update(game);
		if (log) {
			Log.println("Move: " + game.getCurrentLevelTime());
			jgraph.print(game, board);
		}
		nodeToClosestPill = game.getNeighbour(game.getPacmanCurrentNodeIndex(), getNearestPillMove(game, timeDue));
		System.out.println("closest pill calc: " + (System.currentTimeMillis() - startTime));
		Search.searchMove(timeDue);
		PlyInfo p = Search.plyInfo[0];
		Log.println( "Searched " + Search.nodesSearched 
				+ " nodes, budget: " + p.budget + ", max depth: " + Search.deepestSearchedPly()
				+ ", value: " + p.bestValue);
		System.out.println("Move: " + game.getCurrentLevelTime() 
				+ ", Pacman searched " + Search.nodesSearched 
				+ " nodes, budget: " + p.budget + ", max depth: " + Search.deepestSearchedPly()
				+ ", value: " + p.bestValue);
		if (p.bestValue < -20000) {
			Log.println("I will loose");
		} else if (p.bestValue > 20000) {
			Log.println("I won");
		}
		int bestMove = p.bestPacmanMove;
		Node n = jgraph.nodes[board.pacmanLocation];
		myMove = n.neighbourMoves[bestMove];
		long endTime = System.currentTimeMillis();
		System.out.println("Time: " + (endTime - startTime) + " ms");
		
		return myMove;
	}
	
	/* (non-Javadoc)
	 * @see pacman.controllers.Controller#getMove(pacman.game.Game, long)
	 */
	public MOVE getNearestPillMove(Game game,long timeDue)
	{	
		int currentNodeIndex=game.getPacmanCurrentNodeIndex();
		
		//get all active pills
		int[] activePills=game.getActivePillsIndices();
		
		//get all active power pills
		int[] activePowerPills=game.getActivePowerPillsIndices();
		
		//create a target array that includes all ACTIVE pills and power pills
		int[] targetNodeIndices=new int[activePills.length+activePowerPills.length];
		
		for(int i=0;i<activePills.length;i++)
			targetNodeIndices[i]=activePills[i];
		
		for(int i=0;i<activePowerPills.length;i++)
			targetNodeIndices[activePills.length+i]=activePowerPills[i];		
		
		//return the next direction once the closest target has been identified
		return game.getNextMoveTowardsTarget(game.getPacmanCurrentNodeIndex(),game.getClosestNodeIndexFromNodeIndex(currentNodeIndex,targetNodeIndices,DM.PATH),DM.PATH);	
	}
	

}