package org.gvi.solrmarc.normalizer.impl;

import org.gvi.solrmarc.normalizer.IFieldSingleValueNormalizer;

/**
 * Normalizer for text fields where only defined part of text defines normalized value.
 */
public class SubstringNormalizer implements IFieldSingleValueNormalizer {
    /**
     * Position of first included symbol from string value.
     */
    private int start = 0;
    /**
     * Position of first not included symbol in string value.
     */
    private int end = 0;

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
     * Normalize string value.
     *
     * @param value string for normalization.
     * @return - part of string defined via newStart and end values.
     */
    @Override
    public final String normalize(final String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return value.substring(start, end).toLowerCase();
        }
        catch (IndexOutOfBoundsException bex) {
            return null;
        }
    }
}
