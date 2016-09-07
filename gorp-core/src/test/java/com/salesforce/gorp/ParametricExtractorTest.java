package com.salesforce.gorp;

import java.util.Map;

import com.salesforce.gorp.DefinitionParseException;
import com.salesforce.gorp.DefinitionReader;
import com.salesforce.gorp.ExtractionResult;
import com.salesforce.gorp.Gorp;

public class ParametricExtractorTest extends TestBase
{
    public void testSimple() throws Exception
    {
        final String DEF =
 "pattern %num ([0-9]+)\n"+
 "pattern %word ([a-zA-Z]+)\n"+
 "pattern %ip [a-zA-Z\\.]+\n"+
 "template @ip %ip\n"+
 "template @port %num\n"+
 "template @endpoint() $1(@ip):$2(@port)\n"+
 "extract Net {  \n"+
 "  template @endpoint($srcIp,$srcPort)/%word\n"+
 "}\n";
        DefinitionReader defR = DefinitionReader.reader(DEF);
        Gorp def = defR.read();

        ExtractionResult result = def.extract("foo.bar.com:8080/user");
        assertNotNull(result);
        assertEquals("Net", result.getId());
        Map<String,Object> stuff = result.asMap();
        assertEquals("foo.bar.com", stuff.get("srcIp"));
        assertEquals("8080", stuff.get("srcPort"));
        assertEquals(2, stuff.size());
    }

    /*
    /**********************************************************************
    /* Tests to ensure parsing, error handling work
    /**********************************************************************
     */

    public void testErrorDupNames() throws Exception
    {
        // Invalid: dup names
        final String DEF =
 "pattern %num ([0-9]+)\n"+
 "pattern %word ([a-zA-Z]+)\n"+
 "pattern %ip [a-zA-Z\\.]+\n"+
 "template @ip %ip\n"+
 "template @port %num\n"+
 "template @endpoint() $1(@ip):$2(@port)\n"+
 "extract Net {  \n"+
 "  template @endpoint($srcIp,$srcPort)/%word @endpoint($srcIp,$whatever)\n"+
 "}\n";
        DefinitionReader defR = DefinitionReader.reader(DEF);
        try {
            defR.read();
            fail("Should not pass");
        } catch (DefinitionParseException e) {
            verifyException(e, "duplicate extractor name");
            verifyException(e, "srcIp");
        }
    }
}
