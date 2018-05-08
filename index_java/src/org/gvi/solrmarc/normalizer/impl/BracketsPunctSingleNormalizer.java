package org.gvi.solrmarc.normalizer.impl;

import org.gvi.solrmarc.normalizer.IFieldSingleValueNormalizer;

/**
 * Common normalizer for different single Marc fields. Used to remove invisible
 * symbols, text in square brackets, duplicate spaces and punctuation.
 */
public class BracketsPunctSingleNormalizer
        implements IFieldSingleValueNormalizer {

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

    @Override
    public final String normalize(final String value) {
        if (value == null) {
            return null;
        }
        String str = value.replaceAll(reNotVis, "").toLowerCase().
                replaceAll(reBrackets, "").
                replaceAll(rePunct, " ").
                replaceAll(reSpaces, " ").trim();
        if (str.isEmpty()) {
            str = value.toLowerCase().replaceAll(rePunct, " ").
                    replaceAll(reSpaces, " ").trim();
            return str;
        }
        else {
            return str;
        }
    }
}
