package com.salesforce.gorp;

import java.util.Arrays;

import com.salesforce.gorp.model.DefPiece;

import junit.framework.TestCase;

public abstract class TestBase
    extends TestCase
{
    protected void verifyException(Throwable e, String... matches)
    {
        String msg = e.getMessage();
        String lmsg = (msg == null) ? "" : msg.toLowerCase();
        for (String match : matches) {
            String lmatch = match.toLowerCase();
            if (lmsg.indexOf(lmatch) >= 0) {
                return;
            }
        }
        fail("Expected an exception with one of substrings ("+Arrays.asList(matches)+"): got one with message \""+msg+"\"");
    }

    protected void _assertPart(DefPiece part, Class<?> expClass, String expText) {
        assertEquals(expClass, part.getClass());
        assertEquals(expText, part.getText());
    }
}
