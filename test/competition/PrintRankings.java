package competition;

import java.io.*;
import java.util.*;
import java.net.URL;
import java.net.URLConnection;

public class PrintRankings {
	private static PrintStream out;
	
	private static String[] pacmans = new String[] {
		"Memetix", "234",
		"maastricht", "546",
		"ICEP-feat-Spooks", "612",
		"sir_macelon", "629",
		"eiisolver", "610",
	};
	
	private static int parseScore(String line) {
		String nr = line.trim().replaceAll(",", "");
		return Integer.parseInt(nr);
	}
	
	private static String stripTags(String line) {
		int index1 = line.indexOf('>');
		int index2 = line.indexOf('<', index1+1);
		String value = line.substring(index1+1, index2);
		return value;
	}
	
	private static int parseControllerNr(String line) {
		String find = "/controllers/";
		int index1 = line.indexOf(find);
		int index2 = line.indexOf('\"', index1+find.length());
		String nrStr = line.substring(index1+find.length(), index2);
		int nr = Integer.parseInt(nrStr);
		return nr;
	}
	
	/**
	 * Downloads HTML page from pacman website with all games for the given controller,
	 * parses the HTML code and returns a Controller object with the parsed info.
	 * @throws Exception
	 */
	public static Controller parseController(String name, int controller, boolean isGhost) throws Exception {
		String url = "http://www.pacman-vs-ghosts.net/controllers/" + controller;
		URL url2 = new URL(url);
		URLConnection conn = url2.openConnection();
		BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		boolean inResults = false;
		int tdCount = 0;
		Controller c = new Controller();
		c.isGhost = isGhost;
		c.name = name;
		System.out.println("Controller: " + name);
		Result result = new Result();
		for (String line = in.readLine(); line != null; line = in.readLine()) {
			if (!inResults) {
				inResults = line.indexOf("<legend>Games</legend>") >= 0;
			} else {
				//System.out.println(line);
				if (line.indexOf("</tbody>" ) >= 0) {
					break;
				}
				if (line.indexOf("<tr>") >= 0) {
					tdCount = 0;
				} else if (line.indexOf("<td>") >= 0) {
					++tdCount;
				} else if (line.indexOf("</td>") >= 0 || line.indexOf("</tr>") >= 0) {
				} else if (tdCount == 2 && isGhost) {
					result.opponent = stripTags(line);
					//System.out.println("pacman opponent = " + result.opponent);
				} else if (tdCount == 3 && isGhost) {
					result.controllerNr = parseControllerNr(line);
				} else if (tdCount == 4 && !isGhost) {
					result.opponent = stripTags(line);
					//System.out.println("ghost opponent = " + result.opponent);
				} else if (tdCount == 5 && !isGhost) {
					result.controllerNr = parseControllerNr(line);
				} else if (tdCount == 6) {
					result.score = parseScore(line);
					c.results.add(result);
					//System.out.println("score = " + result.score);
					result = new Result();
				}
			}
		}
		in.close();
		System.out.println("Average score: " + c.average() + ", nr games: " + c.results.size());
		return c;
	}
	
	private static Controller find(List<Controller> list, String name) {
		for (Controller c : list) {
			if (c.name.equals(name)) {
				return c;
			}
		}
		return null;
	}
	
	private static List<Controller> getGhostControllers(List<Controller> pacmanList) {
		List<Controller> list = new ArrayList<Controller>();
		for (Controller c : pacmanList) {
			for (Result r : c.results) {
				if (find(list, r.opponent) == null) {
					Controller ghost = new Controller();
					ghost.isGhost = !c.isGhost;
					ghost.name = r.opponent.trim();
					ghost.controllerNr = r.controllerNr;
					list.add(ghost);
				}
			}
		}
		return list;
	}
	
	public static List<Controller> parseControllers(String[] spec, boolean isGhost) throws Exception {
		List<Controller> controllers = new ArrayList<Controller>();
		for (int i = 0; i < spec.length; i += 2) {
			String name = spec[i];
			int nr = Integer.parseInt(spec[i+1]);
			Controller c = parseController(name, nr, isGhost);
			controllers.add(c);
		}
		return controllers;
	}
	
	public static List<Controller> parseControllers(List<Controller> spec) throws Exception {
		List<Controller> controllers = new ArrayList<Controller>();
		for (Controller cInput : spec) {
			System.out.println("Ghost " + cInput);
			if (cInput.controllerNr > 0) {
				Controller c = parseController(cInput.name, cInput.controllerNr, cInput.isGhost);
				controllers.add(c);
				//c.print(System.out);
			}
		}
		return controllers;
	}
	
	public static void printControllers(List<Controller> controllers, boolean rankOnly) {
		Collections.sort(controllers);
		int rank = 1;
		if (rankOnly) {
			out.printf("%3s %-20s %10s %10s\n", "", "controller", "Avg.", "Games played");
		}
		for (Controller c : controllers) {
			if (rankOnly) {
				out.printf("%3d %-20s %10.2f %10d\n", rank, c.name, c.average(), c.results.size());
			} else {
				out.println("Rank: " + rank);
				c.print(out);
				out.println();
			}
			++rank;
		}
	}
	
