package org.gvi.solrmarc.normalizer.impl;

import org.gvi.solrmarc.normalizer.IFieldMultiValueNormalizer;

import java.util.List;

/**
 * Normalizer for publisher Marc field (260b).
 */
public class PublisherNormalizer implements IFieldMultiValueNormalizer {
    /**
     * Maximal length of normalized publisher field.
     */
    private static final int PUBLISHER_LENGTH = 5;
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
     * Normalize single value of publisher field.
     * First, remove invisible symbols and text in square brackets.
     * Second, remove punctuation and duplicate spaces.
     * If result is smaller as PUBLISHER_LENGTH, it will be
     * returned. If not, part of result up to PUBLISHER_LENGTH will be returned.
     *
     * @param value single value of publisher Marc field.
     * @return normalized value of publisher Marc field.
     */
    private String normalize(final String value) {
        String str;
        if (value == null) {
            return null;
        }
        //remove brackets
        str = value.replaceAll(reNotVis, "").replaceAll(reBrackets, "");
        //remove punctuation and spaces, take first 5 symbols
        str = str.replaceAll(rePunct, " ").
                replaceAll(reSpaces, " ").trim().toLowerCase();
        if (str.length() <= PUBLISHER_LENGTH) {
            return str;
        }
        else {
            return str.substring(0, PUBLISHER_LENGTH);
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
