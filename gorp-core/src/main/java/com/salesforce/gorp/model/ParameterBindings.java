/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.gorp.model;

import java.util.ArrayList;
import java.util.List;

public class ParameterBindings
{
    private final String _types;
    private final List<DefPiece> _bound;

    public ParameterBindings(ParameterDeclarations decls) {
        _types = decls.getTypes();
        _bound = new ArrayList<>();
    }

    public int size() {
        return _types.length();
    }

    public void addBound(DefPiece p) {
        if (_bound.size() >= _types.length()) { // sanity check, should never occur
            throw new IllegalStateException("Trying to add another parameter; already have "
                    +_types.length()+" bound (for types '"+_types+"')");
        }
        _bound.add(p);
    }

    public DefPiece getParameter(int ix) {
        if ((ix < 1) || ix > _bound.size()) {
            return null;
        }
        return _bound.get(ix-1);
    }
}
