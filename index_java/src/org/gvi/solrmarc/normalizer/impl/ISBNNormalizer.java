package org.gvi.solrmarc.normalizer.impl;

import org.gvi.solrmarc.normalizer.IFieldMultiValueNormalizer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizer for ISBN Marc field (020a).
 */
public class ISBNNormalizer implements IFieldMultiValueNormalizer {
    /**
     * Minimal length of ISBN.
     */
    private static final int MIN_ISBN_LENGTH = 10;
    /**
     * Maximal length of ISBN.
     */
    private static final int MAX_ISBN_LENGTH = 13;
    /**
     * Regular expression to replace all characters "o" on the "0".
     */
    private String reO = "o";
    /**
     * Regular expression to remove all spaces.
     */
    private String reSpaces = "\\s+";
    /**
     * Regular expression to remove invisible UTF-8 symbols.
     */
    private String reNotVis = "[\u0000-\u001F\u007F-\u009F]";


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

    /**
     * Normalize single value of ISBN.
     * Remove invisible symbols and delimiters,
     * replace characters "o" on "0" and
     * try to find sequence, representing ISBN.
     * If sequence found, return it as String.
     * If sequence is not found, return null.
     *
     * @param value single value of ISBN field.
     * @return ISBN or null.
     */
    private String normalize(final String value) {
        if (value == null) {
            return null;
        }
        String reFilter = ".*[0-9]+x*.*";
        String reBinder = "-+";
        Pattern p = Pattern.compile("[0-9]+x*");
        Matcher m;
        String isbn = null;
        int len = 0;
        String str = value.replaceAll(reNotVis, "").toLowerCase().
                replaceAll(reBinder, "").
                replaceAll(reO, "0").
                replaceAll(reSpaces, "");
        try {
            if (str.matches(reFilter)) {  //it is ISBN
                m = p.matcher(str);
                m.reset();
                while (m.find()) {
                    isbn = m.group();
                    len = isbn.length();
                    if (len >= MIN_ISBN_LENGTH && len <= MAX_ISBN_LENGTH) {
                        return isbn;
                    }
                }
            }
            else { //not ISBN
                return null;
            }
        }
        catch (Exception ex) {
            return null;
        }
        return null;
    }
}
