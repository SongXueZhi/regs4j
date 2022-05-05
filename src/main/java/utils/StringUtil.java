package utils;

import java.util.Collection;
import java.util.StringJoiner;

public class StringUtil {
    /**
     * Compares the similarity between two strings using OSA Damerau-Levenshtein distance. A value between 0 to 1
     * will be returned where 0 means that the two strings has no similarity at all while 1 means that the two strings
     * are exactly the same.
     * @param s1 the first string to compare
     * @param s2 the second string to compare
     * @return the similarity value between the two strings
     */
    public static double editDistance(String s1, String s2) {
        /* implements OSA Damerau-Levenshtein distance from  
         * https://en.wikipedia.org/wiki/Damerau%E2%80%93Levenshtein_distance#Optimal_string_alignment_distance */
        // add padding in front
        s1 = " " + s1;
        s2 = " " + s2;

        char[] a1 = s1.toCharArray();
        char[] a2 = s2.toCharArray();

        // initialize the array
        int[][] distanceMatrix = new int[a1.length][a2.length];
        for (int i = 0; i < distanceMatrix.length; i++)
            distanceMatrix[i][0] = i;
        for (int i = 0; i < distanceMatrix[0].length; i++)
            distanceMatrix[0][i] = i;

        for (int i = 1; i < distanceMatrix.length; i++) {
            for (int j = 1; j < distanceMatrix[0].length; j++) {
                int cost = a1[i] == a2[j] ? 0 : 1;
                distanceMatrix[i][j] = minimum(distanceMatrix[i-1][j] + 1, 
                                                  distanceMatrix[i][j-1] + 1,
                                                  distanceMatrix[i-1][j-1] + cost);
                if (i > 1 && j > 1 && a1[i] == a2[j - 1] && a1[i -1] == a2[j]) {
                    distanceMatrix[i][j] = minimum(distanceMatrix[i][j],
                                                   distanceMatrix[i-2][j-2] + 1);
                }
            }
        }
        int editDistance = distanceMatrix[s1.length() - 1][s2.length() - 1];
        double lenSum = (double) (s1.length() - 1 + s2.length() - 1);
        double similarity =  lenSum - editDistance;
        return similarity / lenSum;
    }

    private static int minimum (int a, int... b) {
        int min = a;
        for (int i : b) {
            min = min < i ? min : i;
        }
        return min;
    }

    public static String reduceWhitespace(String s) {
        s = s.replaceAll("[\\r\\n]+", " ");
        s = s.replaceAll("\\s+", " ");
        return s;
    }

    public static String join(Collection<String> strings, CharSequence delimiter) {
        if (strings == null)
            return null;
        StringJoiner sj = new StringJoiner(delimiter);
        for (String s: strings) {
            sj.add(s);
        }
        return sj.toString();
    }
}
