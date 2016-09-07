package com.salesforce.gorp;

import java.util.Map;

import com.salesforce.gorp.DefinitionParseException;
import com.salesforce.gorp.DefinitionReader;
import com.salesforce.gorp.ExtractionResult;
import com.salesforce.gorp.Gorp;

public class ParametricTemplateTest extends TestBase
{
   public void testSimple() throws Exception
   {
       final String DEF =
"pattern %word ([a-zA-Z]+)\n"+
"pattern %num ([0-9]+)\n"+
"pattern %ip [a-zA-Z\\.]+\n"+
"template @ip %ip\n"+
"template @port %num\n"+
"template @colonPair() @1:@2\n"+
"extract Net {  \n"+
"  template $endpoint(@colonPair(@ip,@port))/%word\n"+
"}\n"+
                   "";
       DefinitionReader defR = DefinitionReader.reader(DEF);
       Gorp def = defR.read();

       ExtractionResult result = def.extract("foo.bar.com:8080/user");
       assertNotNull(result);
       assertEquals("Net", result.getId());
       Map<String,Object> stuff = result.asMap();
       assertEquals("foo.bar.com:8080", stuff.get("endpoint"));
       assertEquals(1, stuff.size());
   }

   /*
   /**********************************************************************
   /* Tests to ensure parsing, error handling work
   /**********************************************************************
    */

   public void testErrorMissingParameters() throws Exception
   {
       // Invalid: must have parameters used if declared to have some
       String DEF =
"pattern %word ([a-zA-Z]+)\n"+
"template @pair() @1:@2\n"+
"template @full @pair\n"+
"extract Result {  \n"+
"  template @full\n"+
"}\n"+
                   "";
       DefinitionReader defR = DefinitionReader.reader(DEF);
       try {
           defR.read();
           fail("Should not pass");
       } catch (DefinitionParseException e) {
           verifyException(e, "Missing parameter list");
           verifyException(e, "@pair");
       }
   }

   // If template is to take parameters, must be declared with parenthesis
   public void testErrorParametersNotDeclared() throws Exception
   {
       // Invalid: must have parameters used if declared to have some
       String DEF =
"template @pair @1:@2\n"+
"template @full xyz\n"+
"extract Result {  \n"+
"  template @full\n"+
"}\n"+
                   "";
       DefinitionReader defR = DefinitionReader.reader(DEF);
       try {
           defR.read();
           fail("Should not pass");
       } catch (DefinitionParseException e) {
           verifyException(e, "Invalid variable reference");
       }
   }

   public void testErrorMalformedParameterList() throws Exception
   {
       // Invalid: must have parameters used if declared to have some
       String DEF =
"template @pair() @1:@2\n"+
"template @a    a\n"+
"template @full @pair(@a\n"+
"extract Result {  \n"+
"  template @full\n"+
"}\n"+
                   "";
       DefinitionReader defR = DefinitionReader.reader(DEF);
       try {
           defR.read();
           fail("Should not pass");
       } catch (DefinitionParseException e) {
           verifyException(e, "Unexpected end of line");
       }
   }

   public void testErrorUndefined() throws Exception
   {
       // Invalid: must have parameters used if declared to have some
       String DEF =
"template @constant text\n"+
"template @abc @full(@ab(@c,@1))\n"+
"template @full() @1\n"+
"extract Result {  \n"+
"  template @full\n"+
"}\n"+
                   "";
       DefinitionReader defR = DefinitionReader.reader(DEF);
       try {
           defR.read();
           fail("Should not pass");
       } catch (DefinitionParseException e) {
           verifyException(e, "non-existing template '@ab'");
       }
   }

   public void testErrorParamMismatch() throws Exception
   {
       // number of formal, actual parameters must match
       String DEF =
"template @pair() @1:@2\n"+
"template @foo foosball\n"+
"template @fooPair @pair(@foo)\n"+
"extract Result {  \n"+
"  template @fooPair\n"+
"}\n";
       DefinitionReader defR = DefinitionReader.reader(DEF);
       try {
           defR.read();
           fail("Should not pass");
       } catch (DefinitionParseException e) {
           verifyException(e, "Parameter mismatch");
       }

       DEF =
"template @pair() @1:@2\n"+
"template @foo foosball\n"+
"template @fooPair @pair(@foo,@foo,@foo)\n"+
"extract Result {  \n"+
"  template @fooPair\n"+
"}\n";
       defR = DefinitionReader.reader(DEF);
       try {
           defR.read();
           fail("Should not pass");
       } catch (DefinitionParseException e) {
           verifyException(e, "Parameter mismatch");
       }
   }
}
