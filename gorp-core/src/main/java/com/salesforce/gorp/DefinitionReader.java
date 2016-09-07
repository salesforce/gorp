/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.gorp;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.jr.ob.JSON;
import com.salesforce.gorp.io.InputLine;
import com.salesforce.gorp.io.InputLineReader;
import com.salesforce.gorp.model.*;
import com.salesforce.gorp.util.StringAndOffset;
import com.salesforce.gorp.util.TokenHelper;

/**
 * Main orchestrator of reading of Extractor definitions; mostly delegates processing
 * to other entities (like {@link InputLineReader} which handles physical-to-logical lines
 * concatenation), only directly handling basic detection of main-level line types during
 * creation of "uncooked" input lines.
 */
public class DefinitionReader
{
    private final static String KNOWN_KEYWORDS =
            "(pattern, template, extract)"
            ;

    private final static String EXTRACTOR_PROPERTIES =
            "(template, append)"
            ;

    /**
     * We use Jackson-jr for simple deserialization of 'append' properties
     */
    private final static JSON _json = JSON.std;
    
    protected final InputLineReader _lineReader;

    protected UncookedDefinitions _uncooked;

    protected final CookedDefinitions _cooked;

    protected DefinitionReader(InputLineReader lineReader) {
        _lineReader = lineReader;
        _cooked = new CookedDefinitions();
    }

    public static DefinitionReader reader(File input) throws IOException
    {
        InputStream in = new FileInputStream(input);
        String srcRef = "file '"+input.getAbsolutePath()+"'";
        return reader(InputLineReader.construct(srcRef, in, true));
    }

    public static DefinitionReader reader(String contents) throws IOException
    {
        Reader r = new StringReader(contents);
        String srcRef = "<input string>";
        return reader(InputLineReader.construct(srcRef, r, true));
    }

    public static DefinitionReader reader(InputLineReader lines) throws IOException {
        return new DefinitionReader(lines);
    }

    /**
     * Method used to fully read the input definition, resolve all included
     * patterns, templates, extractors and extractions, and construct and
     * return resulting {@link Gorp}.
     */
    public Gorp read() throws IOException {
        readUncooked();

        if (_uncooked.getExtractions().isEmpty()) {
            // We don't have InputLine (necessarily) to indicate, but do want to use
            // DefinitionParseException, so need to do:
            throw DefinitionParseException.construct("No extraction definitions found from definition",
                    null, 0);
        }
        return resolveAll();
    }

    /*
    /**********************************************************************
    /* Test support
    /**********************************************************************
     */

    Gorp resolveAll() throws DefinitionParseException {
        resolvePatterns();
        resolveTemplates();
        resolveExtractions();
        return buildExtractor();
    }

    void resolvePatterns() throws DefinitionParseException {
        _cooked.resolvePatterns(_uncooked);
    }

    void resolveTemplates() throws DefinitionParseException {
        _cooked.resolveTemplates(_uncooked);
    }

    void resolveExtractions() throws DefinitionParseException {
        _cooked.resolveExtractions(_uncooked);
    }

    Gorp buildExtractor() throws DefinitionParseException {
        return Gorp.construct(_cooked);
    }

    /*
    /**********************************************************************
    /* High-level flow
    /**********************************************************************
     */

