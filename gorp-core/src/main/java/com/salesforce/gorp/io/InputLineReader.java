/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.gorp.io;

import java.io.*;

/**
 * Simple line-oriented reader abstraction that adds following features on top of
 * standard {@link BufferedReader}:
 *<ol>
 * <li>Skips all lines starting with '#' (possibly prefixed with whitespace)
 *  </li>
 * <li>Skips all empty lines (only whitespace)
 *  </li>
 * <li>Keeps track of line numbers (similar to {@link java.io.LineNumberReader}),
 *    for error reporting purposes
 *  </li>
 * <li>Combines multi-line segments (physical lines that end with backslash character)
 *    into single logical lines, represented as {@link InputLine}s.
 *  </li>
 *</ol>
 */
public class InputLineReader
    implements Closeable
{
    protected final Serializable _sourceRef;

    protected final BufferedReader _reader;

    protected final boolean _autoClose;
    
    protected boolean _closed;
    
    /**
     * Row is 1-based, but advanced after reading physical line; hence this
     * always refers to the row that was just read (and 0 before any reads).
     */
    protected int _row = 0;

    protected InputLineReader(Serializable srcRef, BufferedReader r, boolean autoClose) {
        _sourceRef = srcRef;
        _reader = r;
        _autoClose = autoClose;
    }

    public static InputLineReader construct(Serializable srcRef, InputStream in, boolean autoClose) throws IOException {
        return construct(srcRef, new InputStreamReader(in, "UTF-8"), autoClose);
    }

    public static InputLineReader construct(Serializable srcRef, Reader r, boolean autoClose) throws IOException {
        BufferedReader br = (r instanceof BufferedReader)
                ? ((BufferedReader) r)
                        : new BufferedReader(r);
        return new InputLineReader(srcRef, br, autoClose);
    }

    @Override
    public void close() throws IOException {
        if (!_closed) {
            _closed = true;
            _reader.close();
        }
    }

    public InputLine nextLine() throws IOException
    {
        String line = _nextContentLine();
        if (line == null) {
            return null;
        }
        
        int start = _row;
        if (!line.endsWith("\\")) {
            return InputLine.create(_sourceRef, start, line);
        }
        line = line.substring(0, line.length() - 1);
        InputLine combo = InputLine.create(_sourceRef, start, line);

        while (true) {
            // NOTE: with continuations we are NOT to skip empty lines or comments!
            line = _nextContinuationLine();
            // Illegal to end with continuation
            if (line == null)  {
            	reportError("Unexpected end-of-input when expecting line continuation'");
            }
            if (!line.endsWith("\\")) {
                return combo.appendSegment(line);
            }
            line = line.substring(0, line.length() - 1);
            combo = combo.appendSegment(line);
        }
    }

    public void reportError(String template, Object... args) throws IOException {
        String msg = (args.length == 0) ? template
    			: String.format(template, args);
        throw new IOException(String.format("(%s, row %d): %s", _sourceRef, _row, msg));
    }
    
    protected String _nextContentLine() throws IOException
    {
        if (_closed) {
            return null;
        }
        while (true) {
            String line = _reader.readLine();
            if (line == null) {
                if (_autoClose) {
                    close();
                }
                return line;
            }
            ++_row;
            if (!_isEmptyOrComment(line)) {
                return line;
            }
        }
    }

    protected String _nextContinuationLine() throws IOException
    {
        if (_closed) {
            return null;
        }
        String line = _reader.readLine();
        if (line == null) {
            if (_autoClose) {
                close();
            }
            return line;
        }
        ++_row;
        return line;
    }

    protected boolean _isEmptyOrComment(String line) {
        for (int i = 0, end = line.length(); i < end; ++i) {
            int ch = line.charAt(i);
            if (ch <= 0x0020) { // skip whitespace
                continue;
            }
            return (ch == '#');
        }
        // must be whitespace if we got this far
        return true;
    }
}
