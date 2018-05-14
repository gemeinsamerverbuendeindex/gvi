package org.gvi.solrmarc.normalizer.impl;

import org.gvi.solrmarc.normalizer.IFieldMultiValueNormalizer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalize scale Marc field (255a).
 */
public class ScaleNormalizer implements IFieldMultiValueNormalizer {
    /**
     * Regular expression to remove punctuation.
     */
    private String rePunct = "\\p{Punct}+";
    /**
     * Regular expression to remove duplicate spaces.
     */
    private String reSpaces = "\\s+";
    /**
     * Regular expression to detect numbers in text.
     */
    private String reHasNum = ".*\\p{Digit}.*";
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
     * Normalize single value of volume Marc field.
     * First, invisible characters will be from text removed.
     * Second, text "1:" will be searched and, if found, text after them will
     * be checked for number. If number found, it will be returned. If number
     * is not found, all text without punctuation ans spaces
     * after "1:" will be returned. In case of exception all text without
     * invisible symbols will be returned.
     *
     * @param value single value of scale Marc field.
     * @return - found scaling factor or text without special characters.
     */
    private String normalize(final String value) {
        if (value == null) {
            return null;
        }
        int num;
        String str = value.replaceAll(reNotVis, "").toLowerCase();
        int pos = str.indexOf("1:");
        if (pos >= 0) {
            try {
                String strFoundScale =
                        str.substring(pos + 2).replaceAll(reSpaces, "");
                if (str.matches(reHasNum)) { //has numbers
                    Matcher numMatcher = pattern.matcher(strFoundScale);
                    if (numMatcher.find()) {
                        num = Integer.parseInt(numMatcher.group());
                        return Integer.toString(num);
                    }
                    return strFoundScale.replaceAll(
                            rePunct, " ").replaceAll(reSpaces, " ").trim();
                }
                else {
                    return strFoundScale.replaceAll(
                            rePunct, " ").replaceAll(reSpaces, " ").trim();
                }
            }
            catch (Exception ex) {
                return str.replaceAll(rePunct, " ").replaceAll(reSpaces, " ").trim();
            }
        }
        return str.replaceAll(rePunct, " ").replaceAll(reSpaces, " ").trim();
    }

    @Override
    public final void normalize(final List<String> values,
                                final List<String> normValues) {
        String str;
        normValues.clear();
        if (values == null) {
            return;
        }
        for (String s : values) {
            str = normalize(s);
            if (str != null && !str.isEmpty()) {
                normValues.add(str);
            }
        }
    }
}
