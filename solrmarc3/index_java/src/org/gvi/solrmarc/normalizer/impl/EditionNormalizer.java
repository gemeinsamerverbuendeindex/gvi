package org.gvi.solrmarc.normalizer.impl;

import org.gvi.solrmarc.normalizer.IFieldMultiValueNormalizer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizer for edition Marc field (250a).
 */
public class EditionNormalizer implements IFieldMultiValueNormalizer {
    /**
     * Regular expression to detect if string value has number.
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
     * Regular expression to remove invisible UTF-8 symbols.
     */
    private String reNotVis = "[\u0000-\u001F\u007F-\u009F]";
    /**
     * Regular expression to detect sequence of digits.
     */
    private String reDigit = "\\p{Digit}+";
    /**
     * Pattern for detection sequence of digits.
     */
    private Pattern pattern = Pattern.compile(reDigit);
    /**
     * Matcher for searching sequence of digits.
     */
    private Matcher numMatcher;

    /**
     * Normalize single value of edition field.
     * Remove invisible symbols and text in square brackets.
     * Than, try to find maximal number in rest.
     * If found, return it as String. If not found,
     * try to find number in all text. If found,
     * return it as String. If not found and rest is empty,
     * return edition as "1". If rest is not empty, return
     * value without invisible symbols, without punctuation
     * and duplicate spaces.
     *
     * @param value single value of edition field.
     * @return normalized value of edition field.
     */
    private String normalize(final String value) {
        if (value == null) {
            return "1";
        }
        String str;
        int maxNum = -1;
        int num;
        //remove brackets
        str = value.replaceAll(reNotVis, "").replaceAll(reBrackets, "");
        try {
            if (str.matches(reHasNum)) { //has numbers
                numMatcher = pattern.matcher(str);
                while (numMatcher.find()) {
                    num = Integer.parseInt(numMatcher.group());
                    if (num > maxNum) {
                        maxNum = num;
                    }
                }
                return Integer.toString(maxNum);
            }
            else { //doesn't have numbers, check numbers in brackets
                if (value.matches(reHasNum)) {
                    numMatcher = pattern.matcher(value);
                    while (numMatcher.find()) {
                        num = Integer.parseInt(numMatcher.group());
                        if (num > maxNum) {
                            maxNum = num;
                        }
                    }
                    return Integer.toString(maxNum);
                }
                else {
                    str = str.replaceAll(reNotVis, "").
                            replaceAll(rePunct, " ").
                            replaceAll(reSpaces, " ").trim();
                    if (str.isEmpty()) {
                        return "1";
                    }
                    return str.toLowerCase();
                }
            }
        }
        catch (Exception ex) {
            str = str.replaceAll(reNotVis, "").
                    replaceAll(rePunct, " ").
                    replaceAll(reSpaces, " ").trim();
            if (str.isEmpty()) {
                return null;
            }
            return str.toLowerCase();
        }
    }

    @Override
    public final void normalize(final List<String> values,
                                final List<String> normValues) {
        normValues.clear();
        if (values == null) {
            normValues.add("1");
            return;
        }
        if (values.isEmpty()) {
            normValues.add("1");
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
