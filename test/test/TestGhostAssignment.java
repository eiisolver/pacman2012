package test;

import java.util.Arrays;

import pacman.entries.ghosts.graph.*;

public class TestGhostAssignment {
	
	public void assrt(boolean assertion) {
		if (!assertion) {
			System.out.println("Assertion failed");
			throw new RuntimeException("Assertion failed");
		}
	}
	public void check(int[] edges, int expectedNr) {
		System.out.println(Arrays.toString(edges) + ", " + expectedNr);
		BorderEdge[] borders = new BorderEdge[edges.length];
		for (int i = 0; i < borders.length; ++i) {
			borders[i] = new BorderEdge();
			borders[i].closerGhosts = edges[i];
		}
		GhostAssignment assignment = new GhostAssignment();
		assignment.calcAssignment(borders, borders.length);
		System.out.println(assignment);
		assrt(assignment.bestNrAssignedGhosts == expectedNr);
	}
	
	public void test() {
		check(new int[] {1, 2, 4, 8}, 4);
		check(new int[] {8, 1, 2, 4}, 4);
		check(new int[] {1, 1, 1, 1}, 1);
		check(new int[] {3, 1, 1, 1}, 2);
		check(new int[] {8, 3, 6, 4}, 4);
		check(new int[] {10, 2, 8}, 2);
		check(new int[] {11, 2, 8}, 3);
		check(new int[] {12, 4, 8}, 2);
		check(new int[] {2, 10, 4, 8, 10, 9}, 4);
	}

    public static void main(String[] args) {
    	new TestGhostAssignment().test();
    }
}
