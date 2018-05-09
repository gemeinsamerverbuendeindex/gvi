package org.gvi.solrmarc.normalizer.impl;

import org.gvi.solrmarc.normalizer.IFieldMultiValueNormalizer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizer for publication year Marc field (260c).
 */
public class YearNormalizer implements IFieldMultiValueNormalizer {
    /**
     * Lowest value for year in value.
     */
    private static final int LOW_YEAR = 10;
    /**
     * Regular expression to detect if string value has number.
     */
    private final String reHasNum = ".*\\p{Digit}.*";
    /**
     * Regular expression to remove text in square brackets.
     */
    private final String reBrackets = "\\[[^\\]]*\\]";
    /**
     * Regular expression to remove punctuation.
     */
    private final String rePunct = "\\p{Punct}+";
    /**
     * Regular expression to remove duplicate spaces.
     */
    private final String reSpaces = "\\s+";
    /**
     * Regular expression to detect sequence of digits.
     */
    private final String reDigit = "\\p{Digit}+";
    /**
     * Pattern for detection sequence of digits.
     */
    private final Pattern pattern = Pattern.compile(reDigit);
    /**
     * Regular expression to remove invisible UTF-8 symbols.
     */
    private final String reNotVisible = "[\u0000-\u001F\u007F-\u009F]";
    /**
     * Matcher for searching sequence of digits.
     */
    private Matcher numMatcher;

    /**
     * Normalize single value of publication scale Marc field.
     * First, invisible character are removed and text in square brackets.
     * Second, in text first number greater than 9 will be searched and,
     * if found returned as string value.
     * If numbers not found in text, numbers greater than 9 will be searched
     * in full text including text in square brackets.
     * If numbers not found, than whole text without invisible symbols,
     * spaces and punctuation symbols will be returned.
     *
     * @param value string value of year field.
     * @return - found year as string or whole text without special characters.
     */
    private String normalize(final String value) {
        if (value == null) {
            return null;
        }
        int num;
        //remove brackets
        String str;
        str = value.replaceAll(reNotVisible, "").replaceAll(reBrackets, "");
        try {
            if (str.matches(reHasNum)) { //has numbers
                numMatcher = pattern.matcher(str);
                while (numMatcher.find()) {
                    num = Integer.parseInt(numMatcher.group());
                    if (num >= LOW_YEAR) {
                        return Integer.toString(num);
                    }
                }
                return str.replaceAll(rePunct, " ").
                        replaceAll(reSpaces, " ").trim().toLowerCase();
            }
            else { //doesn't have numbers
                if (value.matches(reHasNum)) { //has numbers
                    numMatcher = pattern.matcher(value);
                    while (numMatcher.find()) {
                        num = Integer.parseInt(numMatcher.group());
                        if (num >= LOW_YEAR) {
                            return Integer.toString(num);
                        }
                    }
                    return value.replaceAll(reNotVisible, "").
                            replaceAll(rePunct, " ").
                            replaceAll(reSpaces, " ").trim().toLowerCase();
                }
                else {
                    return value.replaceAll(reNotVisible, "").
                            replaceAll(rePunct, " ").
                            replaceAll(reSpaces, " ").trim().toLowerCase();
                }
            }
        }
        catch (Exception ex) {
            return value.replaceAll(rePunct, " ").
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
