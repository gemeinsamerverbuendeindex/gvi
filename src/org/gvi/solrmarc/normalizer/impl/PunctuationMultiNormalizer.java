package org.gvi.solrmarc.normalizer.impl;

import org.gvi.solrmarc.normalizer.IFieldMultiValueNormalizer;

import java.util.List;

/**
 * Normalizer for multi value fields, remove  not visible symbols,
 * punctuation and duplicate spaces from text.
 */
public class PunctuationMultiNormalizer extends PunctuationSingleNormalizer
        implements IFieldMultiValueNormalizer {

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
