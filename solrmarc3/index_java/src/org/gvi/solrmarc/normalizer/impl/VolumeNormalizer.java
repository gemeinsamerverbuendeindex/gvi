package org.gvi.solrmarc.normalizer.impl;

import org.gvi.solrmarc.normalizer.IFieldMultiValueNormalizer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizer for volume Marc field (490v).
 */
public class VolumeNormalizer implements IFieldMultiValueNormalizer {
    /**
     * Regular expression to detect if string value has number.
     */
    private String reHasNum = ".*\\p{Digit}.*";
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
     * Normalize single value of volume Marc field.
     * First, invisible characters, punctuation and spaces will
     * be from text removed.
     * Second, positive numbers will be searched and, if found,
     * returned as string.
     * If positive numbers not found, than whole text without
     * special characters will be returned.
     *
     * @param value single value of volume Marc field.
     * @return - found volume or text without special characters.
     */
    private String normalize(final String value) {
        if (value == null) {
            return null;
        }
        String str;
        int num;
        str = value.replaceAll(reNotVis, "").
                replaceAll(rePunct, "").replaceAll(reSpaces, "");
        try {
            if (str.matches(reHasNum)) { //has numbers
                numMatcher = pattern.matcher(str);
                while (numMatcher.find()) {
                    num = Integer.parseInt(numMatcher.group());
                    if (num > 0) {
                        return Integer.toString(num);
                    }
                }
                return str.toLowerCase().trim();
            }
            else {
                return str.toLowerCase().trim();
            }
        }
        catch (Exception ex) {
            return str.toLowerCase().trim();
        }
    }

    @Override
    public final void normalize(final List<String> values,
                                final List<String> normValues) {
        String vol;
        normValues.clear();
        if (values == null) {
            return;
        }
        for (String s : values) {
            vol = normalize(s);
            if (vol != null && !vol.isEmpty()) {
                normValues.add(vol);
            }
        }
    }
}
