/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.gorp.model;

import java.util.*;

import com.salesforce.gorp.io.InputLine;

public class CookedTemplate
    implements DefPieceAppendable
{
    protected final InputLine _source;
    protected final int _sourceOffset;

    protected final String _name;
    protected final List<DefPiece> _parts;

    protected final ParameterDeclarations _paramDecls;

    protected CookedTemplate(InputLine src, int srcOffset,
            String name, List<DefPiece> parts, ParameterDeclarations params)
    {
        _source = src;
        _sourceOffset = srcOffset;
        _name = name;
        _parts = parts;
        _paramDecls = params;
    }

    public static CookedTemplate construct(UncookedDefinition uncooked) {
        List<DefPiece> uncookedParts = uncooked.getParts();
        InputLine source;
        int offset;
        if (uncookedParts.isEmpty()) {
            source = uncooked.getSource();
            offset = 0;
        } else {
            DefPiece p = uncookedParts.get(0);
            source = p.getSource();
            offset = p.getSourceOffset();
        }
        // Parameterization?
        ParameterCollector paramDefs = uncooked.getParameterCollector();
        ParameterDeclarations params = (paramDefs == null) ? null
                : paramDefs.constructDeclarations();

        // could get offset of the first piece, which points to name. But for now let's not bother
        return new CookedTemplate(source, offset, uncooked.getName(),
                new ArrayList<DefPiece>(Math.min(4, uncookedParts.size())),
                params);
    }

    @Override
    public void append(DefPiece part) {
        _parts.add(part);
    }

    @Override
    public String getName() { return _name; }

    @Override
    public Iterable<DefPiece> getParts() { return _parts; }

    public InputLine getSource() { return _source; }

    public boolean hasParameters() {
        return (_paramDecls != null);
    }

    public ParameterDeclarations getParameterDeclarations() {
        return _paramDecls;
    }
}
