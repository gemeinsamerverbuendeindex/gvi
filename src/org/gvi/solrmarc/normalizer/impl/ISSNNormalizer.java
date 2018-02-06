package org.gvi.solrmarc.normalizer.impl;

import org.gvi.solrmarc.normalizer.IFieldMultiValueNormalizer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizer for ISSN Marc field (022a).
 */
public class ISSNNormalizer implements IFieldMultiValueNormalizer {
    /**
     * Length of correct ISSN value.
     */
    private static final int ISSN_LENGTH = 8;
    /**
     * Regular expression to remove duplicate spaces.
     */
    private String reSpaces = "\\s+";
    /**
     * Regular expression to replace all "o" characters on the "0" character.
     */
    private String reO = "o";
    /**
     * Regular expression to remove invisible UTF-8 symbols.
     */
    private String reNotVis = "[\u0000-\u001F\u007F-\u009F]";

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

    /**
     * Normalize single value of ISSN Marc field. Check,
     * if ISSN correct and return ISSN value as String.
     * If value is not correct ISSN, return null.
     *
     * @param value single value of ISSN Marc field..
     * @return normalized value.
     */
    private String normalize(final String value) {
        if (value == null) {
            return null;
        }
        String reFilter = ".*[0-9]+x*.*";
        String reBinder = "-+";
        Pattern p = Pattern.compile("[0-9]+x*");
        Matcher m;
        String issn = null;
        int len = 0;
        String str = value.replaceAll(reNotVis, "").toLowerCase().
                replaceAll(reBinder, "").
                replaceAll(reO, "0").replaceAll(reSpaces, "");
        try {
            if (str.matches(reFilter)) {  //it is like ISSN
                m = p.matcher(str);
                m.reset();
                while (m.find()) {
                    issn = m.group();
                    len = issn.length();
                    if (len == ISSN_LENGTH) {
                        return issn;
                    }
                }
            }
            else { //not ISSN
                return null;
            }
        }
        catch (Exception ex) {
            return null;
        }
        return null;
    }
}
