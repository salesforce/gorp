package com.salesforce.gorp;

import java.io.IOException;
import java.util.*;

import com.salesforce.gorp.DefinitionReader;
import com.salesforce.gorp.model.DefPiece;
import com.salesforce.gorp.model.ExtractorExpression;
import com.salesforce.gorp.model.LiteralPattern;
import com.salesforce.gorp.model.LiteralText;
import com.salesforce.gorp.model.PatternReference;
import com.salesforce.gorp.model.TemplateReference;
import com.salesforce.gorp.model.UncookedDefinition;
import com.salesforce.gorp.model.UncookedDefinitions;
import com.salesforce.gorp.model.UncookedExtraction;

public class UncookedDefTest extends TestBase
{
    public void testSimple() throws Exception
    {
        final String DEF =
"pattern %ws \\s+\n"+
"pattern %optws \\s*\n"+
"pattern %'phrase' \\S+\n"+
"pattern %\"maybeUUID\" %'phrase'\n"+
"# hyphen not valid, must be quoted:\n"+
"pattern %'host-name' %\"phrase\"\n"+
"\n"+
"template @simple Prefix:\n"+
"template @'base' %phrase%optws(sic!) @simple %'host-name'\n"+
"\n"+
"extract FooMessage {  \n"+
"  template @base ($authStatus(Accepted))\n"+
"  append \"service\":\"ssh\", \"logType\":\"security\"  \n"+
"}\n"+

				"";
        DefinitionReader defR = DefinitionReader.reader(DEF);
        defR.readUncooked();
        UncookedDefinitions def = defR._uncooked;

        Map<String,UncookedDefinition> patterns = def.getPatterns();
        
        assertEquals(5, patterns.size());

        assertTrue(patterns.containsKey("ws"));
        assertTrue(patterns.containsKey("optws"));
        assertTrue(patterns.containsKey("phrase"));
        assertTrue(patterns.containsKey("maybeUUID"));
        assertTrue(patterns.containsKey("host-name"));

        Map<String,UncookedDefinition> templates = def.getTemplates();
        assertEquals(2, templates.size());
        assertTrue(templates.containsKey("simple"));
        assertTrue(templates.containsKey("base"));

        Map<String,UncookedExtraction> extr = def.getExtractions();
        assertEquals(1, extr.size());
        assertTrue(extr.containsKey("FooMessage"));
    }

    public void testPatternRefsInPatterns() throws Exception
    {
        final String DEF =
"pattern %wsChar \\s\n"+
"pattern %optws %wsChar*%%\n"+
"pattern %word ([a-z]+)\n"+
"pattern %phrase3   %word %word2%word3\n"
                    ;
        DefinitionReader defR = DefinitionReader.reader(DEF);
        defR.readUncooked();
        UncookedDefinitions def = defR._uncooked;
        Map<String,UncookedDefinition> patterns = def.getPatterns();

        assertEquals(4, patterns.size());
        List<DefPiece> parts;

        // Let's see handling of composite definition
        UncookedDefinition optws = patterns.get("optws");
        assertNotNull(optws);

        parts = optws.getParts();
        assertEquals(2, parts.size());
        assertEquals(PatternReference.class, parts.get(0).getClass());
        assertEquals("wsChar", parts.get(0).getText());
        assertEquals(LiteralPattern.class, parts.get(1).getClass());
        assertEquals("*%", parts.get(1).getText());
        
        UncookedDefinition p3 = patterns.get("phrase3");
        assertNotNull(p3);
        
        parts = p3.getParts();
        assertEquals(4, parts.size());
        assertEquals(PatternReference.class, parts.get(0).getClass());
        assertEquals("word", parts.get(0).getText());
        assertEquals(LiteralPattern.class, parts.get(1).getClass());
        assertEquals(" ", parts.get(1).getText());
        assertEquals(PatternReference.class, parts.get(2).getClass());
        assertEquals("word2", parts.get(2).getText());
        assertEquals(PatternReference.class, parts.get(3).getClass());
        assertEquals("word3", parts.get(3).getText());
    }