    /**
     * First part of processing, reading of contents of extraction definition
     * in "uncooked" form, which does basic tokenization but does not resolve
     * any of named references
     */
    public void readUncooked() throws IOException, DefinitionParseException
    {
        // lazily instantiate just to avoid problems if called more than once
        if (_uncooked != null) {
            return;
        }
        _uncooked = new UncookedDefinitions();

        // 1. Read all input in mostly unprocessed form
        InputLine line;
        while ((line = _lineReader.nextLine()) != null) {
            final String contents = line.getContents();
            StringAndOffset p = TokenHelper.findKeyword(contents, 0);
            if (p == null) {
                line.reportError(0, "No keyword found from line; expected one of %s", KNOWN_KEYWORDS);
            }

            final String keyword = p.match;
            switch (keyword) {
            case "pattern":
                _readPatternDefinition(line, p.restOffset);
                break;
            case "template":
                _readTemplateDefinition(line, p.restOffset);
                break;
            case "extract":
                _readExtractionDefinition(line, p.restOffset);
                break;
            default:
                line.reportError(0, "Unrecognized keyword \"%s\" encountered; expected one of %s",
                        keyword, KNOWN_KEYWORDS);
            }
        }

        // 2. With knowledge of existence (or not) of params, tokenize, as pre-cooking step

        // 2a: tokenize patterns
        for (UncookedDefinition pattern : _uncooked.getPatterns().values()) {
            _tokenizePatternDefinition(pattern);
        }

        // 2b: tokenize templates
        for (UncookedDefinition template : _uncooked.getTemplates().values()) {
            _tokenizeTemplateContents(template.getSource(), template.getDefinitionStart(),
                    _uncooked, template, -1,
                    "template '"+template.getName()+"' definition", template.getParameterCollector());
        }

        // 2c: tokenize extraction templates
        for (UncookedExtraction xtr : _uncooked.getExtractions().values()) {
            UncookedDefinition template = xtr.getTemplate();
            _tokenizeTemplateContents(template.getSource(), template.getDefinitionStart(),
                    _uncooked, template, 0,
                    "extraction template for '"+template.getName()+"'", null);
        }
    }

    /*
    /**********************************************************************
    /* Per-declaration-type parsing
    /**********************************************************************
     */

    private void _readPatternDefinition(InputLine line, int offset) throws DefinitionParseException
    {
        final String contents = line.getContents();
        int ix = TokenHelper.findTypeMarker('%', contents, offset);
        if (ix < 0) {
            line.reportError(offset, "Pattern name must be prefixed with '%'");
        }
        offset = ix+1;// to skip percent marker
        // then read name, do require white space after, to be skipped
        StringAndOffset p = TokenHelper.parseNameAndSkipSpace("pattern", line, contents, offset);
        String name = p.match;

        // First, verify this is not dup
        UncookedDefinition unp = new UncookedDefinition(line, name, false, p.restOffset);
        UncookedDefinition old = _uncooked.addPattern(name, unp);
        if (old != null) {
            line.reportError(offset, "Duplicate pattern definition for name '%s'", name);
        }
    }

    private void _tokenizePatternDefinition(UncookedDefinition unp) throws DefinitionParseException
    {
        final InputLine line = unp.getSource();
        final String contents = line.getContents();
        // And then need to find cross-refs
        // First a quick and cheesy check for common case of no expansions
        final int end = contents.length();
        int offset = unp.getDefinitionStart();
        int ix = contents.indexOf('%', offset);
        if (ix < 0) {
            unp.appendLiteralPattern(contents.substring(offset), offset);
            return;
        }
        StringBuilder sb = new StringBuilder();
        if (ix > 0) {
            sb.append(contents.substring(offset, ix));
        }
        int literalStart = offset;
        while (ix < end) {
            char c = contents.charAt(ix++);
            if (c != '%') {
                sb.append(c);
                continue;
            }
            if (ix == end) {
                line.reportError(ix, "Orphan '%%' at end of pattern '%s' definition", unp.getName());
            }
            c = contents.charAt(ix);
            if (c == '%') {
                sb.append(c);
                ++ix;
                continue;
            }
            StringAndOffset ref = TokenHelper.parseName("pattern", line, contents, ix, false);
            // Re-calc where we continue from etc
            String refName = ref.match;

            if (sb.length() > 0) {
                unp.appendLiteralPattern(sb.toString(), literalStart);
                sb.setLength(0);
            }
            unp.appendPatternRef(refName, ix);
            ix = ref.restOffset;
            literalStart = offset;
        }

        if (sb.length() > 0) {
            unp.appendLiteralPattern(sb.toString(), literalStart);
        }
    }

