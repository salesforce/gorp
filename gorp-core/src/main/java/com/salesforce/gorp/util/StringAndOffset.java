/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.gorp.util;

/**
 * Simple container of a pair of matched String and offset within input
 * for the first character of not-yet-consumed input.
 */
public class StringAndOffset
{
    public final String match;
    public final int restOffset;

    public StringAndOffset(String m, int o) {
        match = m;
        restOffset = o;
    }

    public StringAndOffset withOffset(int o) {
        if (o == restOffset) {
            return this;
        }
        return new StringAndOffset(match, o);
    }
}
