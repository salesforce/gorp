package com.salesforce.gorp;

import com.salesforce.gorp.DefinitionReader;
import com.salesforce.gorp.Gorp;
import com.salesforce.gorp.autom.PolyMatcher;

/**
 * Test that verifies that Polymatcher constructed from definitions seems
 * to work to basic level.
 */
public class PolyMatchTest extends TestBase
{
    public void testSimple() throws Exception
    {
        final String DEF =
"pattern %word (\\w+)\n"+
"template @base %word\n"+
"extract rule1 {  \n"+
"  template @base value=$value(%word) value2=$value2(%word)\n"+
"}\n"+
"extract rule2 {  \n"+
"  template value=%word\n"+
"}\n"+
                    "";
        DefinitionReader defR = DefinitionReader.reader(DEF);
        Gorp def = defR.read();
        PolyMatcher matcher = def.getMatcher();

        int[] matches = matcher.match("value=stuff");
        assertEquals(1, matches.length);
        assertEquals(1, matches[0]);

        matches = matcher.match("prefix value=a value2=b");
        assertEquals(1, matches.length);
        assertEquals(0, matches[0]);
    }

    public void testIntermediate() throws Exception
    {
        final String DEF =
//"pattern %phrase [^ \\t]+\n"+
"pattern %phrase \\S+\n"+
"pattern %num \\d+\n"+
"pattern %ts %phrase\n"+
"extract interm {  \n"+
"  template <%num> (foo)[bar] $eventTimeStamp(%ts) end:'$timestamp(%ts)' THE END.\n"+
"}\n"+
    "";

        DefinitionReader defR = DefinitionReader.reader(DEF);
        Gorp def = defR.read();
        PolyMatcher matcher = def.getMatcher();

        int[] matches = matcher.match("<123> (foo)[bar] 12:30:58 end:'15:07:00Z' THE END.");
        assertEquals(1, matches.length);
        assertEquals(0, matches[0]);
    }

    public void testQuoted() throws Exception
    {
        final String DEF =
"pattern %word (\\w+)\n"+
"pattern %quoted \\\"[^\\\"]*\\\"\n"+
"extract quoted {  \n"+
"  template header value=$value(%quoted)\n"+
"}\n"+
"extract unquoted {  \n"+
"  template header value=$value(%word)\n"+
"}\n"+
                    "";
        DefinitionReader defR = DefinitionReader.reader(DEF);
        Gorp def = defR.read();
        PolyMatcher matcher = def.getMatcher();

        int[] matches;

        matches = matcher.match("header value=stuff");
        assertEquals(1, matches.length);
        assertEquals(1, matches[0]);

        matches = matcher.match("header value=\"stuff\"");
        assertEquals(1, matches.length);
        assertEquals(0, matches[0]);
    }

    public void testComplex() throws Exception
    {
        final String DEF =
"pattern %word [a-zA-Z]+\n"+
"pattern %phrase [^ \t]+\n"+
"pattern %num ([0-9]+)\n"+
"pattern %ts %phrase\n"+
"pattern %ip %phrase\n"+
"pattern %maybeUUID %phrase\n"+
"pattern %hostname %phrase\n"+
"template @base <%num>$eventTimeStamp(%ts) $logAgent(%ip) RealSource: \"$logSrcIp(%ip)\"\\\n"+
" Environment: \"$environment(%phrase)\"\\\n"+
" UUID: \"$uuid(%maybeUUID)\"\\\n"+
" RawMsg: <%num>$rawMsgTS(%word %num %phrase) $logSrcHostname(%hostname) $appname(%word)[$appPID(%num)]\n"+
"\n"+
"extract baseMatch {\n"+
"  template @base\n"+
"}\n";

        String INPUT = "<86>2015-05-12T20:57:53.302858+00:00 10.1.11.141 RealSource: \"10.10.5.3\""
                +" Environment: \"TEST\""
                +" UUID: \"NONE\""
                +" RawMsg: <123>something 1324 keyboard-interactive/pam google.com sshd[137]"
                ;
        
        DefinitionReader defR = DefinitionReader.reader(DEF);
        Gorp def = defR.read();
        PolyMatcher matcher = def.getMatcher();

        int[] matches = matcher.match(INPUT);
        assertEquals(1, matches.length);
        assertEquals(0, matches[0]);
    }
}
