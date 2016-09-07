/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.gorp.model;

import com.salesforce.gorp.io.InputLine;

public class LiteralText extends LiteralPiece
{
    protected LiteralText(InputLine src, int offset, String lit) {
        super(src, offset, lit);
        // sanity check, to avoid adding unnecessary empty Literals
        if (lit.isEmpty()) {
            throw new IllegalArgumentException("Internal error: trying to append empty LiteralText");
        }
    }
}
