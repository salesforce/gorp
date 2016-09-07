package com.salesforce.gorp.util;

import com.salesforce.gorp.TestBase;
import com.salesforce.gorp.util.RegexHelper;

public class RegexHelperTest extends TestBase
{
    public void testLiteralTextEscaping()
    {
        assertEquals("", RegexHelper.quoteLiteralAsRegexp(""));
        assertEquals("\\(foo\\)", RegexHelper.quoteLiteralAsRegexp("(foo)"));
        assertEquals("\\[foo\\]", RegexHelper.quoteLiteralAsRegexp("[foo]"));
        assertEquals("a\\\\b", RegexHelper.quoteLiteralAsRegexp("a\\b"));
    }

    public void testPatternForAutomatonEscaping()
    {
        assertEquals("", RegexHelper.massageRegexpForAutomaton(""));

        // and then "well-known" regexps, both within and outside of brackets
        assertEquals("["+RegexHelper.CHAR_CLASS_AUTOMATON_w+"]+",
                RegexHelper.massageRegexpForAutomaton("[\\w]+"));
        assertEquals("["+RegexHelper.CHAR_CLASS_AUTOMATON_w+"]+",
                RegexHelper.massageRegexpForAutomaton("\\w+"));
        assertEquals("["+RegexHelper.CHAR_CLASS_AUTOMATON_d+RegexHelper.CHAR_CLASS_AUTOMATON_s+"]+",
                RegexHelper.massageRegexpForAutomaton("[\\d\\s]+"));
    }

    public void testPatternForRegexpEscaping()
    {
        assertEquals("", RegexHelper.massageRegexpForJDK(""));

        // Need to change groups to non-matching
        assertEquals("stuff(?:[ab]+(?:[de]+))",
                RegexHelper.massageRegexpForJDK("stuff([ab]+([de]+))"));
        assertEquals("stuff\\(sic\\)",
                RegexHelper.massageRegexpForJDK("stuff\\(sic\\)"));
    }
}
