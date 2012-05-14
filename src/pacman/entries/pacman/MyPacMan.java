package pacman.entries.pacman;

import java.awt.Color;
import java.io.File;
import java.util.*;
import pacman.controllers.Controller;
import pacman.entries.pacman.graph.*;
import pacman.game.Constants.DM;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.game.GameView;

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
	
	public MyPacMan() {
		board.initHash();
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
			board.initHash();
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
		//System.out.println("closest pill calc: " + (System.currentTimeMillis() - startTime));
		Search.searchIterationFinished = new Runnable() {

			@Override
			public void run() {
				Node n = jgraph.nodes[board.pacmanLocation];
				lastMove = n.neighbourMoves[Search.plyInfo[0].bestPacmanMove];
				//System.out.println("Set lastMove to " + lastMove);
			}
		};
		Search.searchMove(game, timeDue);
		PlyInfo p = Search.plyInfo[0];
		int bestMove = p.bestPacmanMove;
		Node n = jgraph.nodes[board.pacmanLocation];
		myMove = n.neighbourMoves[bestMove];
		Log.println( "Searched " + Search.nodesSearched 
				+ " nodes, budget: " + p.budget + ", max depth: " + Search.deepestSearchedPly()
				+ ", value: " + p.bestValue);
		System.out.println("Move: " + game.getCurrentLevelTime() 
				+ " L" + game.getCurrentLevel() + ", Pacman searched " + Search.nodesSearched 
				+ " nodes, budget: " + p.budget + ", max depth: " + Search.deepestSearchedPly()
				+ ", value: " + p.bestValue + ", move: " + myMove);
		if (p.bestValue < -20000) {
			Log.println("I will loose");
			System.out.println("I will loose");
		} else if (p.bestValue > 20000) {
			Log.println("I won");
		} else if (p.nrSurvivingMoves == 1) {
			Log.println("Only move");
			System.out.println("Only move");
		}
		long endTime = System.currentTimeMillis();
		System.out.println("Time: " + (endTime - startTime) + " ms");
		/*if (true) {
			List<Integer> visitedList = new ArrayList<Integer>();
			for (int i = 0; i < jgraph.nodes.length; ++i) {
				if (Search.pacmanVisited[i]) {
					visitedList.add(i);
				}
			}
			int[] nodeList = new int[visitedList.size()];
			for (int i = 0; i < nodeList.length; ++i) {
				nodeList[i] = visitedList.get(i);
			}
			GameView.addPoints(game,Color.GREEN, nodeList);
		}*/
		/*if (myMove != lastMove) {
			System.err.println("lastMove != myMove, myMove = " + myMove + ", lastMove = " + lastMove);
			System.out.println("lastMove != myMove, myMove = " + myMove + ", lastMove = " + lastMove);
		}*/
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