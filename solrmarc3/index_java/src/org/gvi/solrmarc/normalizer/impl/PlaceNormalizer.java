package org.gvi.solrmarc.normalizer.impl;

import org.gvi.solrmarc.normalizer.IFieldMultiValueNormalizer;

import java.util.List;

/**
 * Normalizer for place Marc field (260a).
 */
public class PlaceNormalizer implements IFieldMultiValueNormalizer {
    /**
     * Length of normalized place field.
     */
    private static final int LENGTH_OF_PLACE = 5;
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
     * Normalize single value of place Marc field.
     * First, invisible characters, punctuation and duplicate
     * spaces will be removed from text,
     * than first word in text will be returned if it has length
     * smaller as 5 or first 5 symbols of first word.
     *
     * @param value single value of place Marc field.
     * @return - normalized value of place Marc field..
     */
    private String normalize(final String value) {
        String str;
        String retStr = "";
        if (value == null) {
            return null;
        }
        //remove brackets, punctuation, duplicate spaces
        str = value.replaceAll(reNotVis, "").replaceAll(reBrackets, "");
        str = str.replaceAll(rePunct, " ").replaceAll(reSpaces, " ").trim();
        int endword = str.indexOf(' ');
        if (endword == -1) {
            if (str.length() >= LENGTH_OF_PLACE) {
                retStr = str.substring(0, LENGTH_OF_PLACE);
            }
            else {
                retStr = str;
            }
        }
        else {
            if (endword >= LENGTH_OF_PLACE) {
                retStr = str.substring(0, LENGTH_OF_PLACE);
            }
            else {
                retStr = str.substring(0, endword);
            }
        }
        return retStr.toLowerCase();
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
