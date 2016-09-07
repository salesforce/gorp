/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.gorp.model;

import com.salesforce.gorp.io.InputLine;

/**
 * Piece used to represent a reference to template parameter: name of an included
 * template, passed as a positional argument to parameterized template.
 */
public class TemplateParameterReference extends DefPiece {
    private final String _parentId;
    private final int _position;

    public TemplateParameterReference(InputLine src, int offset, String parentId, int pos) {
        super(src, offset, String.valueOf(pos));
        _parentId = parentId;
        _position = pos;
    }

    public String getParentId() { return _parentId; }
    public int getPosition() { return _position; }
}
