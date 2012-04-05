package test;

import java.io.*;
import java.util.*;
import pacman.game.*;
import pacman.game.Constants.MOVE;
import pacman.entries.ghosts.graph.*;

public class LoadPosition {
	public void loadPosition(Board b, List<String> strings) {
		int ghostNr = 0;
		b.nrPowerPills = 0;
		b.nrPills = 0;
		for (int i = 0; i < b.graph.nodes.length; ++i) {
			b.containsPill[i] = false;
			b.containsPowerPill[i] = false;
		}
		for (int i = 0; i < strings.size(); ++i) {
			String line = strings.get(i);
			for (int x = 4; x < line.length(); ++x) {
				int j = x-4;
				char c = line.charAt(x);
				if (c == 'P') {
					Node n = b.graph.find(j, i);
					b.pacmanLocation = n.index;
					b.pacmanLastMove = n.neighbourMoves[0];
				} else if (c == 'U' || c == 'D' || c == 'R' || c == 'L') {
					Node n = b.graph.find(j, i);
					MyGhost ghost = b.ghosts[ghostNr];
					ghost.currentNodeIndex = n.index;
					ghost.edibleTime = 0;
					ghost.lairTime = 0;
					if (c == 'U') {
						ghost.lastMoveMade = MOVE.UP;
					} else if (c == 'D') {
						ghost.lastMoveMade = MOVE.DOWN;
					} else if (c == 'R') {
						ghost.lastMoveMade = MOVE.RIGHT;
					} else if (c == 'L') {
						ghost.lastMoveMade = MOVE.LEFT;
					}
					++ghostNr;
				} else if (c == 'X') {
					Node n = b.graph.find(j, i);
					b.powerPillLocation[b.nrPowerPills] = n.index;
					b.containsPowerPill[n.index] = true;
				}
			}
		}
		b.nrPowerPillsLeft = b.nrPowerPills;
	}
	
	public List<String> loadFile(File file) throws Exception {
		List<String> list = new ArrayList<String>();
		BufferedReader in = new BufferedReader(new FileReader(file));
		for (String line = in.readLine(); line != null; line = in.readLine()) {
			list.add(line);
		}
		in.close();
		return list;
	}
	public void loadPosition(Board b, Game game, File file) throws Exception {
		String[] mazeStrings = new String[] {
				"0,571,1520,571,0,1009,RIGHT,3,false,868,130,0,RIGHT,520,0,0,RIGHT,860,130,0,RIGHT,1037,130,0,DOWN,1111111111111111111111111111111111000000000000000000000011011110011110011110011111111111111000000000000000000000000000000000000010011001111111110000000000000000110000110000110000000000100011000011111111000000000011000000,1110",
				"1,3678,11375,677,1,236,DOWN,4,true,186,0,0,LEFT,60,0,0,RIGHT,177,0,0,LEFT,57,0,0,RIGHT,000000000000001111111011100000000000101100101100100000011000000010111100100011100000000000001111100111111001100111100111100110011001111000000000000000100010001111110000000011100111100111111111111100001111111111111111111111111111111111111111,1011",
				"2,6983,21215,981,2,45,RIGHT,4,true,170,0,0,RIGHT,112,58,0,DOWN,375,58,0,RIGHT,376,58,0,RIGHT,0000000000000111111111000111001100000000001111111111000011110000001111110001110000000111111111000000000101010100000000000000000000000000000000000001000001000000000111000011001111001111000000111111111111110011110011110000001111111111111111,0101",
				"3,11692,33515,2686,3,510,RIGHT,1,true,602,0,0,UP,750,0,0,DOWN,516,0,0,RIGHT,762,0,0,DOWN,000000000000000000000000000000000000000000000100000000000000010000000100000001000000000000000000110000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000,0000"
		};
		List<String> lines = loadFile(file);
		int mazeNr = Integer.parseInt(lines.get(0));
		game.setGameState(mazeStrings[mazeNr]);
		b.graph = new JunctionGraph();
		b.graph.createFromMaze(game);
		b.update(game);
		lines.remove(0);
		loadPosition(b, lines);
		Search.update(b, b.graph, game);
	}

	public static void main(String[] args) {
		String state = "3,11692,33515,2686,3,510,RIGHT,1,true,602,0,0,UP,750,0,0,DOWN,516,0,0,RIGHT,762,0,0,DOWN,000000000000000000000000000000000000000000000100000000000000010000000100000001000000000000000000110000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000,0000";
		String state0 = "0,571,1520,571,0,1009,RIGHT,3,false,868,130,0,RIGHT,520,0,0,RIGHT,860,130,0,RIGHT,1037,130,0,DOWN,1111111111111111111111111111111111000000000000000000000011011110011110011110011111111111111000000000000000000000000000000000000010011001111111110000000000000000110000110000110000000000100011000011111111000000000011000000,1110";
		Game game = new Game();
		game.setGameState(state0);
		GameView gameView = new GameView(game);
		gameView.showGame();
	}
}
