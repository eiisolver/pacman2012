package test;

public class Test {
    static class LL<T> {
        public T obj;
        public LL<T> next;
        public LL<T> prev;

        public void insert(LL<T> elem){
            elem.next = next;
            elem.prev = prev;
            next.prev = elem;
            next = elem;
        }
    }

    public boolean match(int[] arr, int n) {
        System.out.print("match: n=" + n + ", arr=");
        for (int v : arr) {
            System.out.print(v + " ");
        }
        System.out.println();
        if (n == 0) {
            return true;
        }
        for (int i = 0; i < arr.length; ++i) {
            for (int j = 0; j < n; ++j) {
                if ((arr[i] & (1 <<j)) != 0) {
                    int mask = (1 << j) - 1;
                    System.out.println("trying i = " + i + ", j = " + j);
                    int[] arr2 = new int[arr.length];
                    for (int k = 0; k < arr.length; ++k) {
                        if (k == i) {
                            arr2[k] = 0;
                        } else {
                            arr2[k] = (arr[k] & mask)
                                    | ((arr[k] >> 1) & ~mask);
                        }
                    }
                    if (match(arr2, n-1)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void checkMatch(int[] arr, int n, boolean expected) {
        System.out.println("Check match");
        if (match(arr, n) != expected) {
            System.out.println("Wrong");
            throw new RuntimeException("Wrong");
        } else {
            System.out.println("Correct");
        }
    }

    public void testMatch() {
        checkMatch(new int[]{3, 0, 0}, 2, false);
        checkMatch(new int[]{1, 1, 0}, 2, false);
        checkMatch(new int[]{2, 0, 3}, 2, true);
        checkMatch(new int[]{2, 0, 1}, 2, true);
        checkMatch(new int[]{1, 1, 6}, 3, false);
        checkMatch(new int[]{1, 2, 4}, 3, true);
    }

    public static void main(String[] args) {
        new Test().testMatch();
    }
}
