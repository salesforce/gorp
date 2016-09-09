package com.salesforce.gorp.jdkre;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.salesforce.gorp.ExtractionResult;
import com.salesforce.gorp.io.InputLine;
import com.salesforce.gorp.model.CookedExtraction;
import com.salesforce.gorp.model.FlattenedExtraction;

public class JDKRegexpCookedExtraction
    extends CookedExtraction
{
    protected final Pattern _regexp;

    protected JDKRegexpCookedExtraction(InputLine source, String name,
            int index, Map<String,Object> append,
            Pattern regexp, String regexpSource, String[] extractorNames)
    {
        super(source, name, index, append, regexpSource, extractorNames);
        _regexp = regexp;
    }

    public static JDKRegexpCookedExtraction construct(int index, FlattenedExtraction src,
            Pattern regexp, String regexpSource, List<String> extractorNamesList)
    {
        String[] extrNames = extractorNamesList.toArray(new String[extractorNamesList.size()]);
        return new JDKRegexpCookedExtraction(src.getSource(), src.getName(),
                index, src.getAppends(),
                regexp, regexpSource, extrNames);
    }

    @Override
    public ExtractionResult match(String input) {
        Matcher m = _regexp.matcher(input);
        return m.matches() ? _constructMatch(input, m) : null;
    }

    @Override
    public Pattern getRegexp() {
        return _regexp;
    }

    @Override
    public String getRegexpDesc() {
        return _regexp.pattern();
    }

    protected ExtractionResult _constructMatch(String input, Matcher m)
    {
        final int count = m.groupCount();
        String[] values = new String[count];
        for (int i = 0; i < count; ++i) {
            values[i] = m.group(i+1);
        }
        return constructMatch(input, values);
    }
}
