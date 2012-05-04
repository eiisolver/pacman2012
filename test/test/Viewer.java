package test;

import static pacman.game.Constants.DELAY;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import pacman.game.Game;
import pacman.game.GameView;

public class Viewer {
	private boolean paused = false;
	private int currMove;
	ArrayList<String> timeSteps;
	private long delay = 40;
	GameView gv=null;

	private void setMoveNr(int moveNr) {
		currMove = moveNr;
		if (currMove < 0) {
			currMove = 0;
		} else if (currMove >= timeSteps.size()) {
			currMove = timeSteps.size() - 1;
		}
		gv.repaint();
	}
	
	private char getNrLives(int moveNr) {
		String state = timeSteps.get(moveNr);
		int index = state.indexOf(",false");
		if (index < 0) {
			index = state.indexOf(",true");
		}
		return state.charAt(index-1);
	}
	
	private char getLevel(int moveNr) {
		return timeSteps.get(moveNr).charAt(0);
	}
    /**
	 * Replay a previously saved game.
	 *
	 * @param fileName The file name of the game to be played
	 * @param visual Indicates whether or not to use visuals
	 */
	public void replayGame(String fileName,boolean visual)
	{
		timeSteps=loadReplay(fileName);
		
		Game game=new Game(0);
		
		
		if(visual)
			gv=new GameView(game).showGame();
		gv.getFrame().addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(KeyEvent event) {
				char c = event.getKeyChar();
				if (c == ' ') {
					paused = !paused;
				} else if (c == '<') {
					setMoveNr(currMove - 100);
				} else if (c == '>') {
					setMoveNr(currMove + 100);
				} else if (c == 'l') {
					char currLevel = getLevel(currMove);
					for (; currMove <timeSteps.size()-1 && getLevel(currMove) == currLevel; ++currMove) {
					}
				} else if (c == 'v') {
					if (currMove <timeSteps.size()-1) {
						++currMove;
					}
					char currNrLives = getNrLives(currMove);
					for (; currMove <timeSteps.size()-1 && getNrLives(currMove) == currNrLives; ++currMove) {
					}
					--currMove;
				} else if (c >= '0' && c <= '9') {
					delay = 95 - (c-'0')*10;
				} else {
			    	switch(event.getKeyCode()) {
				    	case KeyEvent.VK_UP: 	
				    		setMoveNr(currMove + 20);
				    		break;
				    	case KeyEvent.VK_RIGHT: ;
				    	setMoveNr(currMove + 1);
				    	break;
				    	case KeyEvent.VK_DOWN: 	
				    		setMoveNr(currMove-20);
				    		break;
				    	case KeyEvent.VK_LEFT: 	
				    		setMoveNr(currMove-1);
				    		break;
				    	case KeyEvent.VK_HOME:
				    		setMoveNr(0);
				    		break;
				    	case KeyEvent.VK_END:
				    		setMoveNr(timeSteps.size()-1);
				    		break;
				    	default: 				;
			    	}
				}
			}

			@Override
			public void keyReleased(KeyEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void keyTyped(KeyEvent arg0) {
			}
			
		});
		for(currMove=0;;)
		{		
			game.setGameState(timeSteps.get(currMove));

			try
			{
				Thread.sleep(delay);
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
	        if(visual)
	        	gv.repaint();
			if (!paused && currMove<timeSteps.size()-1) {
				++currMove;
			}
		}
	}
	
    //load a replay
    private static ArrayList<String> loadReplay(String fileName)
	{
    	ArrayList<String> replay=new ArrayList<String>();
		
        try
        {         	
        	BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));	 
            String input=br.readLine();		
            
            while(input!=null)
            {
            	if(!input.equals(""))
            		replay.add(input);

            	input=br.readLine();	
            }
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
        }
        
        return replay;
	}
    
    public static void main(String[] args) {
    	//new Viewer().replayGame("games/236790.txt", true);
    	new Viewer().replayGame("replay.txt", true);
    }

}
