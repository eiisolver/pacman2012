package pacman.entries.ghosts;

import java.util.*;

import pacman.game.Game;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
public class OpeningBook {
	private static HashMap<String, List<Move>> positions = new HashMap<String, List<Move>>();
	
	public static void init() {
		// typical 58th move against starter pacman
		put("0,58,140,58,0,794,UP,3,false,480,0,0,LEFT,1292,0,2,NEUTRAL,1292,0,22,NEUTRAL,1292,0,42,NEUTRAL,1111111111,1111", 44,36, MOVE.LEFT);
		put("0,82,200,82,0,569,UP,3,false,456,0,0,LEFT,554,0,0,DOWN,496,0,0,LEFT,1292,0,18,NEUTRAL,1111111,1111", 44, 12, MOVE.DOWN);
		// typical right move against starter pacman
		put("0,173,310,173,0,801,UP,2,false,516,0,0,RIGHT,1292,0,2,NEUTRAL,1292,0,22,NEUTRAL,1292,0,42,NEUTRAL,1111111,1111", 44,72, MOVE.RIGHT);
		//put("0,163,310,163,0,801,UP,2,false,516,0,0,RIGHT,1292,0,2,NEUTRAL,1292,0,22,NEUTRAL,1292,0,42,NEUTRAL,1111111,1111", 44,72, MOVE.RIGHT);
		put("0,195,360,195,0,642,DOWN,2,false,538,0,0,RIGHT,518,0,0,RIGHT,498,0,0,NEUTRAL,1292,0,20,NEUTRAL,11111111,1111", 44,54, MOVE.LEFT);
		put("0,185,360,185,0,642,DOWN,2,false,538,0,0,RIGHT,547,0,0,DOWN,498,0,0,NEUTRAL,1292,0,20,NEUTRAL,111111111,1111", 44,54, MOVE.LEFT);
	}
	
	public static boolean findPosition(Game game, EnumMap<GHOST, MOVE> myMove) {
		if (game.getCurrentLevel() != 0 || game.getCurrentLevelTime() > 1000) {
			return false;
		}
		String gamePosition = game.getGameState();
		String pos = normalize(gamePosition);
		List<Move> list = positions.get(pos);
		if (list != null) {
			for (Move move : list) {
				for(GHOST ghost : GHOST.values()) {
					int index = game.getGhostCurrentNodeIndex(ghost);
					if (index >= 0 && game.getNodeXCood(index) == move.x && game.getNodeYCood(index) == move.y) {
						myMove.put(ghost, move.move);
						System.out.println("In opening book: (" + move.x + ", " + move.y + ") " + move.move);
					}
				}
			}
		}
		return list != null;
	}
	
	private static void put(String position, int y, int x, MOVE move) {
		Move m = new Move();
		m.x = x;
		m.y = y;
		m.move = move;
		List<Move> list = new ArrayList<Move>();
		list.add(m);
		positions.put(normalize(position), list);
	}
	
	private static String normalize(String position) {
		int index1 = findComma(position, 4);
		int index2 = findComma(position, 25);
		int index3 = position.lastIndexOf(',');
		String sub = position.substring(index1, index2) + position.substring(index3);
		return sub;
	}
	
	private static int findComma(String position, int commaNr) {
		int index = position.indexOf(',');
		for (int i = 1; i < commaNr; ++i) {
			index = position.indexOf(',', index+1);
		}
		return index;
	}
	
	private static class Move {
		int x;
		int y;
		MOVE move;
		
	}
}
