/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.gorp.util;

public class RegexHelper
{
    public final static String CHAR_CLASS_AUTOMATON_d = "0-9"  ;
    public final static String CHAR_CLASS_AUTOMATON_s = " \b\f\n\r\t";
    public final static String CHAR_CLASS_AUTOMATON_w = "a-zA-Z_0-9";

    public static String quoteLiteralAsRegexp(String text)
    {
        final int end = text.length();
        
        StringBuilder sb = new StringBuilder(end + 8);

        for (int i = 0; i < end; ) {
            char c = text.charAt(i++);

            switch (c) {
            case ' ': // one of few special cases: collate, collapse into "one or more" style regexp
            case '\t':
                while ((i < end) && text.charAt(i) <= ' ') {
                    ++i;
                }
                sb.append("[ \t]+");
                break;

            case '.':
                sb.append("\\.");
                break;

                // Looks like we need to match not just open, but close parenthesis; probably same for others
            case '(':
            case ')':
            case '[':
            case ']':
            case '\\':
            case '{':
            case '}':
            case '|':
            case '*':
            case '?':
            case '+':
            case '$':
            case '^':
                // Automaton has heartburn with less-than
            case '<':
            case '>':
                // as well as with quoted entries (how about single quotes?)
            case '"':
                // and some other operators
            case '&':
                sb.append('\\');
                sb.append(c);
                break;
                
            default:
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // for tests
    protected static String massageRegexpForAutomaton(String pattern) {
        StringBuilder sb = new StringBuilder();
        massageRegexpForAutomaton(pattern, sb);
        return sb.toString();
    }
    
    public static void massageRegexpForAutomaton(String pattern, StringBuilder sb)
    {
        /* Oddities with RegExp by Automaton:
         *
         * - Backslash quote ONLY works for literal character
         *    + no pre-defined ones for control chars (like linefeed with `\n`, tab etc)
         *    + no pre-defined character classes like `\s` or `\w`
         *
         * So let's catch all "backslash-alphanum" cases, and either convert or
         * throw exception
         */
        // Anything to really quote for Automaton? Could perhaps translate some
        // named escapes?
        if (pattern.indexOf('\\') < 0) {
            sb.append(pattern);
            return;
        }
        final int end = pattern.length();
        // need to keep track of character classes, so...
        int bracketLevels = 0;

        main_loop:
        for (int i = 0; i < end; ) {
            char c = pattern.charAt(i++);

            switch (c) {
            case '[':
                sb.append(c);
                ++bracketLevels;
                continue main_loop;
            case ']':
                sb.append(c);
                --bracketLevels;
                continue main_loop;
            case '\\':
                if (i < end) {
                    break;
                }
                // could catch orphan backslash, but for now pass to parser as-is
            default:
                sb.append(c);
                continue main_loop;
            }

            // If character class, need to know whether to enclose in brackets or not
            boolean hadBracket = (bracketLevels > 0) && (pattern.charAt(i-2) == '[');
            char d = pattern.charAt(i++);

            switch (d) {
            // First, well-known replacements
            case '\\': // fine as-is
                break;

            // Simple control-chars
            case 'b':
                d = '\b';
                break;
            case 'f':
                d = '\f';
                break;
            case 'n':
                d = '\n';
                break;
            case 'r':
                d = '\r';
                break;
            case 't':
                d = '\t';
                break;
                
            // "Well-known" character classes
            // (let's hope simple inclusion works, like with JDK Regexps)
            case 'd':
                _appendCharClass(sb, d, hadBracket, bracketLevels, CHAR_CLASS_AUTOMATON_d);
                continue main_loop;
            case 'D':
                _appendCharClass(sb, d, hadBracket, bracketLevels, "^"+CHAR_CLASS_AUTOMATON_d);
                continue main_loop;
            case 's':
                _appendCharClass(sb, d, hadBracket, bracketLevels, CHAR_CLASS_AUTOMATON_s);
                continue main_loop;
            case 'S':
                _appendCharClass(sb, d, hadBracket, bracketLevels, "^"+CHAR_CLASS_AUTOMATON_s);
                continue main_loop;
            case 'w':
                _appendCharClass(sb, d, hadBracket, bracketLevels, CHAR_CLASS_AUTOMATON_w);
                continue main_loop;
            case 'W':
                _appendCharClass(sb, d, hadBracket, bracketLevels, "^"+CHAR_CLASS_AUTOMATON_w);
                continue main_loop;
                
            default: // unknown; only ok if NOT alphanumeric
                if (Character.isAlphabetic(d) || Character.isDigit(d)) {
                    throw new IllegalArgumentException("Unrecognized backslash escape '\\"
                            +d+"; can only escape backslash (\\\\),"
                            +" use known control-codes (\\n, \\r, \\t),"
                            +" escape non-alphanumeric (\\$, \\(, ...) or refer to"
                            +" a 'well-known' character class (\\s, \\S, \\d, \\D, \\w, \\W)");
                }
            }
            sb.append(c);
            sb.append(d);
        }
    }

    private static void _appendCharClass(StringBuilder sb, char charClass,
            boolean hadBracket, int bracketNesting,
            String chars) {
        // First things first; if no nesting, surround with brackets
        if (bracketNesting == 0) {
            sb.append('[');
            sb.append(chars);
            sb.append(']');
            return;
        }
        // second: if within nesting, will not append, but will have to
        // fail if we try to use negation AND we did not just have bracket
        if (chars.startsWith("^") && !hadBracket) {
            throw new IllegalArgumentException("Can not use negated character class \\"
                    +charClass+" within character class in position other than first (Automaton limitation)");
        }
        sb.append(chars);
    }

    // for tests
    protected static String massageRegexpForJDK(String pattern) {
        StringBuilder sb = new StringBuilder();
        massageRegexpForJDK(pattern, sb);
        return sb.toString();
    }
    
    public static void massageRegexpForJDK(String pattern, StringBuilder sb)
    {
        // With "regular" regexps need to avoid capturing groups, and for that
        // need to copy backslash escapes verbatim
        final int end = pattern.length();

        for (int i = 0; i < end; ) {
            char c = pattern.charAt(i++);

            switch (c) {
            case '\\':
                sb.append(c);
                // copy escaped, unless we are at end; end is probably an error condition
                // but for now let's not care, should be caught by regexp parser if necessary
                if (i < end) {
                    sb.append(pattern.charAt(i++));
                }
                break;
                
            case '(':
                // change to non-capturing
                sb.append("(?:");
                break;
            default:
                sb.append(c);
            }
        }
    }
    
}
