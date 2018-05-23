package org.gvi.solrmarc.normalizer.impl;

import org.gvi.solrmarc.normalizer.IFieldMultiValueNormalizer;

import java.util.List;

/**
 * Normalizer for "Name of part" Marc field.
 */
public class NampartNormalizer implements IFieldMultiValueNormalizer {
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
     * Normalize single value of Nampart field.
     * Remove invisible symbols, text in square brackets,
     * punctuation and duplicate spaces.
     *
     * @param value single value of field.
     * @return normalized value.
     */
    private String normalize(final String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll(reNotVis, "").toLowerCase().
                replaceAll(reBrackets, "").
                replaceAll(rePunct, " ").
                replaceAll(reSpaces, " ").trim();
    }

    @Override
    public final void normalize(
            final List<String> values,
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
