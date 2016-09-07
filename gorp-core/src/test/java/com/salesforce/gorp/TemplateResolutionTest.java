package com.salesforce.gorp;

import java.util.List;
import java.util.Map;

import com.salesforce.gorp.DefinitionReader;
import com.salesforce.gorp.model.*;

public class TemplateResolutionTest extends TestBase
{
    public void testSimplest() throws Exception
    {
        final String DEF = "template @base (%{a}:foo)\n"+
"template @full @base...\n";
        DefinitionReader defR = DefinitionReader.reader(DEF);
        defR.readUncooked();
        // sanity checks
        assertEquals(0, defR._uncooked.getPatterns().size());
        assertEquals(2, defR._uncooked.getTemplates().size());
        defR.resolvePatterns();
        defR.resolveTemplates();
        CookedDefinitions cooked = defR._cooked;

        List<DefPiece> parts;

        CookedTemplate t = cooked.findTemplate("base");
        assertNotNull(t);
        parts = (List<DefPiece>) t.getParts();
        assertEquals(3, parts.size());
        _assertPart(parts.get(0), LiteralText.class, "(");
        _assertPart(parts.get(1), LiteralPattern.class, "a");
        _assertPart(parts.get(2), LiteralText.class, ":foo)");

        t = cooked.findTemplate("full");
        assertNotNull(t);
        parts = (List<DefPiece>) t.getParts();

        // First three same as above
        assertEquals(4, parts.size());
        _assertPart(parts.get(0), LiteralText.class, "(");
        _assertPart(parts.get(1), LiteralPattern.class, "a");
        _assertPart(parts.get(2), LiteralText.class, ":foo)");
        _assertPart(parts.get(3), LiteralText.class, "...");
    }

    public void testSimple() throws Exception
    {
        final String DEF =
"pattern %a a\n"+
"template @base (%a:foo)\n"+
"template @full @base...%{[.*{2}]}--%a\n"+
                    "";
        DefinitionReader defR = DefinitionReader.reader(DEF);
        defR.readUncooked();
        // sanity check
        assertEquals(1, defR._uncooked.getPatterns().size());
        assertEquals(2, defR._uncooked.getTemplates().size());

        defR.resolvePatterns();
        defR.resolveTemplates();

        CookedDefinitions cooked = defR._cooked;

        // sanity check for pattern(s)
        Map<String, LiteralPattern> patterns = cooked.getPatterns();
        assertEquals(1, patterns.size());
        assertEquals("a", patterns.get("a").getText());

        // and then the beef, templates:
        List<DefPiece> parts;

        CookedTemplate t = cooked.findTemplate("base");
        assertNotNull(t);
        parts = (List<DefPiece>) t.getParts();
        assertEquals(3, parts.size());
        _assertPart(parts.get(0), LiteralText.class, "(");
        _assertPart(parts.get(1), LiteralPattern.class, "a");
        _assertPart(parts.get(2), LiteralText.class, ":foo)");

        t = cooked.findTemplate("full");
        assertNotNull(t);
        parts = (List<DefPiece>) t.getParts();

        // First three same as above
        assertEquals(7, parts.size());
        _assertPart(parts.get(0), LiteralText.class, "(");
        _assertPart(parts.get(1), LiteralPattern.class, "a");
        _assertPart(parts.get(2), LiteralText.class, ":foo)");
        _assertPart(parts.get(3), LiteralText.class, "...");
        _assertPart(parts.get(4), LiteralPattern.class, "[.*{2}]");
        _assertPart(parts.get(5), LiteralText.class, "--");
        _assertPart(parts.get(6), LiteralPattern.class, "a");
    }

    public void testWithExtractors() throws Exception
    {
        final String DEF =
"pattern %w [a-zA-Z]+\n"+
"template @base value=$value(%w)\n"+
"template @full @base extra=$extra($prop1(%w),$prop2(%w))\n"+
                    "";
        DefinitionReader defR = DefinitionReader.reader(DEF);
        defR.readUncooked();
        // sanity check
        assertEquals(1, defR._uncooked.getPatterns().size());
        assertEquals(2, defR._uncooked.getTemplates().size());

        defR.resolvePatterns();

        CookedDefinitions cooked = defR._cooked;

        // sanity check for pattern(s)
        Map<String, LiteralPattern> patterns = cooked.getPatterns();
        assertEquals(1, patterns.size());

        // then templates
        defR.resolveTemplates();
        List<DefPiece> parts;

        CookedTemplate t = cooked.findTemplate("base");
        assertNotNull(t);
        parts = (List<DefPiece>) t.getParts();

        assertEquals(2, parts.size());
        _assertPart(parts.get(0), LiteralText.class, "value=");
        _assertPart(parts.get(1), ExtractorExpression.class, "value");

        t = cooked.findTemplate("full");
        assertNotNull(t);
        parts = (List<DefPiece>) t.getParts();
        assertEquals(4, parts.size());
        _assertPart(parts.get(0), LiteralText.class, "value=");
        _assertPart(parts.get(1), ExtractorExpression.class, "value");
        _assertPart(parts.get(2), LiteralText.class, " extra=");
        _assertPart(parts.get(3), ExtractorExpression.class, "extra");

        parts = (List<DefPiece>) ((ExtractorExpression) parts.get(3)).getParts();
        assertEquals(3, parts.size());
        _assertPart(parts.get(0), ExtractorExpression.class, "prop1");
        _assertPart(parts.get(1), LiteralText.class, ",");
        _assertPart(parts.get(2), ExtractorExpression.class, "prop2");
    }
}
