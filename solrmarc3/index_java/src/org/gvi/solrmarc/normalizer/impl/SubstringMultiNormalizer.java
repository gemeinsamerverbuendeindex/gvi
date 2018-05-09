package org.gvi.solrmarc.normalizer.impl;

import org.gvi.solrmarc.normalizer.IFieldMultiValueNormalizer;

import java.util.List;

/**
 * Normalizer for multi value text fields where only defined part
 * of text defines normalized value.
 */
public class SubstringMultiNormalizer implements IFieldMultiValueNormalizer {

    /**
     * Position of first included symbol from string value.
     */
    private int start = 0;
    /**
     * Position of first not included symbol in string value.
     */
    private int end = 0;
    /**
     * Default normalized value used if index values are out of bounds.
     */
    private String defaultValue = "tu";

    /**
     * Get default value for normalized values if they are empty.
     *
     * @return default value of normalized field.
     */
    public final String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Set default value for normalized values if they are empty.
     *
     * @param newDefaultValue new default value of normalized field.
     */
    public final void setDefaultValue(final String newDefaultValue) {
        this.defaultValue = newDefaultValue;
    }

    /**
     * Get position of first included symbol from string value by normalisation.
     *
     * @return position of first included symbol from string value
     * by normalisation.
     */
    public final int getStart() {
        return start;
    }

    /**
     * Set position of first included symbol from string value by normalisation.
     *
     * @param newStart new position of first included symbol from string
     *                 value by normalisation.
     */
    public final void setStart(final int newStart) {
        this.start = newStart;
    }

    /**
     * Get position of first not included symbol in string value.
     *
     * @return position of first not included symbol in string value.
     */
    public final int getEnd() {
        return end;
    }

    /**
     * Set position of first not included symbol in string value.
     *
     * @param newEnd new position of first not included symbol in string value.
     */
    public final void setEnd(final int newEnd) {
        this.end = newEnd;
    }

    /**
     * Normalize single string value.
     *
     * @param value string for normalization.
     * @return part of string or default value, if exception happens.
     */
    private String normalize(final String value) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return value.substring(start, end).toLowerCase();
        }
        catch (IndexOutOfBoundsException bex) {
            return defaultValue;
        }
    }

    @Override
    public final void normalize(
            final List<String> values, final List<String> normValues) {
        String vol;
        normValues.clear();
        if (values == null || values.isEmpty()) {
            normValues.add(defaultValue);
            return;
        }
        for (String s : values) {
            vol = normalize(s);
            if (vol != null && !vol.isEmpty()) {
                normValues.add(vol);
            }
        }
    }
}
