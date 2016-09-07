/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.gorp.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.salesforce.gorp.io.InputLine;

public class TemplateReference extends DefPiece
    implements DefPieceAppendable
{
    protected List<DefPiece> _parameters;

    public TemplateReference(InputLine src, int offset, String lit) {
        super(src, offset, lit);
    }

    protected TemplateReference(TemplateReference base, List<DefPiece> p) {
        super(base);
        _parameters = p;
    }
    
    /**
     * "Mutant factory" method for creating a new instance, but with a different
     * set of parameters. This is used during binding of parameters.
     */
    public TemplateReference withParameters(List<DefPiece> params) {
        return new TemplateReference(this, params);
    }

    public boolean takesParameters() {
        return _parameters != null;
    }

    public List<DefPiece> getParameters() {
        return _parameters;
    }

    // // // DefPieceAppendable
    
    @Override
    public String getName() {
        return getText();
    }

    @Override
    public Iterable<DefPiece> getParts() {
        if (_parameters == null) {
            return Collections.emptyList();
        }
        return _parameters;
    }

    @Override
    public void append(DefPiece part) {
        if (_parameters == null) {
            _parameters = new ArrayList<>();
        }
        _parameters.add(part);
    }
}
