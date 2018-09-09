package ug.air.switchaccess;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import java.util.List;

/**
 * Created by jq on 8/26/17.
 */

public class LanguageModel {

    @SerializedName("conditional_word_frequencies")
    Map<String, List<String>> conditionalWordFrequency;

    @SerializedName("first_word_frequency_ranking")
    List<String> firstWordFrequencyRanking;

    @SerializedName("word_frequency_ranking")
    List<String> wordFrequencyRanking;

    @SerializedName("capitalised_words")
    Map<String,Boolean> capitalisedWords;

    public LanguageModel() {

    }

    public String[] mostFrequentWords(String previousWord, String beginning, int numWords) {
        String[] wordList = new String[numWords];

        if (previousWord.length()>8) {
            previousWord = previousWord.substring(0,7);
        }

        // Initialise to list of empty strings
        for (int i=0;i<numWords;i++) {
            wordList[i] = " "; // empty strings cause crash in adjustCase
        }
        int numMatchesFound = 0;

        // Find the matching words which match the current beginning text
        if (previousWord.length()==0) {
            int i = 0;
            while (numMatchesFound<numWords && i<firstWordFrequencyRanking.size()) {
                if (firstWordFrequencyRanking.get(i).startsWith(beginning.toLowerCase())) {
                    wordList[numMatchesFound] = firstWordFrequencyRanking.get(i);
                    numMatchesFound++;
                }
                i++;
            }
        } else {
            if (conditionalWordFrequency.containsKey(previousWord)) {
                int i = 0;
                while (numMatchesFound < numWords && i < conditionalWordFrequency.get(previousWord).size()) {
                    if (conditionalWordFrequency.get(previousWord).get(i).startsWith(beginning.toLowerCase())) {
                        wordList[numMatchesFound] = conditionalWordFrequency.get(previousWord).get(i);
                        numMatchesFound++;
                    }
                    i++;
                }
            }
        }

        // If we haven't found enough suggestions based on previous word, look for the most common
        // words that fit the characters so far.
        if (numMatchesFound<numWords) {
            int i = 0;
            while (numMatchesFound<numWords && i<wordFrequencyRanking.size()) {
                if (wordFrequencyRanking.get(i).startsWith(beginning.toLowerCase())) {
                    wordList[numMatchesFound] = wordFrequencyRanking.get(i);
                    numMatchesFound++;
                }
                i++;
            }
        }

        // Some words are normally capitalised
        for (int i=0;i<numMatchesFound;i++) {
            if (capitalisedWords.containsKey(wordList[i])) {
                wordList[i] = wordList[i].substring(0, 1).toUpperCase() + wordList[i].substring(1);
            }
        }

        return wordList;
    }
}
