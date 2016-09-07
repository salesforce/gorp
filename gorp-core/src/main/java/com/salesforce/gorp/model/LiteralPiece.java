/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.gorp.model;

import com.salesforce.gorp.io.InputLine;

/**
 * Intermediate base class that represents pieces that are leaf components
 * and do not contain any further references that need resolution.
 * This does not necessarily mean that no further processing is needed;
 * typically values contained will still go through various escaping or
 * translation processes.
 */
public class LiteralPiece extends DefPiece
{
    public LiteralPiece(InputLine src, int offset, String lit) {
        super(src, offset, lit);
    }
}
