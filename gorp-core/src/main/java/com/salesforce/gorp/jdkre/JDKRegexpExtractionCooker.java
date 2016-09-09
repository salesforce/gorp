package com.salesforce.gorp.jdkre;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.salesforce.gorp.ExtractionCooker;
import com.salesforce.gorp.model.CookedExtraction;
import com.salesforce.gorp.model.FlattenedExtraction;
import com.salesforce.gorp.util.RegexHelper;

public class JDKRegexpExtractionCooker extends ExtractionCooker
{
    private final static JDKRegexpExtractionCooker INSTANCE = new JDKRegexpExtractionCooker();

    public static JDKRegexpExtractionCooker instance() {
        return INSTANCE;
    }

    @Override
    public CookedExtraction cook(int index, String regexpSource, FlattenedExtraction extr)
        throws PatternSyntaxException
    {
        Pattern regexp = Pattern.compile(regexpSource);
        return JDKRegexpCookedExtraction.construct(index, extr, regexp, regexpSource,
                extr.getExtractorNames());
    }

    public void appendPattern(String pattern, StringBuilder buffer) {
        RegexHelper.massageRegexpForJDK(pattern, buffer);
    }

    public void appendLiteral(String literal, StringBuilder buffer) {
        RegexHelper.quoteLiteralAsRegexp(literal, buffer);
    }

    public void appendStartExpression(StringBuilder buffer) {
        buffer.append('(');
    }

    public void appendFinishExpression(StringBuilder buffer) {
        buffer.append(')');
    }
}
