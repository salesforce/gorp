/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.gorp.model;

import java.util.Map;

import com.salesforce.gorp.ExtractionResult;
import com.salesforce.gorp.io.InputLine;

/**
 * Base API for "cooked" (fully processed) extraction rule, to be implemented by
 * implementation classes using concrete regular expression packages.
 */
public abstract class CookedExtraction
{
    protected final InputLine _source;
    protected final String _name;
    protected final Map<String,Object> _append;

    protected final String _regexpSource;
    protected final String[] _extractorNames;

    protected CookedExtraction(InputLine source, String name,
            int index, Map<String,Object> append,
            String regexpSource, String[] extractorNames)
    {
        _source = source;
        _name = name;
        _append = append;
        _regexpSource = regexpSource;
        _extractorNames = extractorNames;
    }

    public String getName() {
        return _name;
    }

    public Map<String,Object> getExtra() {
        return _append;
    }

    public String getRegexpSource() {
        return _regexpSource;
    }

    /**
     * Helper method called by <code>Gorp</code> to construct actual results, given
     * that match has occurred. Needs to weave 
     */
    public ExtractionResult constructMatch(String input, String[] values)
    {
        return new ExtractionResult(getName(), input, this, _extractorNames, values);
    }

    // // // // Abstract methods for sub-classes to implement

    public abstract ExtractionResult match(String input);

    public abstract Object getRegexp();
    
    public abstract String getRegexpDesc();
}
