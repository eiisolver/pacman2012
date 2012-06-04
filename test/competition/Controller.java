package competition;

import java.io.*;
import java.util.*;

public class Controller implements Comparable<Controller> {
	public String name;
	public List<Result> results = new ArrayList<Result>();
	public boolean isGhost;
	public int controllerNr;
	public static Comparator<Controller> AVERAGE_OF_AVERAGE_COMPARATOR = new Comparator<Controller>() {

		@Override
		public int compare(Controller c1, Controller c2) {
			double result = c2.averageOfAverages() - c1.averageOfAverages();
			if (c1.isGhost) {
				result = -result;
			}
			if (result > 0) {
				return 1;
			} else if (result < 0) {
				return -1;
			}
			return 0;
		}
		
	};
	
	public double average() {
		if (results.size() == 0) {
			return 0;
		}
		int total = 0;
		for (Result result : results) {
			total += result.score;
		}
		return ((double) total)/results.size();
	}
	
	public double average(String opponent) {
		int total = 0;
		int nr = 0;
		for (Result result : results) {
			if (result.opponent.equals(opponent)) {
				++nr;
				total += result.score;
			}
		}
		if (nr == 0) {
			return 0;
		} else {
			return ((double) total)/nr;
		}
	}
	
	/**
	 * Returns the names of all opponents
	 * @return
	 */
	public Set<String> getOpponents() {
		HashSet<String> opponents = new HashSet<String>();
		for (Result result : results) {
			opponents.add(result.opponent);
		}
		return opponents;
	}
	
	public double averageOfAverages() {
		Set<String> opponents = getOpponents();
		if (opponents.size() == 0) {
			return 0;
		}
		double sum = 0;
		for (String opponent : opponents) {
			sum += average(opponent);
		}
		return sum/opponents.size();
	}
	
	public void print(PrintStream out) {
		out.println((isGhost ? "Ghosts" : "Pacman") + ": " + name);
		Collections.sort(results);
		for (Result result : results) {
			out.printf("  %-20s %10d\n", result.opponent, result.score);
		}
		out.printf("Average: %10.2f\n", average());
		out.println("Nr games: " + results.size());
	}

	@Override
	public int compareTo(Controller controller) {
		double result = controller.average() - average();
		if (isGhost) {
			result = -result;
		}
		if (result > 0) {
			return 1;
		} else if (result < 0) {
			return -1;
		}
		return 0;
	}

	@Override
	public String toString() {
		return "Controller [name=" + name
				+ ", isGhost=" + isGhost + ", controllerNr=" + controllerNr
				+ "]";
	}
	
}
