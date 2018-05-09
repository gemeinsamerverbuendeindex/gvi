package org.gvi.solrmarc.normalizer;

import java.util.List;

/**
 * Interface defining normalization of multi value
 * fields to single value string.
 *
 * @see de.kobv.k2.ca.normalizer.impl.CoauthorsNormalizer
 */
public interface IFieldMultiToSingleNormalizer {
    /**
     * Normalize list of string values to single string.
     *
     * @param values list of values.
     * @return normalized string value.
     */
    String normalize(List<String> values);
}
