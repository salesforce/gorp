/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.gorp.model;

import java.util.Map;

import com.salesforce.gorp.io.InputLine;

/**
 * Definition of a single extraction, right after tokenization, but before
 * resolution of included pattern and template references.
 */
public class UncookedExtraction
{
    protected final InputLine _source;
    protected final String _name;
    protected final UncookedDefinition _template;
    protected final Map<String,Object> _append;
    
    public UncookedExtraction(InputLine source, String name, UncookedDefinition t,
            Map<String,Object> append)
    {
        _source = source;
        _name = name;
        _template = t;
        _append = append;
    }

    public InputLine getSource() {
        return _source;
    }

    public String getName() {
        return _name;
    }

    public UncookedDefinition getTemplate() {
        return _template;
    }

    public Map<String,Object> getAppends() {
        return _append;
    }
}
