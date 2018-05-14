package org.gvi.solrmarc.normalizer;

/**
 * Interface defining normalization for all single value fields.
 *
 * @see de.kobv.k2.ca.normalizer.impl.SubstringNormalizer
 * @see de.kobv.k2.ca.normalizer.impl.PunctuationSingleNormalizer
 * @see de.kobv.k2.ca.normalizer.impl.BracketsPunctSingleNormalizer
 */
public interface IFieldSingleValueNormalizer {
    /**
     * Normalize string value.
     *
     * @param value string for normalization.
     * @return normalized value.
     */
    String normalize(String value);
}