    private void _readTemplateDefinition(InputLine line, int startOffset) throws DefinitionParseException
    {
        final String contents = line.getContents();
        int ix = TokenHelper.findTypeMarker('@', contents, startOffset);
        if (ix < 0) {
            line.reportError(startOffset, "Template name must be prefixed with '@'");
        }
        ix += 1;
        StringAndOffset p = TokenHelper.parseName("template", line, contents, ix, false);
        String name = p.match;
        final int nameOffset = ix;
        ix = p.restOffset;

        // See if this is parameterized template (with parameter/argument, placeholders)
        int ix2 = TokenHelper.skipEmptyParens(contents, ix);
        boolean hasParams;
        if (ix2 > ix) {
            ix = ix2;
            hasParams = true;
        } else {
            hasParams = false;
        }
        ix2 = TokenHelper.skipSpace(contents, ix);
        if (ix == ix2) {
            line.reportError(ix, "Missing space character after template name '%s'", name);
        }
        ix = ix2;

        // Then verify this is not dup
        UncookedDefinition unp = new UncookedDefinition(line, name, hasParams, ix);
        UncookedDefinition old = _uncooked.addTemplate(name, unp);
        if (old != null) {
            line.reportError(nameOffset, "Duplicate template definition for name '%s'", name);
        }
    }

    /**
     * Shared parsing method implementation that handles parsing of contents of either
     * template, or extractor.
     *
     * @param hasParams whether it is legal to have template or extractor variables (references
     *     using positional index, to be passed on actual invocation)
     */
    private int _tokenizeTemplateContents(InputLine line, int ix, UncookedDefinitions uncookedDefs,
            DefPieceContainer container,
            int parenCount, String desc, ParameterCollector vars)
        throws DefinitionParseException
    {
        // And then need to find template AND pattern references, literal patterns
        final String contents = line.getContents();
        final int end = contents.length();
        StringBuilder sb = new StringBuilder();
        int literalStart = ix;
        final boolean gotVars = (vars != null);

        while (ix < end) {
            char c = contents.charAt(ix++);
            if ((c == '%') || (c == '@') || (c == '$')) {
                if (ix == end) {
                    line.reportError(ix, "Orphan '%c' at end of %s", c, desc);
                }
                // doubling up used as escaping mechanism:
                char d = contents.charAt(ix);
                if (c == d) {
                    sb.append(c);
                    ++ix;
                    continue;
                }
                if (sb.length() > 0) {
                    container.appendLiteralText(sb.toString(), literalStart);
                    sb.setLength(0);
                }

                // literal patterns allowed
                StringAndOffset p;
                if (c == '%') {
                    if (d == '{') {
                        ++ix;
                        p = TokenHelper.parseInlinePattern(line, contents, ix);
                        container.appendLiteralPattern(p.match, ix);
                    } else {
                        // otherwise named ref; no pattern variables (yet?)
                        p = TokenHelper.parseName("pattern", line, contents, ix, false);
                        container.appendPatternRef(p.match, ix);
                    }
                    ix = p.restOffset;
                } else if (c == '@') { // template, named refs, parameter refs, parameterized refs
                    ix = _tokenizeTemplateReference(line, ix, uncookedDefs, desc, vars, container);
                } else { // must be '$', extractor definition
                    p = TokenHelper.parseName("extractor", line, contents, ix, gotVars);
                    ix = p.restOffset;
                    ExtractorExpression extr;
                    int pos;
                    if (gotVars
                            && (pos = TokenHelper.parseIfNonNegativeNumber(p.match)) >= 0) {
                        // 1-based index; avoid OOME/DoS by not allowing positions past 999999
                        if ((pos < 1) || (pos > 999999)) {
                            line.reportError(ix, "Invalid extractor name parameter %d in %s", pos, desc);
                        }
                        vars.add(line, ix, pos, c);
                        extr = container.appendVariableExtractor(pos, ix);
                    } else {
                        extr = container.appendExtractor(p.match, ix);
                    }
                    // That was simple, but now need to decode contents, recursively
                    ix = _tokenizeInlineExtractor(line, ix, uncookedDefs, vars, extr);
                }
                literalStart = ix;
                continue;
            }
            // When parsing contents of an extractor
            if (parenCount > 0) {
                if (c == '(') {
                    ++parenCount;
                } else if (c == ')') {
                    if (--parenCount == 0) {
                        break;
                    }
                }
                // but append normally, unless we bailed out
            }
            sb.append(c);
        }

        if (sb.length() > 0) {
            container.appendLiteralText(sb.toString(), literalStart);
        }

        if (parenCount > 0) {
            line.reportError(ix, "Missing closing parenthesis at end of %s", desc);
        }
        return ix;
    }

