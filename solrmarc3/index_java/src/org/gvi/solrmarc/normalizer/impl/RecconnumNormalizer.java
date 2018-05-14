package org.gvi.solrmarc.normalizer.impl;

import org.gvi.solrmarc.normalizer.IFieldMultiValueNormalizer;

import java.util.List;

/**
 * Normalizer for record control number marc field (773w).
 */
public class RecconnumNormalizer implements IFieldMultiValueNormalizer {
    /**
     * Regular expression to remove punctuation.
     */
    private String rePunct = "\\p{Punct}+";
    /**
     * Regular expression to remove duplicate spaces.
     */
    private String reSpaces = "\\s+";
    /**
     * Regular expression to remove text in square brackets.
     */
    private String reBrackets = "\\([^\\)]*";
    /**
     * Regular expression to remove invisible UTF-8 symbols.
     */
    private String reNotVis = "[\u0000-\u001F\u007F-\u009F]";

    /**
     * Normalize single value of record control number Marc field.
     * Remove invisible symbols, text in square brackets,
     * punctuation and duplicate spaces.
     *
     * @param value value of record control number.
     * @return normalized String value.
     */
    private String normalize(final String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        //remove parentheses, punctuation and spaces
        return value.replaceAll(reNotVis, "").
                toLowerCase().replaceAll(reBrackets, "").
                replaceAll(rePunct, " ").replaceAll(reSpaces, " ").trim();
    }

    @Override
    public final void normalize(final List<String> values,
                                final List<String> normValues) {
        String recnum;
        normValues.clear();
        if (values == null) {
            return;
        }
        for (String s : values) {
            recnum = normalize(s);
            if (recnum != null && !recnum.isEmpty()) {
                normValues.add(recnum);
            }
        }
    }
}
