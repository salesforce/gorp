/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.gorp;

import java.util.*;

import com.salesforce.gorp.model.CookedExtraction;

/**
 * Result gotten by matching an input line against extraction rules.
 * If multiple matches would occur, highest matching one (one defined
 * first in the source definition) is used.
 */
public class ExtractionResult
{
    protected final String _id;

    protected final String _input;

    protected final CookedExtraction _matchedExtraction;

    protected final String[] _extractorNames;
    protected final String[] _extractedValues;

    public ExtractionResult(String id, String input, CookedExtraction extr,
            String[] names, String[] values)
    {
        _id = id;
        _input = input;
        _matchedExtraction = extr;
        _extractorNames = names;
        _extractedValues = values;
    }

    public String getId() { return _id; }
    public String getInput() { return _input; }
    public CookedExtraction getMatchedExtraction() { return _matchedExtraction; }

    public Map<String,Object> getExtra() { return _matchedExtraction.getExtra(); }

    /**
     * Method to call to get extracted results (including values to append, if any)
     * as a {link java.util.Map}.
     * Equivalent to calling
     *<pre>
     *    asMap(null);
     *</pre>
     * so that "id" of the matching extraction is not included as a property
     */
    public Map<String,Object> asMap() {
        return asMap(null);
    }
    
    /**
     * Method to call to get extracted results (including values to append, if any)
     * as a {link java.util.Map}.
     * 
     * @param idAs Optional property to use for id of the matched extraction; if null,
     *     name is not added as a property
     */
    public Map<String,Object> asMap(String idAs)
    {
        Map<String,Object> extra = getExtra();
        int size = _extractedValues.length;
        if (extra != null) {
            size += extra.size();
        }
        if (idAs != null) {
            ++size;
        }
        LinkedHashMap<String,Object> result = new LinkedHashMap<>(size);

        // Start with id; add actual matches, append appendables
        if (idAs != null) {
            result.put(idAs, _id);
        }
        for (int i = 0, end = _extractedValues.length; i < end; ++i) {
            result.put(_extractorNames[i], _extractedValues[i]);
        }
        if (extra != null) {
            result.putAll(extra);
        }
        return result;
    }
}
