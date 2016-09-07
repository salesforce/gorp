package com.salesforce.gorp;

import java.util.Map;

import com.salesforce.gorp.DefinitionParseException;
import com.salesforce.gorp.DefinitionReader;
import com.salesforce.gorp.model.*;

public class PatternResolutionTest extends TestBase
{
    public void testSimple() throws Exception
    {
        final String DEF =
"pattern %a a\n"+
"pattern %b b\n"+
"pattern %c stuff!\n"+
"pattern %abba (%a%b %'b'-%a)\n"+
"pattern %full %abba %c\n"+
                    "";
        DefinitionReader defR = DefinitionReader.reader(DEF);
        defR.readUncooked();
        // sanity check
        assertEquals(5, defR._uncooked.getPatterns().size());
        
        defR.resolvePatterns();

        CookedDefinitions cooked = defR._cooked;

        Map<String, LiteralPattern> pats = cooked.getPatterns();
        assertEquals(5, pats.size());

        // and then verify that we got them all right
        assertEquals("a", pats.get("a").getText());
        assertEquals("b", pats.get("b").getText());
        assertEquals("stuff!", pats.get("c").getText());
        assertEquals("(ab b-a)", pats.get("abba").getText());
        assertEquals("(ab b-a) stuff!", pats.get("full").getText());
    }

    public void testFailForUndefined() throws Exception
    {
        DefinitionReader defR = DefinitionReader.reader("pattern %a Ok: %b\npattern %b But... %c\n");
        defR.readUncooked();
        try {
            defR.resolvePatterns();
        } catch (DefinitionParseException e) {
            verifyException(e, "non-existing pattern '%c'");
        }
    }

    public void testFailForDirectCycle() throws Exception
    {
        DefinitionReader defR = DefinitionReader.reader("pattern %a Kaboom: %a\n");
        defR.readUncooked();
        try {
            defR.resolvePatterns();
        } catch (DefinitionParseException e) {
            verifyException(e, "cyclic pattern reference to '%a'");
        }

        defR = DefinitionReader.reader("pattern %a %b\npattern %b %a");
        defR.readUncooked();
        try {
            defR.resolvePatterns();
        } catch (DefinitionParseException e) {
            verifyException(e, "cyclic pattern reference to '%a'");
        }
    }
}
