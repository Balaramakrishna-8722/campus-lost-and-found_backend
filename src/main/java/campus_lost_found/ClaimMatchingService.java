package campus_lost_found;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;

import campus_lost_found.model.Claim;
import campus_lost_found.model.Item;

@Service
public class ClaimMatchingService {

    public MatchResult analyze(Item foundItem, Claim claim) {

        int score = 0;
        StringBuilder reasons = new StringBuilder();

        /*
         * 1. ITEM NAME MATCH
         *
         * Example:
         *
         * Found item: "Blue Backpack"
         * Claim details: "Blue backpack with small front pocket"
         *
         * This is strong evidence.
         */
        double itemNameMatch = similarity(
                foundItem.getItemName(),
                claim.getIdentifyingDetails()
        );

        if (itemNameMatch >= 0.75) {

            score += 35;

            reasons.append(
                    "The claim details strongly match the found item name. "
            );

        } else if (itemNameMatch >= 0.40) {

            score += 20;

            reasons.append(
                    "The claim details partially match the found item name. "
            );
        }


        /*
         * 2. LOCATION MATCH
         *
         * Location is useful supporting evidence,
         * but should not dominate the result.
         */
        double locationMatch = similarity(
                foundItem.getLocation(),
                claim.getLostLocation()
        );

        if (locationMatch >= 0.75) {

            score += 15;

            reasons.append(
                    "The lost and found locations match. "
            );

        } else if (locationMatch > 0) {

            score += 8;

            reasons.append(
                    "The lost and found locations partially match. "
            );
        }


        /*
         * 3. DESCRIPTION MATCH
         *
         * We count meaningful shared words.
         *
         * One generic word such as "black" should
         * not automatically produce a large score.
         */
        DescriptionMatch descriptionMatch =
                compareDescription(
                        foundItem.getDescription(),
                        claim.getIdentifyingDetails()
                );

        if (descriptionMatch.matchingWords >= 3) {

            score += 30;

            reasons.append(
                    "Several identifying details match the found item's description. "
            );

        } else if (descriptionMatch.matchingWords == 2) {

            score += 20;

            reasons.append(
                    "Two identifying details match the found item's description. "
            );

        } else if (descriptionMatch.matchingWords == 1) {

            score += 8;

            reasons.append(
                    "One identifying detail matches the found item's description. "
            );
        }


        /*
         * 4. SPECIFIC ITEM INFORMATION
         *
         * If the claimant explicitly mentions a meaningful
         * part of the item name, give additional evidence.
         */
        if (containsMeaningfulItemWord(
                claim.getIdentifyingDetails(),
                foundItem.getItemName())) {

            score += 20;

            reasons.append(
                    "The claim contains a specific word from the found item name. "
            );
        }


        /*
         * Prevent scores above 100.
         */
        if (score > 100) {
            score = 100;
        }


        /*
         * Automatic recommendation.
         *
         * This is ONLY a recommendation.
         *
         * The administrator still makes the final decision.
         */
        String recommendation;

        if (score >= 80) {

            recommendation = "APPROVE";

        } else if (score >= 50) {

            recommendation = "REVIEW";

        } else {

            recommendation = "REJECT";
        }


        /*
         * If nothing matched, explain that clearly.
         */
        if (reasons.length() == 0) {

            reasons.append(
                    "There is not enough matching information."
            );
        }


        return new MatchResult(
                score,
                recommendation,
                reasons.toString()
        );
    }


    /*
     * Compares two pieces of text using meaningful words.
     */
    private double similarity(
            String first,
            String second) {

        Set<String> firstWords = getWords(first);
        Set<String> secondWords = getWords(second);

        if (firstWords.isEmpty()
                || secondWords.isEmpty()) {

            return 0;
        }

        int matchingWords = 0;

        for (String word1 : firstWords) {

            for (String word2 : secondWords) {

                if (wordsAreRelated(word1, word2)) {

                    matchingWords++;

                    break;
                }
            }
        }

        return (double) matchingWords
                / Math.max(
                        firstWords.size(),
                        secondWords.size()
                );
    }