	public static void printAverageOfAverageRankings(List<Controller> controllers) {
		List<Controller> avgAvgList = new ArrayList<Controller>(controllers);
		Collections.sort(avgAvgList, Controller.AVERAGE_OF_AVERAGE_COMPARATOR);
		int rank = 1;
		out.printf("%3s %-20s %10s %10s\n", "", "controller", "Avg.", "Unique opponents");
		for (Controller c : avgAvgList) {
			out.printf("%3d %-20s %10.2f %10d\n", rank, c.name, c.averageOfAverages(), c.getOpponents().size());
			++rank;
		}
	}

	private static boolean isTopController(String name, List<Controller> controllerList, int nrToCompare) {
		for (int i = 0; i < controllerList.size() && i < nrToCompare; i++) {
			Controller c = controllerList.get(i);
			if (c.name.equals(name)) {
				return true;
			}
		}
		return false;
	}
	
	public static void printFilteredControllers(List<Controller> controllers, List<Controller> opponents, int nrToCompare) {
		out.println("FILTERED");
		Collections.sort(controllers);
		for (Controller c : controllers) {
			out.println(c.name);
			out.println("--------------");
			for (Result result : c.results) {
				if (isTopController(result.opponent.trim(), opponents, nrToCompare)) {
					out.printf("%-20s %15d\n", result.opponent, result.score);
				}
			}
			out.println();
		}
	}
	
	public static void compareControllers(Controller c1, Controller c2) {
		double sum1 = 0;
		double sum2 = 0;
		int nrCommonOpponents = 0;
		Set<String> opponentSet = new HashSet<String>();
		for (Result result : c1.results) {
			if (!opponentSet.contains(result.opponent)) {
				opponentSet.add(result.opponent);
				double avg1 = c1.average(result.opponent);
				double avg2 = c2.average(result.opponent);
				if (avg2 > 0) {
					sum1 += avg1;
					sum2 += avg2;
					++nrCommonOpponents;
				}
			}
		}
		out.println("Comparison " + c1.name + " vs " + c2.name);
		out.printf("%-20s: average: %10.2f\n", c1.name, (sum1/nrCommonOpponents));
		out.printf("%-20s: average: %10.2f\n", c2.name, (sum2/nrCommonOpponents));
		out.println("Nr common opponents: " + nrCommonOpponents);
		out.println();
	}

	public static void main(String[] args) throws Exception {
		out = new PrintStream(new File("rankings.txt"));
		List<Controller>pacmanList = parseControllers(pacmans, false);
		List<Controller>ghostSpec = getGhostControllers(pacmanList);
		List<Controller>ghostList = parseControllers(ghostSpec);
		Collections.sort(ghostList);
		List<Controller>fullPacmanSpec = getGhostControllers(ghostList);
		List<Controller>fullPacmanList = parseControllers(fullPacmanSpec);
		Collections.sort(fullPacmanList);
		out.println("Standings per " + new Date());
		out.println("PACMAN RANKING");
		printControllers(fullPacmanList, true);
		out.println();
		out.println("GHOSTS RANKING");
		printControllers(ghostList, true);
		out.println();
		out.println("PACMAN AVERAGE OF AVERAGES");
		out.println();
		out.println("Average of average against each opponent");
		out.println();
		printAverageOfAverageRankings(fullPacmanList);
		out.println("GHOSTS AVERAGE OF AVERAGES");
		out.println();
		out.println("Average of average against each opponent");
		out.println();
		printAverageOfAverageRankings(ghostList);
		out.println();
		int nrToCompare = 6;
		out.println("TOP PACMAN CONTROLLER COMPARISON");
		for (int i = 0; i < nrToCompare; ++i) {
			for (int j = i+1; j < nrToCompare; ++j) {
				compareControllers(fullPacmanList.get(i), fullPacmanList.get(j));
			}
		}
		out.println();
		out.println("TOP GHOST CONTROLLER COMPARISON");
		for (int i = 0; i < nrToCompare; ++i) {
			for (int j = i+1; j < nrToCompare; ++j) {
				compareControllers(ghostList.get(i), ghostList.get(j));
			}
		}
		out.println("DETAILED PACMAN RESULTS");
		printControllers(fullPacmanList, false);
		out.println();
		out.println("DETAILED GHOST RESULTS");
		printControllers(ghostList, false);
		out.println();
		out.println("GHOST RESULTS AGAINST TOP PACMAN CONTROLLERS");
		printFilteredControllers(ghostList, fullPacmanList, nrToCompare);
		out.println();
		out.println("PACMAN RESULTS AGAINST TOP GHOST CONTROLLERS");
		printFilteredControllers(fullPacmanList, ghostList, nrToCompare);
	}
}
