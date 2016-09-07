/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.gorp.model;

import java.util.*;

import com.salesforce.gorp.DefinitionParseException;
import com.salesforce.gorp.io.InputLine;

/**
 * Encapsulation of a definition (pattern, template, extractor) that
 * has only been tokenized, but where named references have not been
 * resolved, nor any escaping/quoting performed.
 */
public class UncookedDefinition
    implements DefPieceContainer
{
    protected final InputLine _source;

    protected final String _name;

    /**
     * Container of possible parameters this definition takes, if any.
     */
    protected final ParameterCollector _parameters;

    /**
     * Offset to where the actual definition starts, after naming and trailing
     * white space.
     */
    protected final int _definitionStart;

    /**
     * Sequence of pieces of this definition instance.
     */
    protected List<DefPiece> _parts = new LinkedList<DefPiece>();

    public UncookedDefinition(InputLine src, String name, boolean hasParameters, int definitionStart) {
        _source = src;
        _name = name;
        _parameters = hasParameters ? new ParameterCollector() : null;
        _definitionStart = definitionStart;
    }

    @Override
    public void append(DefPiece part) {
        _parts.add(part);
    }

    @Override
    public void appendLiteralPattern(String literal, int offset) {
        _parts.add(new LiteralPattern(_source, offset, literal));
    }

    @Override
    public void appendLiteralText(String literal, int offset) {
        _parts.add(new LiteralText(_source, offset, literal));
    }

    @Override
    public void appendPatternRef(String name, int offset) {
        _parts.add(new PatternReference(_source, offset, name));
    }

    @Override
    public TemplateReference appendTemplateRef(String name, int offset) {
        TemplateReference ref = new TemplateReference(_source, offset, name);
        _parts.add(ref);
        return ref;
    }

    @Override
    public void appendTemplateVariable(String parentId, int varPos, int offset) {
        _parts.add(new TemplateParameterReference(_source, offset, parentId, varPos));
    }

    @Override
    public ExtractorExpression appendExtractor(String name, int offset) {
        ExtractorExpression extr = new ExtractorExpression(_source, offset, name);
        _parts.add(extr);
        return extr;
    }

    @Override
    public ExtractorExpression appendVariableExtractor(int varPos, int offset) {
        ExtractorExpression extr = new ExtractorExpression(_source, offset, varPos);
        _parts.add(extr);
        return extr;
    }
    
    @Override
    public String getName() {
        return _name;
    }

    public InputLine getSource() {
        return _source;
    }

    public boolean hasParameters() {
        return _parameters != null;
    }

    public ParameterCollector getParameterCollector() {
        return _parameters;
    }

    public int getDefinitionStart() {
        return _definitionStart;
    }

    public List<DefPiece> getParts() {
        return _parts;
    }

    public void reportError(String template, Object... args) throws DefinitionParseException {
        InputLine source;
        int offset;
        if (_parts.isEmpty()) { // unlikely?
            source = _source;
            offset = 0;
        } else {
            DefPiece p = _parts.get(0);
            source = p.getSource();
            offset = p.getSourceOffset();
        }
        source.reportError(offset, template, args);
    }
}
