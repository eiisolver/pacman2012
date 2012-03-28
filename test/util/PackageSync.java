package util;

import java.io.*;

/**
 * synchronizes pacman.entries.ghosts.graph and  pacman.entries.pacman.graph
 * @author louis
 *
 */
public class PackageSync {

	public static void copyFile(File src, File dest) throws Exception {
		System.out.println("copy " + src );
		System.out.println("  to: " + dest);
		BufferedReader in = new BufferedReader(new FileReader(src));
		PrintStream out = new PrintStream(dest);
		for (String line = in.readLine(); line != null; line = in.readLine()) {
			if (line.startsWith("package pacman.entries.ghosts.graph;")) {
				out.println("package pacman.entries.pacman.graph;");
			} else {
				out.println(line);
			}
		}
		out.close();
		in.close();
	}
	public static void main(String[] args) throws Exception {
		File dir1 = new File("src/pacman/entries/ghosts/graph");
		File dir2 = new File("src/pacman/entries/pacman/graph");
		File[] files = dir1.listFiles();
		for (File file : files) {
			File destFile = new File(dir2, file.getName());
			copyFile(file, destFile);
		}
	}
}