    private int _tokenizeParameterizedTemplate(InputLine line, int ix, UncookedDefinitions uncookedDefs,
            String desc, ParameterCollector vars,
            TemplateReference ref)
        throws DefinitionParseException
    {
        final String contents = line.getContents();
        final int end = contents.length();

        // First things first: should have parameters, immediately following
        if ((ix >= end) || (contents.charAt(ix) != '(')) {
            line.reportError(ix,
                    "Missing parameter list for template reference '@%s'", ref.getText());
        }
        ++ix; // to skip open parenthesis
        
        for (int paramIndex = 1; ix < end; ++paramIndex) {
            char c = contents.charAt(ix++);
            if (c == ')') {
                return ix;
            }
            if (paramIndex > 1) { // expect comma after first parameter
                if (c != ',') {
                    line.reportError(ix,
                            "Unexpected character %s in template parameter list for '@%s': expected either ',' or ')')'",
                            TokenHelper.charDesc(c), ref.getText());
                }
                if (ix >= end) {
                    break;
                }
                c = contents.charAt(ix++);
            }
            switch (c) {
            case '@':
                ix = _tokenizeTemplateReference(line, ix, uncookedDefs,
                        desc, vars, ref);
                break;
            case '$':
                ix = _tokenizeExtractorParameter(line, ix, desc, vars, ref);
                break;
            default:
                line.reportError(ix,
                        "Unexpected character %s in template parameter list for '@%s': expected either type marker '@' or closing ')'",
                        TokenHelper.charDesc(c), ref.getText());
            }
        }

        // Should not get this far
        line.reportError(ix, "Unexpected end of line within parameter list for template '@%s'", ref.getText());
        return ix;
    }

    private int _tokenizeTemplateReference(InputLine line, int ix, UncookedDefinitions uncookedDefs,
            String desc,
            ParameterCollector vars, DefPieceAppendable container)
        throws DefinitionParseException
    {
        final String contents = line.getContents();
        StringAndOffset p = TokenHelper.parseName("template parameter", line, contents, ix, (vars != null));
        ix = p.restOffset;
        String id = p.match;
        int pos;
        if ((vars != null)
                && ((pos = TokenHelper.parseIfNonNegativeNumber(id)) >= 0)) {
            // 1-based index; avoid OOME/DoS by not allowing positions past 999999
            if ((pos < 1) || (pos > 999999)) {
                line.reportError(ix, "Invalid template parameter %d in %s", pos, desc);
            }
            vars.add(line, ix, pos, '@'); // this validates that type for given position is consistent
            container.append(new TemplateParameterReference(line, ix, container.getName(), pos));
        } else {
            // Verify that such a template exists
            UncookedDefinition refdTemplate = uncookedDefs.findTemplate(id);
            if (refdTemplate == null) {
                line.reportError(ix, "Referencing non-existing template '@%s' from '%s'",
                        id, desc);
            }
            TemplateReference ref = new TemplateReference(line, ix, id);
            container.append(ref);
            // then: do we expect parameters? If so, decode
            if (refdTemplate.hasParameters()) {
                ix = _tokenizeParameterizedTemplate(line, ix, uncookedDefs,
                        desc, vars, ref);
            }
        }
        return ix;
    }
    
    private int _tokenizeExtractorParameter(InputLine line, int ix,
            String desc,
            ParameterCollector vars, DefPieceAppendable container)
        throws DefinitionParseException
    {
        final String contents = line.getContents();
        StringAndOffset p = TokenHelper.parseName("extractor parameter", line, contents, ix, (vars != null));
        ix = p.restOffset;
        String id = p.match;
        int pos;

        if ((vars != null)
                && ((pos = TokenHelper.parseIfNonNegativeNumber(id)) >= 0)) {
            // 1-based index; avoid OOME/DoS by not allowing positions past 999999
            if ((pos < 1) || (pos > 999999)) {
                line.reportError(ix, "Invalid extractor parameter %d in %s", pos, desc);
            }
            vars.add(line, ix, pos, '$'); // this validates that type for given position is consistent
            container.append(new ExtractorParameterReference(line, ix, container.getName(), pos));
        } else { // or, if just passing name...
            container.append(new ExtractorExpression(line, ix, id));
        }
        return ix;
    }

