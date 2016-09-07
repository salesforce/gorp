package com.salesforce.gorp.io;

import com.salesforce.gorp.TestBase;
import com.salesforce.gorp.io.InputLine;

public class InputLineTest extends TestBase
{
    public void testSingleLine() {
        InputLine inputLine = InputLine.create("foo", 2, "Something");
        assertEquals(1, inputLine.rowCount());
        assertEquals(2, inputLine.getStartRow());
        assertEquals("Something", inputLine.getContents());
    }

    public void testMultiLine() {
        InputLine inputLine = InputLine.create("File 'foo'", 5, "Line1");
        inputLine = inputLine.appendSegment("Line2");
        inputLine = inputLine.appendSegment("Line3");
        // physical column offset would be 2, but reported as 1-based (like rows), so:
        assertEquals("[File 'foo' (7,3)]", inputLine.constructDesc(12));
    }
}