    public void testTemplateRefsInPatterns() throws Exception
    {
        final String DEF =
"pattern %wsChar \\s\n"+
"\n"+
"template @base Stuff:\n"+
"template @actual @'base'%'wsChar'and%{\\s}more\n"+
""
                    ;
        DefinitionReader defR = DefinitionReader.reader(DEF);
        defR.readUncooked();
        UncookedDefinitions def = defR._uncooked;
        Map<String,UncookedDefinition> templates = def.getTemplates();

        assertEquals(2, templates.size());
        List<DefPiece> parts;

        // Let's see handling of composite definition
        UncookedDefinition base = templates.get("base");
        assertNotNull(base);

        parts = base.getParts();
        assertEquals(1, parts.size());
        assertEquals(LiteralText.class, parts.get(0).getClass());
        assertEquals("Stuff:", parts.get(0).getText());
        
        UncookedDefinition p5 = templates.get("actual");
        assertNotNull(p5);
        
        parts = p5.getParts();
        assertEquals(5, parts.size());
        assertEquals(TemplateReference.class, parts.get(0).getClass());
        assertEquals("base", parts.get(0).getText());
        assertEquals(PatternReference.class, parts.get(1).getClass());
        assertEquals("wsChar", parts.get(1).getText());
        assertEquals(LiteralText.class, parts.get(2).getClass());
        assertEquals("and", parts.get(2).getText());
        assertEquals(LiteralPattern.class, parts.get(3).getClass());
        assertEquals("\\s", parts.get(3).getText());
        assertEquals(LiteralText.class, parts.get(4).getClass());
        assertEquals("more", parts.get(4).getText());
    }

    public void testExtractors() throws Exception
    {
        final String DEF = "template @actual value=$value(Accepted$$%{\\d+})\n";
                    ;
        DefinitionReader defR = DefinitionReader.reader(DEF);
        defR.readUncooked();
        UncookedDefinitions def = defR._uncooked;
        Map<String,UncookedDefinition> templates = def.getTemplates();

        assertEquals(1, templates.size());

        UncookedDefinition actual = templates.get("actual");
        assertNotNull(actual);

        _assertPart(actual.getParts().get(0), LiteralText.class, "value=");
        _assertPart(actual.getParts().get(1), ExtractorExpression.class, "value");
        ExtractorExpression extr = (ExtractorExpression) actual.getParts().get(1);
        List<DefPiece> parts = (List<DefPiece>) extr.getParts();
        assertEquals(2, parts.size());
        _assertPart(parts.get(0), LiteralText.class, "Accepted$");
        _assertPart(parts.get(1), LiteralPattern.class, "\\d+");
    }

    public void testExtractors2() throws Exception
    {
        final String DEF =
"pattern %w [a-zA-Z]+\n"+
"template @base value=$value(%w)\n"+
"template @full @base extra=$extra($prop1(%w),$prop2(%w))\n"
                    ;
        DefinitionReader defR = DefinitionReader.reader(DEF);
        defR.readUncooked();
        UncookedDefinitions def = defR._uncooked;
        Map<String,UncookedDefinition> templates = def.getTemplates();

        assertEquals(2, templates.size());

        UncookedDefinition actual = templates.get("base");
        assertNotNull(actual);
        _assertPart(actual.getParts().get(0), LiteralText.class, "value=");
        _assertPart(actual.getParts().get(1), ExtractorExpression.class, "value");
        ExtractorExpression extr = (ExtractorExpression) actual.getParts().get(1);
        List<DefPiece> parts = (List<DefPiece>) extr.getParts();
        assertEquals(1, parts.size());
        _assertPart(parts.get(0), PatternReference.class, "w");

        UncookedDefinition full = templates.get("full");
        assertNotNull(full);
        _assertPart(full.getParts().get(0), TemplateReference.class, "base");
        _assertPart(full.getParts().get(1), LiteralText.class, " extra=");
        _assertPart(full.getParts().get(2), ExtractorExpression.class, "extra");

        extr = (ExtractorExpression) full.getParts().get(2);
        parts = (List<DefPiece>) extr.getParts();
        if (parts.size() > 0) {
            _assertPart(parts.get(0), ExtractorExpression.class, "prop1");
            if (parts.size() > 1) {
                _assertPart(parts.get(1), LiteralText.class, ",");
                if (parts.size() > 2) {
                    _assertPart(parts.get(2), ExtractorExpression.class, "prop2");
                }
            }
        }
        assertEquals(3, parts.size());
    }
    
    // // // // Tests for failure handling
    
    public void testDupPatternName() throws Exception
    {
        final String DEF =
"pattern %'ws' \\s+\n"+
"pattern %optws \\s*\n"+
"pattern %ws \\S+\n"
        ;
        DefinitionReader defR = DefinitionReader.reader(DEF);
        try {
            defR.readUncooked();
            fail("Should have detected duplicate name");
        } catch (IOException e) {
            verifyException(e, "duplicate");
        }
    }

    public void testOrphanPercent() throws Exception
    {
        final String DEF =
"pattern %'ws' \\s+%\n"
        ;
        DefinitionReader defR = DefinitionReader.reader(DEF);
        try {
            defR.readUncooked();
            fail("Should have detected duplicate name");
        } catch (IOException e) {
            verifyException(e, "Orphan '%'");
        }
    }
}
