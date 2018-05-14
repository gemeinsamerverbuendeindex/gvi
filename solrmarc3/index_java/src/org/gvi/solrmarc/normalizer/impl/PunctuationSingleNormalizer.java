package org.gvi.solrmarc.normalizer.impl;

import org.gvi.solrmarc.normalizer.IFieldSingleValueNormalizer;

/**
 * Normalizer for single value fields, remove not visible symbols,
 * punctuation and duplicate spaces from text.
 */
public class PunctuationSingleNormalizer implements
        IFieldSingleValueNormalizer {
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

    @Override
    public final String normalize(final String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll(reNotVis, "").toLowerCase().
                replaceAll(rePunct, " ").
                replaceAll(reSpaces, " ").
                replaceAll("'", "").trim();
    }
}
