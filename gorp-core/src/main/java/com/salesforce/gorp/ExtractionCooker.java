package com.salesforce.gorp;

import com.salesforce.gorp.model.CookedExtraction;
import com.salesforce.gorp.model.FlattenedExtraction;

/**
 * Simple factory used by {@link com.salesforce.gorp.Gorp} to convert
 * {@link com.salesforce.gorp.model.FlattenedExtraction}s into
 * {@link com.salesforce.gorp.model.CookedExtraction}s; latter needed for
 * actual extraction of template values from input.
 * This factory interface allows for plugging alternate backends based on
 * regexp packages other than one provided by JDK (<code>java.util.regex</code>).
 * 
 * @see com.salesforce.gorp.jdkre.JDKRegexpExtractionCooker
 */
public abstract class ExtractionCooker
{
    /**
     * Method for actual conversion from "raw" but non-nested {@link FlattenedExtraction}
     * into fully processed {@link CookedExtraction} that input is pattern-matched against.
     */
    public abstract CookedExtraction cook(int index, String regexpSource,
            FlattenedExtraction extr);

    public abstract void appendPattern(String pattern, StringBuilder buffer);
    public abstract void appendLiteral(String literal, StringBuilder buffer);

    public abstract void appendStartExpression(StringBuilder buffer);
    public abstract void appendFinishExpression(StringBuilder buffer);
}
