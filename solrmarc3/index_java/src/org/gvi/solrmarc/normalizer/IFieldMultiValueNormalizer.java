package org.gvi.solrmarc.normalizer;

import java.util.List;

/**
 * Interface defining normalization for all multi value fields.
 *
 * @see de.kobv.k2.ca.normalizer.impl.EditionNormalizer
 * @see de.kobv.k2.ca.normalizer.impl.ISBNNormalizer
 * @see de.kobv.k2.ca.normalizer.impl.ISSNNormalizer
 * @see de.kobv.k2.ca.normalizer.impl.NampartNormalizer
 * @see de.kobv.k2.ca.normalizer.impl.NumpartNormalizer
 * @see de.kobv.k2.ca.normalizer.impl.PagesNormalizer
 * @see de.kobv.k2.ca.normalizer.impl.PlaceNormalizer
 * @see de.kobv.k2.ca.normalizer.impl.PublisherNormalizer
 * @see de.kobv.k2.ca.normalizer.impl.PunctuationMultiNormalizer
 * @see de.kobv.k2.ca.normalizer.impl.RecconnumNormalizer
 * @see de.kobv.k2.ca.normalizer.impl.ScaleNormalizer
 * @see de.kobv.k2.ca.normalizer.impl.SubstringMultiNormalizer
 * @see de.kobv.k2.ca.normalizer.impl.VolumeNormalizer
 * @see de.kobv.k2.ca.normalizer.impl.YearNormalizer
 */
public interface IFieldMultiValueNormalizer {
    /**
     * Normalize all strings from List.
     *
     * @param values     - List of string values.
     * @param normValues - List of normalized string values.
     */
    void normalize(List<String> values, List<String> normValues);
}
