package dev.znlp.segment.impl.ner;

import java.util.*;

/**
 * NER Evaluate Utils
 *
 * @author jimichan
 */
public class NEREvaluateUtils {

    public static void printNERScore(Map<String, double[]> scores) {
        System.out.printf("%4s\t%6s\t%6s\t%6s\n", "NER", "P", "R", "F1");
        for (Map.Entry<String, double[]> entry : scores.entrySet()) {
            String type = entry.getKey();
            double[] s = entry.getValue();
            System.out.printf("%4s\t%6.2f\t%6.2f\t%6.2f\n", type, s[0], s[1], s[2]);
        }
    }

    public static Set<String> combineNER(List<String> nerArray) {
        Set<String> result = new LinkedHashSet<>();
        int begin = 0;
        if (nerArray.isEmpty()) return result;

        String prePos = posOf(nerArray.get(0));
        for (int i = 1; i < nerArray.size(); i++) {
            String tag = nerArray.get(i);
            char firstChar = tag.isEmpty() ? ' ' : tag.charAt(0);
            
            // Start of a new entity or O
            if (firstChar == 'B' || firstChar == 'S' || firstChar == 'O') {
                // If previous was an entity part (not O), add it
                if (i - begin > 0 && !"O".equals(prePos)) {
                    // Only add if it's a valid entity sequence? 
                    // Simple logic: from 'begin' to 'i', entity type is 'prePos'
                    result.add(String.format("%d\t%d\t%s", begin, i, prePos));
                }
                begin = i;
            }
            // Update prePos for the new segment
            prePos = posOf(nerArray.get(i));
        }
        
        // Handle the last segment
        if (nerArray.size() - begin > 0 && !"O".equals(prePos)) {
             result.add(String.format("%d\t%d\t%s", begin, nerArray.size(), prePos));
        }
        return result;
    }

    private static String posOf(String label) {
        int index = label.indexOf('-');
        if (index == -1) {
            // If label is "O", return "O"
            // If label is "nr" (should not happen in B-nr scheme, but safe to return)
            return label;
        } else {
            return label.substring(index + 1);
        }
    }
}
