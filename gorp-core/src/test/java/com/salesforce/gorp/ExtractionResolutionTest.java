package com.salesforce.gorp;

import java.util.*;

import com.salesforce.gorp.DefinitionParseException;
import com.salesforce.gorp.DefinitionReader;
import com.salesforce.gorp.Gorp;
import com.salesforce.gorp.model.*;

public class ExtractionResolutionTest extends TestBase
{
    public void testSimple() throws Exception
    {
        final String DEF =
"pattern %a a\n"+
"template @base (%a:foo)\n"+
"extract rule1 {  \n"+
"  template @base value=$MyValue(%a:%{\\w+})\n"+
"  append { \"enabled\" : true, \"x\" : 3 }\n"+
"}"+
                    "";
        DefinitionReader defR = DefinitionReader.reader(DEF);
        defR.readUncooked();
        // sanity check
        assertEquals(1, defR._uncooked.getPatterns().size());
        assertEquals(1, defR._uncooked.getTemplates().size());
        assertEquals(1, defR._uncooked.getExtractions().size());

        Gorp fullDef = defR.read();

        List<CookedExtraction> extras = fullDef.getExtractions();
        assertEquals(1, extras.size());
        CookedExtraction extr = extras.get(0);

        // easy part first, check append
        Map<String,Object> appends = extr.getExtra();
        assertNotNull(appends);
        assertEquals(2, appends.size());
        assertEquals(Boolean.TRUE, appends.get("enabled"));
        assertEquals(Integer.valueOf(3), appends.get("x"));

        /*
        CookedTemplate t = extr.getTemplate();

        // and then the beef, templates:
        List<DefPiece> parts;

        parts = (List<DefPiece>) t.getParts();

        assertEquals(5, parts.size());
        _assertPart(parts.get(0), LiteralText.class, "(");
        _assertPart(parts.get(1), LiteralPattern.class, "a");
        _assertPart(parts.get(2), LiteralText.class, ":foo)");
        _assertPart(parts.get(3), LiteralText.class, " value=");
        _assertPart(parts.get(4), ExtractorExpression.class, "MyValue");

        ExtractorExpression extractor = (ExtractorExpression) parts.get(4);
        parts = (List<DefPiece>) extractor.getParts();
        assertEquals(3, parts.size());
        _assertPart(parts.get(0), LiteralPattern.class, "a");
        _assertPart(parts.get(1), LiteralText.class, ":");
        _assertPart(parts.get(2), LiteralPattern.class, "\\w+");
        */
    }

    // // // Failing tests

    public void testEmpty() throws Exception
    {
        final String DEF = "pattern %a a\n"+
"template @base (%a:foo)\n";
        DefinitionReader defR = DefinitionReader.reader(DEF);
        try {
            defR.read();
            fail("Should have failed due to no extractions");
        } catch (DefinitionParseException e) {
            verifyException(e, "No extraction definitions found");
        }
    }

    public void testDupExtractorName() throws Exception
    {
        final String DEF = "pattern %word \\w+\n"+
"template @extr $value(%word)\n"+
"extract match {  \n"+
"  template @extr @extr\n"+
"}\n";

        DefinitionReader defR = DefinitionReader.reader(DEF);
        try {
            defR.read();
            fail("Should have failed due to dup extraction name");
        } catch (DefinitionParseException e) {
            verifyException(e, "Duplicate extractor name");
        }
    }
}
