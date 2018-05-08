package org.gvi.solrmarc.normalizer.impl;

import org.gvi.solrmarc.normalizer.IFieldMultiValueNormalizer;

import java.util.List;

/**
 * Normalizer for ISMN field.
 */
public class ISMNNormalizer implements IFieldMultiValueNormalizer {
    /**
     * Normalize all values of field.
     * @param values     - List of string values.
     * @param normValues - List of normalized string values.
     */
    @Override
    public void normalize(List<String> values, List<String> normValues) {
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

    /**
     * Normalize single value of ISMN.
     * Remove invisible symbols and delimiters,
     * and try to find sequence, representing ISMN.
     * If sequence found, return it as String.
     * If sequence is not found, return null.
     *
     * @param value single value of ISMN field.
     * @return ISMN or null.
     */
    private String normalize(final String value) {
        if (value == null) {
            return null;
        }
        String reFilter = "9790[0-9]+";
        String reFilterm = "m[0-9]+";
        String reBinder = "-+";
        String reNotVis = "[\u0000-\u001F\u007F-\u009F]";
        String reSpaces = "\\s+";
        String ismn = null;
        String str = value.replaceAll(reNotVis, "").toLowerCase().
                replaceAll(reBinder, "").
                replaceAll(reSpaces, "");
        try {
            if (str.matches(reFilter)) {  //it is ISMN starting from 9790
                ismn = str.substring(4, str.length() - 1);
                return ismn;
            }
            else {
                if (str.matches(reFilterm)) {//it is ISMN starting from m
                    ismn = str.substring(1, str.length() - 1);
                    return ismn;
                }
                else {
                    return null;
                }
            }
        }
        catch (Exception ex) {
            return null;
        }
    }
}
