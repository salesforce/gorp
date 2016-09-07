/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.gorp.io;

import java.io.Serializable;
import java.util.Arrays;

import com.salesforce.gorp.DefinitionParseException;

/**
 * Simple abstract over line-oriented input where a single logical line may come
 * from multiple physical lines, combined by lines ending with a backslash
 */
public class InputLine
    implements Serializable // just to serialize parse exception
{
    private static final long serialVersionUID = 1L;

    // transient since we can't guarantee it is actually serializable
    protected final Serializable _sourceRef;

    /**
     * Row number of the first physical line of this logical input line
     */
    protected final int _startRow;

    protected final String _input;

    /**
     * Offsets of physical lines within {@link #_input}, not including the
     * first line that has offset of 0. Used to calculate actual column position
     * within physical line (as well as row) from offset in logical line.
     */
    protected final int[] _offsets;
    
    protected InputLine(Serializable ref, int row, String input, int[] offsets) {
        _sourceRef = ref;
        _startRow = row;
        _input = input;
        _offsets = offsets;
    }

    /**
     * Factory method for constructing a single, non-joint input line.
     */
    public static InputLine create(Serializable ref, int row, String input) {
        return new InputLine(ref, row, input, null);
    }

    /**
     * Factory method used when we have at least one continuation line.
     */
    public static InputLine create(Serializable ref, int row, String input1, String input2) {
        int[] offsets = new int[1];
        offsets[0] = input1.length();
        return new InputLine(ref, row, input1 + input2, offsets);
    }

    /**
     * Mutant factory method used for appending another line segment, creating
     * and returning resulting segment instance.
     */
    public InputLine appendSegment(String segment) {
        // should we even allow this?
        if (_offsets == null) {
            return create(_sourceRef, _startRow, _input, segment);
        }
        int olen = _offsets.length;
        int[] offsets = Arrays.copyOf(_offsets, olen + 1);
        offsets[olen] = _input.length();
        return new InputLine(_sourceRef, _startRow, _input + segment, offsets);
    }

    public int getStartRow() {
        return _startRow;
    }

    public int rowCount() {
        if (_offsets == null) {
            return 1;
        }
        return 1 + _offsets.length;
    }

    public String getContents() {
        return _input;
    }

    public <T> T reportError(int physicalOffset, String template, Object... args) throws DefinitionParseException {
        String msg = (args.length == 0) ? template : String.format(template, args);
        throw DefinitionParseException.construct(msg, this, physicalOffset);
    }

    /**
     * @param physicalColumnOffset Offset within logical input line, 0-based
     */
    public String constructDesc(int physicalColumnOffset) {
        // single line? Easy
        int row = _startRow;
        int column = physicalColumnOffset;
        if (_offsets != null) {
            int base = 0;
            // if multiline, find position within original physical input line
            for (int i = 0, len = _offsets.length; i < len; ++i) {
                if (column < _offsets[i]) {
                    break;
                }
                ++row;
                base = _offsets[i];
            }
            column -= base;
        }
        return String.format("[%s (%d,%d)]",
                String.valueOf(_sourceRef), row, column+1);
    }
}
