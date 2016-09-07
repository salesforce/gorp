package com.salesforce.gorp.io;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import com.salesforce.gorp.TestBase;
import com.salesforce.gorp.io.InputLine;
import com.salesforce.gorp.io.InputLineReader;

public class InputLineReaderTest extends TestBase
{
    public void testSimple() throws Exception
    {
        List<InputLine> lines = _readAllLines(
                "line 1",
                "line 2",
                "# commentary",
                "   ",
                "line 3\\",
                " with continuation \\",
                "or two...\\",
                " or three!",
                "    # more comments"
                );
        if (lines.size() < 3) {
            fail("Expected 3 lines, got "+lines.size());
        }
        assertEquals("line 1", lines.get(0).getContents());
        assertEquals("line 2", lines.get(1).getContents());
        assertEquals("line 3 with continuation or two... or three!", lines.get(2).getContents());

        if (lines.size() > 3) {
            fail("Expected 3 lines, got "+lines.size());
        }
    }

    public void testFail() throws Exception
    {
        try {
            _readAllLines(
                    "line 1",
                    "line 2",
                    "combo... \\"
                    );
            fail("Should fail with exception");
        } catch (IOException e) {
            verifyException(e, "unexpected end-of-input when expecting line continuation");
            // also, should have expected row number too
            verifyException(e, "row 3");
            // as well as input identifier
            verifyException(e, "<test>");
        }
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */
    
    protected List<InputLine> _readAllLines(String... stuff) throws IOException
    {
        StringBuilder sb = new StringBuilder(100);
        int i = 0;

        for (String str : stuff) {
            sb.append(str);
            // try all 3 types of linefeeds
            switch (++i % 3) {
            case 0 : sb.append("\r\n"); break;
            case 2 : sb.append("\r"); break;
            default: sb.append("\n");
            }
        }
        return _readAllLines(new StringReader(sb.toString()));
    }

    protected List<InputLine> _readAllLines(Reader r) throws IOException
    {
        ArrayList<InputLine> lines = new ArrayList<>();
        InputLineReader lineReader = InputLineReader.construct("<test>", r, true);
        InputLine l;

        while ((l = lineReader.nextLine()) != null) {
            lines.add(l);
        }
        return lines;
    }
}
