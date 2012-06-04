package competition;

public class Result implements Comparable<Result> {
	public String opponent;
	public int controllerNr;
	public int score;
	
	@Override
	public int compareTo(Result other) {
		return other.score - score;
	}
	
	
}
