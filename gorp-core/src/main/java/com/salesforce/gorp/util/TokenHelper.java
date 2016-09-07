/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.gorp.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.salesforce.gorp.DefinitionParseException;
import com.salesforce.gorp.io.InputLine;

public class TokenHelper
{
    protected final static Pattern P_KEYWORD_REST = Pattern.compile("\\s*(\\w*)\\s*(.*)");

    /*
    /**********************************************************************
    /* Simple skipping, matching
    /**********************************************************************
     */
    
    public static StringAndOffset findKeyword(String contents, int offset)
    {
        Matcher m = P_KEYWORD_REST.matcher(contents);
        if (!m.matches()) {
            return null;
        }
        // Need to retain offsets for error reporting
        return new StringAndOffset(m.group(1), m.start(2));
    }

    /**
     * Helper method to call to find expected type marker character, optionally preceded
     * by ignorable whitespace.
     *
     * @return Index of the marker character, if one found; -1 if not
     */
    public static int findTypeMarker(char marker, String contents, int ix)
    {
        for (int end = contents.length(); ix < end; ++ix) {
            char c = contents.charAt(ix);
            if (c == marker) {
                return ix;
            }
            if (isWS(c)) {
                break;
            }
        }
        return -1;
    }

    /**
     * Helper method to see if we are pointing to a simple "()" construct at
     * current input offset; and if so, skip it, returning positive integer;
     * otherwise returning -1
     */
    public static int skipEmptyParens(String contents, int ix)
    {
        final int end = contents.length();
        if (((ix + 1) < end)
            && (contents.charAt(ix) == '(')
            && (contents.charAt(ix+1) == ')')) {
                return ix+2;
        }
        return -1;
    }

    public static int skipSpace(String contents, int ix) {
        final int end = contents.length();

        for (; ix < end; ++ix) {
            if (!isWS(contents.charAt(ix))) {
                break;
            }
        }
        return ix;
    }

    public static int matchRemaining(String contents, int ix, char charToMatch)
    {
        boolean found = false;
        final int end = contents.length();

        while (ix < end) {
            char c = contents.charAt(ix++);
            if (c == charToMatch) {
                if (found) {
                    break;
                }
                found = true;
            } else if (!isWS(c)) {
                break;
            }
        }
        return found ? ix : -1;
    }

    /*
    /**********************************************************************
    /* Main parse methods
    /**********************************************************************
     */

    /**
     *<p>
     * NOTE: assumption is that there is no whitespace to skip, that is, the name
     * starts from the first character position.
     */
    public static StringAndOffset parseNameAndSkipSpace(String type, InputLine inputLine,
            String contents, int ix) throws DefinitionParseException
    {
        StringAndOffset offset = parseName(type, inputLine, contents, ix, false);
        // and skip whitespace, if any
        int restStart = offset.restOffset;
        final int end = contents.length();

        // having nothing is ok (end-of-line)
        if (restStart >= end) {
            return offset;
        }
        // otherwise must have something
        if (!isWS(contents.charAt(restStart))) {
              return inputLine.reportError(restStart, "Missing space character after %s name '%s'",
                      type, offset.match);
        }
        while ((++restStart < end) && isWS(contents.charAt(restStart))) {
            ;
        }
        return offset.withOffset(restStart);
    }

    /**
     *<p>
     * NOTE: assumption is that there is no whitespace to skip, that is, the name
     * starts from the first character position.
     */
    public static StringAndOffset parseName(String type, InputLine inputLine,
            String contents, int ix, boolean allowNumbers) throws DefinitionParseException
    {
        // Two options: quoted, unquoted; and two types of quotes as well
        final int end = contents.length();
        if (ix >= end) {
            inputLine.reportError(end, "Missing %s name", type);
        }

        int nameStart, restStart;
        String name = null;

        char c = contents.charAt(ix);
        if (c == '"' || c == '\'') {
            ++ix;
            int q = contents.indexOf(c, ix);
            if (q < 0) { // should give better error message
                return inputLine.reportError(end, "Missing closing quote ('%c') for %s name",
                        c, type);
            }
            nameStart = ix;
            name = contents.substring(nameStart, q);
            restStart = q+1;
        } else if (!Character.isJavaIdentifierStart(c)) { // should give better error message
            // One more thing; may be misplaced variable?
            if (isNumber(c)) {
                if (allowNumbers) {
                    nameStart = ix;
                    while ((ix < end) && isNumber(contents.charAt(ix))) {
                        ++ix;
                    }
                    name = contents.substring(nameStart, ix);
                } else {
                    return inputLine.reportError(ix,
                            "Invalid variable reference instead of %s name: can not use variable references here (missing parenthesis after template name?)",
                            type);
                }
            }
            restStart = ix;
        } else {
            nameStart = ix;
            while (++ix < end) {
                c = contents.charAt(ix);
                if (!_isNonLeadingNameChar(c)) {
                    break;
                }
            }
            name = contents.substring(nameStart, ix);
            restStart = ix;
        }
        return new StringAndOffset(name, restStart);
    }

    /**
     * Helper method for finding closing '}', taking into account possible escaping,
     * nested {...} constructs.
     */
    public static StringAndOffset parseInlinePattern(InputLine inputLine,
            String contents, int start) throws DefinitionParseException
    {
        final int end = contents.length();
        int nesting = 1;
        int i = start;

        while (i < end) {
            char c = contents.charAt(i++);
            if (c == '\\') { // skip anything that is escaped
                ++i;
                continue;
            }
            if (c == '{') {
                ++nesting;
            } else if (c == '}') {
                if (--nesting == 0) {
                    return new StringAndOffset(contents.substring(start, i-1), i);
                }
            }
        }

        // should not get here
        return inputLine.reportError(start, "Missing closing '{' for inline pattern");
    }

    /*
    /**********************************************************************
    /* Error reporting helpers
    /**********************************************************************
     */
    
    public static String charDesc(char c) {
        if ((c < 0x0020) || Character.isISOControl(c)) {
            return String.format("code 0x%04x", (int) c);
        }
        return String.format("'%c' (code 0x%04x)", c, (int) c);
    }
    
    /*
    /**********************************************************************
    /* Internal helper methods
    /**********************************************************************
     */

    public static int parseIfNonNegativeNumber(String str) {
        final int end = str.length();
        if (end == 0) {
            return -1;
        }
        int num = 0;
        for (int i = 0; i < end; ++i) {
            char c = str.charAt(i);
            if (!isNumber(c)) {
                return -1;
            }
            num = (num * 10) + (c - '0');
        }
        return num;
    }

    //  could use full JDK approach, with extra whitespace; but for now just allow ASCII whitespace
    public static boolean isWS(char ch) {
        return (ch <= ' ');
    }

    public static boolean isNumber(char ch) {
        return (ch <= '9') && (ch >= '0');
    }
    
    private static boolean _isNonLeadingNameChar(char c) {
        // 24-Nov-2015, as per [issue#2], do NOT include hyphen as valid
        /*
        if (c == '-') {
            return true;
        }
        */
        return Character.isJavaIdentifierPart(c);
    }
}
