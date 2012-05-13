package test;

import java.io.*;
import java.util.*;

public class DownloadAndView {
	public static int gameNr = 962405;
	
	public static List<String> load(int gameNr) throws Exception {
		String fileName = "games/" + gameNr + ".txt";
		ArrayList<String> result = new ArrayList<String>();
		BufferedReader in = new BufferedReader(new FileReader(fileName));
		for (String line = in.readLine(); line != null; line = in.readLine()) {
			result.add(line);
		}
		return result;
	}

	public static void main(String[] args) throws Exception {
		String fileName = "games/" + gameNr + ".txt";
		if (!new File(fileName).exists()) {
			new Downloader().downloadAndSave(gameNr);
		}
		new Viewer().replayGame(fileName, true);
	}
}
