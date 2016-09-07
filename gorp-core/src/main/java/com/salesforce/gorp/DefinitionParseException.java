/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.gorp;

import java.io.IOException;

import com.salesforce.gorp.io.InputLine;

/**
 * Exception type used to indicate problems during reading of
 * a definition, using {@link DefinitionReader}.
 */
public class DefinitionParseException extends IOException
{
    private static final long serialVersionUID = 1L;

    protected final InputLine _source;
    protected final int _sourceOffset;
    protected final String _sourceDesc;

    public DefinitionParseException(String msg, InputLine src, int srcOffset,
            String srcDesc) {
        super(msg);
        _source = src;
        _sourceOffset = srcOffset;
        _sourceDesc = srcDesc;
    }

    public static DefinitionParseException construct(String msg, InputLine src, int srcOffset)
    {
        String srcDesc = (src == null) ? "N/A": src.constructDesc(srcOffset);
        return new DefinitionParseException(String.format("(%s): %s", srcDesc, msg),
                src, srcOffset, srcDesc);
    }

    public InputLine getSource() { return _source; }
    public int getSourceOffset() { return _sourceOffset; }

    /**
     * Helper method that may be used to get a developer-readable description
     * of the source location for the problem that cause this exception to be
     * thrown, if such location is known.
     */
    public String getSourceDesc() {
        return _sourceDesc;
    }        
}