    /**
     * Method that will read contents of a given inline extractor definition, up to
     * closing parenthesis
     */
    private int _tokenizeInlineExtractor(InputLine line, int ix,
            UncookedDefinitions uncookedDefs, ParameterCollector vars,
            ExtractorExpression extr)
        throws DefinitionParseException
    {
        final String contents = line.getContents();
        final int end = contents.length();

        if ((ix >= end) || contents.charAt(ix) != '(') {
            line.reportError(ix, "Invalid declaration for extractor '%s': missing opening parenthesis",
                    extr.getName());
        }
        ++ix;

        return _tokenizeTemplateContents(line, ix, uncookedDefs, extr, 1,
                "extractor '"+extr.getName()+"' expression", vars);
    }

    private void _readExtractionDefinition(InputLine line, int offset) throws IOException
    {
        String contents = line.getContents();
        StringAndOffset p = TokenHelper.parseNameAndSkipSpace("extraction", line, contents, offset);
        String name = p.match;

        // And the rest should consist of just a single open curly brace, and optional white space
        int ix = TokenHelper.matchRemaining(contents, p.restOffset, '{');
        if (ix != contents.length()) {
            line.reportError(p.restOffset, "Unexpected content for extraction '%s': expected only opening '{'",
                    name);
        }

        UncookedDefinition template = null;
        Map<String,Object> append = null;
        
        // For contents within, should have name/content sections
        while (true) {
            line = _lineReader.nextLine();
            if (line == null) {
                _lineReader.reportError("Unexpected end-of-input in extraction '%s' definition", name);
            }
            contents = line.getContents();
            // Either name/value pair, or closing brace
            ix = TokenHelper.matchRemaining(contents, 0, '}');
            if (ix >= 0) {
                if (ix >= contents.length()) {
                    break;
                }
                line.reportError(p.restOffset, "Unexpected content after closing '}' for extraction '%s'",
                        name);
            }

            ix = TokenHelper.skipSpace(contents, 0);
            p = TokenHelper.parseNameAndSkipSpace("extraction", line, contents, ix);
            ix = p.restOffset;
            String prop = p.match;

            switch (prop) {
            case "template":
                {
                    if (template != null) {
                        line.reportError(ix, "More than one 'template' specified for '"+name+"'");
                    }
                    template = new UncookedDefinition(line, "", false, ix);
                }
                break;
            case "append":
                // Contents are JSON, but for convenience we wrap it as Object if not
                // already Object (since we must get key/value pairs)
                {
                    append = _readAppend(line, ix, contents.substring(ix), append);
                }
                break;
            default:
                line.reportError(ix, "Unrecognized extraction property \"%s\" encountered; expected one of %s",
                        prop, EXTRACTOR_PROPERTIES);
            }
        }

        if (template == null) {
            line.reportError(ix, "Missing 'template' for extraction '%s'", name);
        }
        
        UncookedExtraction extr = new UncookedExtraction(line, name, template, append);
        _uncooked.addExtraction(name, extr);
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private Map<String,Object> _readAppend(InputLine line, int offset,
            String rawJson, Map<String,Object> old)
        throws DefinitionParseException
    {
        rawJson = rawJson.trim();
        if (rawJson.isEmpty()) {
            return old;
        }
        
        // First things first: ensure it's a JSON Object
        if (!rawJson.startsWith("{")) {
            // but must start with a field name
            if (rawJson.startsWith("\"")) {
                rawJson = "{" + rawJson + "}";
            }
        }
        Object raw;
        try {
            raw = _json.anyFrom(rawJson);
        } catch (Exception e) {
            DefinitionParseException exc = DefinitionParseException.construct
                    ("Invalid JSON content to 'append': "+e.getMessage(), line, offset);
            exc.initCause(e);
            throw exc;
        }
        if (!(raw instanceof Map<?,?>)) {
            line.reportError(offset,
                    "Invalid 'append' value: must be JSON Object, or sequence of key/value pairs; was parsed as %s",
                    raw.getClass().getName());
        }
        @SuppressWarnings("unchecked")
        Map<String,Object> newStuff = (Map<String,Object>) raw;

        if (old == null) {
            return newStuff;
        }
        old.putAll(newStuff);
        return old;
    }
}
