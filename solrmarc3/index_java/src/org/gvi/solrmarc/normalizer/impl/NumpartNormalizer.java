package org.gvi.solrmarc.normalizer.impl;

import org.gvi.solrmarc.normalizer.IFieldMultiValueNormalizer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizer for "Number of part" Marc field (245n).
 */
public class NumpartNormalizer implements IFieldMultiValueNormalizer {
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
     * Normalize single value of Numpart field.
     * Remove invisible symbols and text in square brackets, check,
     * if rest contains numbers.
     * If contains, first found number will be returned as String.
     * If not, text without punctuation
     * and duplicate spaces will be returned.
     *
     * @param value single value of Numpart field.
     * @return normalized value of field.
     */
    private String normalize(final String value) {
        if (value == null) {
            return null;
        }
        int num;
        //remove brackets
        String str;
        str = value.replaceAll(reNotVis, "").replaceAll(reBrackets, "");
        if (str.matches(reHasNum)) { //has numbers
            try {
                numMatcher = pattern.matcher(str);
                numMatcher.find();
                num = Integer.parseInt(numMatcher.group());
                return Integer.toString(num);
            }
            catch (Exception ex) {
                return str.replaceAll(rePunct, " ").
                        replaceAll(reSpaces, " ").trim().toLowerCase();
            }
        }
        else { //doesn't have numbers
            return str.replaceAll(rePunct, " ").
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
