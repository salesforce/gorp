/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.gorp.model;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.salesforce.gorp.ExtractionResult;
import com.salesforce.gorp.io.InputLine;

public class CookedExtraction
{
    protected final InputLine _source;
    protected final String _name;
    protected final Map<String,Object> _append;

    protected final Pattern _regexp;
    protected final String _regexpSource;
    protected final String[] _extractorNames;

    protected CookedExtraction(InputLine source, String name,
            int index, Map<String,Object> append,
            Pattern regexp, String regexpSource, String[] extractorNames)
    {
        _source = source;
        _name = name;
        _append = append;
        _regexp = regexp;
        _regexpSource = regexpSource;
        _extractorNames = extractorNames;
    }

    public static CookedExtraction construct(int index, FlattenedExtraction src,
            Pattern regexp, String regexpSource, List<String> extractorNamesList)
    {
        String[] extrNames = extractorNamesList.toArray(new String[extractorNamesList.size()]);
        return new CookedExtraction(src._source, src._name,
                index, src._append,
                regexp, regexpSource, extrNames);
    }

    public String getName() {
        return _name;
    }

    public Map<String,Object> getExtra() {
        return _append;
    }

    public Pattern getRegexp() {
        return _regexp;
    }

    public String getRegexpSource() {
        return _regexpSource;
    }

    public String getRegexpDesc() {
        return _regexp.pattern();
    }

    /**
     * Helper method called by <code>Gorp</code> to construct actual results, given
     * that match has occurred. Needs to weave 
     */
    public ExtractionResult constructMatch(String input, String[] values)
    {
        return new ExtractionResult(getName(), input, this, _extractorNames, values);
    }
}
