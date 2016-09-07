/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.gorp;

import java.io.IOException;

/**
 * Exception used to indicate a problems occuring during
 * extraction process (by {@link Gorp}.
 */
public class ExtractionException extends IOException
{
    private static final long serialVersionUID = 1L;

    protected final String _input;

    public ExtractionException(String input, String msg) {
        super(msg);
        _input = input;
    }
    
    public ExtractionException(String input, String msg, Exception e) {
        super(msg, e);
        _input = input;
    }

    public String getInput() {
        return _input;
    }
}