    /*
     * Compares descriptions and returns the number
     * of meaningful matching words.
     */
    private DescriptionMatch compareDescription(
            String first,
            String second) {

        Set<String> firstWords = getWords(first);
        Set<String> secondWords = getWords(second);

        int matchingWords = 0;

        for (String word1 : firstWords) {

            for (String word2 : secondWords) {

                if (wordsAreRelated(word1, word2)) {

                    matchingWords++;

                    break;
                }
            }
        }

        return new DescriptionMatch(
                matchingWords
        );
    }


    /*
     * Converts text into clean words.
     *
     * It also handles cases such as:
     *
     * "blackcolor"
     *
     * by recognizing common combined color words.
     */
    private Set<String> getWords(String text) {

        Set<String> words = new HashSet<>();

        if (text == null) {
            return words;
        }

        String cleaned = text
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ");

        /*
         * Handle common combined color words.
         *
         * Example:
         *
         * blackcolor
         * ->
         * black color
         */
        cleaned = cleaned
                .replace("blackcolor", "black color")
                .replace("bluecolor", "blue color")
                .replace("redcolor", "red color")
                .replace("greencolor", "green color")
                .replace("whitecolor", "white color")
                .replace("yellowcolor", "yellow color")
                .replace("browncolor", "brown color")
                .replace("greycolor", "grey color")
                .replace("graycolor", "gray color");

        String[] splitWords =
                cleaned.trim().split("\\s+");

        for (String word : splitWords) {

            if (word.length() >= 2) {

                words.add(word);
            }
        }

        return words;
    }


    /*
     * Determines whether two words represent
     * the same or closely related information.
     */
    private boolean wordsAreRelated(
            String first,
            String second) {

        if (first.equals(second)) {

            return true;
        }


        /*
         * American/British spelling.
         */
        if (first.equals("color")
                && second.equals("colour")) {

            return true;
        }

        if (first.equals("colour")
                && second.equals("color")) {

            return true;
        }


        /*
         * Limited partial matching for long words.
         *
         * Example:
         *
         * backpack
         * backpacks
         *
         * But we avoid partial matching for short
         * generic words such as:
         *
         * black
         * class
         * phone
         */
        if (first.length() >= 7
                && second.length() >= 7) {

            if (first.contains(second)
                    || second.contains(first)) {

                return true;
            }
        }

        return false;
    }


    /*
     * Checks whether the claim contains a meaningful
     * word from the found item's name.
     *
     * Very short/common words are ignored.
     */
    private boolean containsMeaningfulItemWord(
            String text,
            String itemName) {

        if (text == null || itemName == null) {

            return false;
        }

        Set<String> textWords = getWords(text);
        Set<String> itemWords = getWords(itemName);

        for (String itemWord : itemWords) {

            /*
             * Ignore very short words because they
             * provide little evidence.
             */
            if (itemWord.length() < 4) {
                continue;
            }

            for (String textWord : textWords) {

                if (wordsAreRelated(
                        itemWord,
                        textWord)) {

                    return true;
                }
            }
        }

        return false;
    }


    /*
     * Stores the number of matching description words.
     */
    private static class DescriptionMatch {

        private final int matchingWords;

        public DescriptionMatch(
                int matchingWords) {

            this.matchingWords = matchingWords;
        }
    }


    /*
     * Result returned to the controller.
     */
    public static class MatchResult {

        private final int score;
        private final String recommendation;
        private final String reasons;

        public MatchResult(
                int score,
                String recommendation,
                String reasons) {

            this.score = score;
            this.recommendation = recommendation;
            this.reasons = reasons;
        }

        public int getScore() {

            return score;
        }

        public String getRecommendation() {

            return recommendation;
        }

        public String getReasons() {

            return reasons;
        }
    }
}