/**
 * Almost all code in this class has been written by Kettling, 
 * see http://forum.pacman-vs-ghosts.net/viewtopic.php?f=6&t=16
 */
package test;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pacman.game.Game;

public class Downloader {
	private Game game;
	/**
	 * Try to protect us from errors
	 */

	private void safeSetGameState( String newstate )
	{
		try
		{
			game.setGameState( newstate );
		}
		catch( Exception e )
		{
			//JOptionPane.showMessageDialog( this, e, "Exception", JOptionPane.WARNING_MESSAGE );
			e.printStackTrace();
		}
	}

	private static String moveNtoS( String n )
	{
		if( n.equals( "0" )) { return( "UP" ); }
		if( n.equals( "1" )) { return( "RIGHT" ); }
		if( n.equals( "2" )) { return( "DOWN" ); }
		if( n.equals( "3" )) { return( "LEFT" ); }
		if( n.equals( "4" )) { return( "NEUTRAL" ); }
		return( n ); /* Probably broken but give benefit of the doubt */
	}

	/**
	 * Load from a file into an array of Strings (in standard Game format)
	 * TODO: translate from WWW format if required (autodetect)
	 */
	private ArrayList<String> loadReplayFromStream( InputStream strm )
	{
		ArrayList<String> replay=new ArrayList<String>();

		try
		{
			Pattern www_line = Pattern.compile( ".*?"		+ /* Ignore */
						"[{]ma:([0-9]+),"		+ /*  1 */
						"tt:([0-9]+),"			+ /*  2 */
						"li:([0-9]+),"			+ /*  3 */
						"sc:([0-9]+),"			+ /*  4 */
						"lt:([0-9]+),"			+ /*  5 */
						"le:([0-9]+),"			+ /*  6 */
						"pn:([0-9]+),"			+ /*  7 */
						"pd:([0-9]+),"			+ /*  8 */
						"gh:\\[[{]gn:([0-9]+),"		+ /*  9 */
						"di:([0-9]+),"			+ /* 10 */
						"et:([0-9]+),"			+ /* 11 */
						"lt:([0-9]+)[}],"		+ /* 12 */
						"[{]gn:([0-9]+),"		+ /* 13 */
						"di:([0-9]+),"			+ /* 14 */
						"et:([0-9]+),"			+ /* 15 */
						"lt:([0-9]+)[}],"		+ /* 16 */
						"[{]gn:([0-9]+),"		+ /* 17 */
						"di:([0-9]+),"			+ /* 18 */
						"et:([0-9]+),"			+ /* 19 */
						"lt:([0-9]+)[}],"		+ /* 20 */
						"[{]gn:([0-9]+),"		+ /* 21 */
						"di:([0-9]+),"			+ /* 22 */
						"et:([0-9]+),"			+ /* 23 */
						"lt:([0-9]+)[}]\\],"		+ /* 24 */
						"pi:\"([^\"]*)\","		+ /* 25 */
						"po:\"([^\"]*)\"[}],"		);/* 26 */

			BufferedReader br=new BufferedReader( new InputStreamReader( strm ));
			String input;

			while(( input = br.readLine()) != null )
			{
	/*
	 * Magic pattern matching -- if it looks like it could be a line from the WWW, then presume it is
	 * and convert to a regular line. Difference between Java data handling and Javascript.
	 */
				if( input.equals( "" )) continue;
				Matcher m = www_line.matcher( input );
				if( m.matches())
				{
					input =           m.group(  1 )  + "," + /* ma: mazeIndex                       */
						          m.group(  2 )  + "," + /* tt: totalTime                       */
						          m.group(  4 )  + "," + /* sc: score                           */
						          m.group(  5 )  + "," + /* lt: currentLevelTime                */
						          m.group(  6 )  + "," + /* le: levelCount                      */
						          m.group(  7 )  + "," + /* pn: pacman.currentNodeIndex         */
						moveNtoS( m.group(  8 )) + "," + /* pd: pacman.lastMoveMade             */
						          m.group(  3 )  + "," + /* li: pacman.numberOfLivesRemaining   */
						          "false"        + "," + /* ??: pacman.hasReceivedExtraLife     */
						          m.group(  9 )  + "," + /* gn: ghost[0].currentNodeIndex       */
						          m.group( 11 )  + "," + /* et: ghost[0].edibleTime             */
						          m.group( 12 )  + "," + /* lt: ghost[0].lairTime               */
						moveNtoS( m.group( 10 )) + "," + /* di: ghost[0].lastMoveMade           */
						          m.group( 13 )  + "," + /* gn: ghost[1].currentNodeIndex       */
						          m.group( 15 )  + "," + /* et: ghost[1].edibleTime             */
						          m.group( 16 )  + "," + /* lt: ghost[1].lairTime               */
						moveNtoS( m.group( 14 )) + "," + /* di: ghost[1].lastMoveMade           */
						          m.group( 17 )  + "," + /* gn: ghost[2].currentNodeIndex       */
						          m.group( 19 )  + "," + /* et: ghost[2].edibleTime             */
						          m.group( 20 )  + "," + /* lt: ghost[2].lairTime               */
						moveNtoS( m.group( 18 )) + "," + /* di: ghost[2].lastMoveMade           */
						          m.group( 21 )  + "," + /* gn: ghost[3].currentNodeIndex       */
						          m.group( 23 )  + "," + /* et: ghost[3].edibleTime             */
						          m.group( 24 )  + "," + /* lt: ghost[3].lairTime               */
						moveNtoS( m.group( 22 )) + "," + /* di: ghost[3].lastMoveMade           */
						          m.group( 25 )  + "," + /* pi: pills                           */
						          m.group( 26 )        ; /* po: powerPills                      */
				}
				else if( input.equals( "];" ))
				{
					break; /* Treat this as end of input */
				}
				replay.add( input );
			}
		}
		catch( IOException ioe )
		{
			//JOptionPane.showMessageDialog( this, ioe, "IOException", JOptionPane.WARNING_MESSAGE );
			ioe.printStackTrace();
			return null;
		}
		if( replay == null )
		{
			//JOptionPane.showMessageDialog( this, "Failed to load the game", "Load From File", JOptionPane.WARNING_MESSAGE );
		}
		return replay;
	}

	/**
	 * Open the file to a stream and kick it down to the real loader
	 */
	private ArrayList<String> loadReplayFromFile( String fileName )
	{
		try
		{
			return( loadReplayFromStream( new FileInputStream( fileName )));
		}
		catch( IOException ioe )
		{
			ioe.printStackTrace();
			//JOptionPane.showMessageDialog( this, ioe, "IOException", JOptionPane.WARNING_MESSAGE );
		}
		return null;
	}
	/**
	 * Make a network connection and kick it down to the real loader
	 */
	public ArrayList<String> loadReplayFromURL( String url ) throws Exception
	{
			URL url2 = new URL( url );
			URLConnection conn = url2.openConnection();
			return( loadReplayFromStream( conn.getInputStream()));
	}
	
	public void downloadAndSave(int gameNr) throws Exception {
		String url = "https://wcci12.s3.amazonaws.com/games/" + gameNr + "/replay.txt";
		System.out.println("downloading " + url);
		ArrayList<String> list = loadReplayFromURL(url);
		PrintStream out = new PrintStream(new File("games", gameNr + ".txt"));
		for (String s : list) {
			out.println(s);
		}
		out.close();
	}
	
	public static void main(String[] args) throws Exception {
		new Downloader().downloadAndSave(236790);
	}



}
