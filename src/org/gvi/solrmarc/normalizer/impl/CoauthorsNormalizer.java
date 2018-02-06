package org.gvi.solrmarc.normalizer.impl;

import org.gvi.solrmarc.normalizer.IFieldMultiToSingleNormalizer;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Normalizer for coauthors Marc field (700a).
 */
public class CoauthorsNormalizer implements IFieldMultiToSingleNormalizer {
    /**
     * Number of used coauthors for comparison.
     */
    private static final int NUMBER_OF_USED_COAUTHORS = 3;
    /**
     * Regular expression to remove punctuation.
     */
    private String rePunct = "\\p{Punct}+";
    /**
     * Regular expression to remove duplicate spaces.
     */
    private String reSpaces = "\\s+";
    /**
     * Regular expression to remove invisible UTF-8 symbols.
     */
    private String reNotVis = "[\u0000-\u001F\u007F-\u009F]";
    /**
     * Comparator used to sort values of field.
     */
    private StringComparator comparator = new StringComparator();

    /**
     * Normalize single value of coauthors field.
     * Remove invisible symbols, punctuation and duplicate spaces.
     *
     * @param value single value of coauthors field.
     * @return normalized value.
     */
    private String normalize(final String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll(reNotVis, "").toLowerCase().
                replaceAll(rePunct, " ").
                replaceAll(reSpaces, " ").trim();
    }

    @Override
    /**
     * Normalize content of coauthors field, make single
     * String from all values. Sort all values alphabetical
     * and concatenate first 3 values of field.
     */
    public final String normalize(final List<String> value) {
        String s = null;
        if (value == null) {
            return null;
        }
        for (int i = 0; i < value.size(); i++) {
            s = value.get(i);
            value.set(i, normalize(s));
        }
        Collections.sort(value, comparator);
        switch (value.size()) {
            case 0:
                s = "";
                break;
            case 1:
                if (value.get(0) == null) {
                    s = "";
                }
                else {
                    s = value.get(0);
                }
//                s = ((value.get(0) == null) ? "" : value.get(0));
                break;
            case 2:
                if (value.get(0) == null) {
                    s = "" + " ";
                }
                else {
                    s = value.get(0) + " ";
                }
                if (value.get(1) == null) {
                    s += "";
                }
                else {
                    s += value.get(1);
                }
                break;
            case NUMBER_OF_USED_COAUTHORS:
                if (value.get(0) == null) {
                    s = "" + " ";
                }
                else {
                    s = value.get(0) + " ";
                }
                if (value.get(1) == null) {
                    s += "";
                }
                else {
                    s += value.get(1) + " ";
                }
                if (value.get(2) == null) {
                    s = "";
                }
                else {
                    s = value.get(2);
                }
                break;
            default:
                if (value.get(0) == null) {
                    s = "" + " ";
                }
                else {
                    s = value.get(0) + " ";
                }
                if (value.get(1) == null) {
                    s += "";
                }
                else {
                    s += value.get(1) + " ";
                }
                if (value.get(2) == null) {
                    s += "";
                }
                else {
                    s += value.get(2);
                }
                break;
        }
        return s.trim();
    }

    /**
     * Inner class for comparison two strings including cases
     * if one of them or both are not defined.
     */
    private class StringComparator implements Comparator<String> {

        /**
         * Compare 2 String values including case if
         * one or both of them are empty or null.
         *
         * @param s  first String for comparison.
         * @param s2 second String for comparison.
         * @return 0 - if both of them empty or not defined,
         * 1 - if only first is empty or not defined,
         * -1 - if second is empty or not defined
         * and result of usual comparison of two Strings
         * in another cases.
         */
        public int compare(final String s, final String s2) {
            if (s == null || s.isEmpty()) {
                if (s2 == null || s2.isEmpty()) {
                    return 0;
                }
                else {
                    return 1;
                }
            }
            else {
                if (s2 == null || s2.isEmpty()) {
                    return -1;
                }
                else {
                    return s.compareTo(s2);
                }
            }
        }
    }
}
