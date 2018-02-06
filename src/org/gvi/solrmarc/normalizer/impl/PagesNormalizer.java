package org.gvi.solrmarc.normalizer.impl;

import org.gvi.solrmarc.normalizer.IFieldMultiValueNormalizer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizer for pages Marc field (300a).
 */
public class PagesNormalizer implements IFieldMultiValueNormalizer {
    /**
     * Minimal number of pages in record.
     */
    private static final int MIN_PAGES_NUMBER = 10;
    /**
     * Regular expression to detect numbers in text.
     */
    private String reHasNum = ".*\\p{Digit}.*";
    /**
     * Regular expression to remove text in square brackets.
     */
    private String reBrackets = "\\[[^\\]]*\\]";
    /**
     * Regular expression to remove punctuation.
     */
    private String rePunct = "\\p{Punct}+";
    /**
     * Regular expression to remove duplicate spaces.
     */
    private String reSpaces = "\\s+";
    /**
     * Regular expression to detect sequence of digits.
     */
    private String reDigit = "\\p{Digit}+";
    /**
     * Pattern for detection sequence of digits.
     */
    private Pattern pattern = Pattern.compile(reDigit);
    /**
     * Regular expression to remove invisible UTF-8 symbols.
     */
    private String reNotVis = "[\u0000-\u001F\u007F-\u009F]";
    /**
     * Matcher for searching sequence of digits.
     */
    private Matcher numMatcher;

    /**
     * Normalize single value of pages Marc field.
     * Remove invisible symbols, text in square brackets and check,
     * if rest contains digits.
     * If contains, find maximal number and if it bigger as MIN_PAGES_NUMBER,
     * return it as String.
     * If maximal number smaller as MIN_PAGES_NUMBER or text not contains
     * digits, return text without duplicate spaces and punctuation.
     *
     * @param value single value of pages Marc field.
     * @return normalized value of field.
     */
    private String normalize(final String value) {
        int maxNum = -1;
        int num;
        if (value == null) {
            return null;
        }
        //remove brackets
        String str;
        str = value.replaceAll(reNotVis, "").replaceAll(reBrackets, "");
        try {
            if (str.matches(reHasNum)) { //has numbers
                numMatcher = pattern.matcher(str);
                numMatcher.reset();
                while (numMatcher.find()) {
                    num = Integer.parseInt(numMatcher.group());
                    if (num > maxNum) {
                        maxNum = num;
                    }
                }
                if (maxNum >= MIN_PAGES_NUMBER) {
                    return Integer.toString(maxNum);
                }
                else {
                    return str.replaceAll(rePunct, " ").
                            replaceAll(reSpaces, " ").trim().toLowerCase();
                }
            }
            else { //doesn't have numbers
                return value.replaceAll(reNotVis, "").
                        replaceAll(rePunct, " ").
                        replaceAll(reSpaces, " ").trim().toLowerCase();
            }
        }
        catch (Exception ex) {
            return value.replaceAll(reNotVis, "").
                    replaceAll(rePunct, " ").
                    replaceAll(reSpaces, " ").trim().toLowerCase();
        }
    }

    @Override
    public final void normalize(final List<String> values,
                                final List<String> normValues) {
        normValues.clear();
        if (values == null) {
            return;
        }
        String str;
        for (String s : values) {
            str = normalize(s);
            if (str != null && !str.isEmpty()) {
                normValues.add(str);
            }
        }
    }
}
