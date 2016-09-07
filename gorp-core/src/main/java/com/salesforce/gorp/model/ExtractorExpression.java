/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.gorp.model;

import java.util.*;

import com.salesforce.gorp.io.InputLine;

/**
 * Similar to other {@link DefPiece}s, expect that it is structured and besides
 * name (of variable/property to extract value for) also contains nested sequence
 * of pieces (possibly including other extractors, but more commonly patterns,
 * pattern references; theoretically also template references).
 */
public class ExtractorExpression
    extends DefPiece
    implements DefPieceContainer, DefPieceAppendable
{
    private List<DefPiece> _parts;

    /**
     * For "variable" extractors, logical position of the parameter that will
     * specify actual name; 1-based
     */
    private final int _variablePos;

    public ExtractorExpression(InputLine src, int offset, String lit) {
        super(src, offset, lit);
        _parts = new ArrayList<>();
        _variablePos = -1;
    }

    public ExtractorExpression(InputLine src, int offset, int variablePos) {
        super(src, offset, String.valueOf(variablePos));
        _parts = new ArrayList<>();
        _variablePos = variablePos;
    }

    protected ExtractorExpression(ExtractorExpression base, List<DefPiece> newParts) {
        super(base);
        _parts = newParts;
        _variablePos = base._variablePos;
    }

    protected ExtractorExpression(ExtractorExpression base, String newName) {
        super(base, newName);
        _parts = base._parts;
        _variablePos = -1;
    }

    public ExtractorExpression withName(String name) {
        return new ExtractorExpression(this, name);
    }
    
    public ExtractorExpression withParts(List<DefPiece> newParts) {
        return new ExtractorExpression(this, newParts);
    }

    public ExtractorExpression empty() {
        return new ExtractorExpression(this, new ArrayList<DefPiece>());
    }

    @Override
    public String getName() {
        return getText();
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
    
    // Also DefPieceAppendable during resolution
    @Override
    public void append(DefPiece part) {
        _parts.add(part);
    }

    @Override
    public Iterable<DefPiece> getParts() {
        return _parts;
    }

    public boolean isPositional() {
        return _variablePos >= 0; // -1 is the "not positional" marker
    }

    public int getPosition() {
        return _variablePos;
    }
}
