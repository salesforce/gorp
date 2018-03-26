/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.gorp.autom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;

/**
 * Helper class for constructing actual {@link Automata} from multiple pattern definitions
 * which at this point are parseable by `Automaton` package.
 */
public class PolyMatcher
{
    private final int[] NO_MATCH = {};
    private final Automata automata;
    
    /**
     * Most of the extra flags are not applicable, partly since they
     * use syntax that is different "standard" regexp, and as such would
     * otherwise need to be translated. Extra operators do not seem particularly
     * useful either, so removing them should make things bit safer and
     * possibly more efficient.
     */
    private final static int FLAGS = RegExp.NONE;

    protected PolyMatcher(Automata a) {
        automata = a;
    }

    public static PolyMatcher create(String... patterns) {
        return create(Arrays.asList(patterns));
    }

    public static PolyMatcher create(List<String> patterns) {
        return new PolyMatcher(createAutomaton(patterns));
    }

    private static Automata createAutomaton(List<String> patterns) {
        final List<Automaton> automata = new ArrayList<>();
        for (String ptn: patterns) {
            try {
                Automaton automaton = new RegExp(ptn, FLAGS).toAutomaton();
                automaton.minimize();
                automata.add(automaton);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid regexp, "+e.getMessage()+", source: "+_printablePattern(ptn));
            }
        }
        return Automata.construct(automata);
    }

    private static String _printablePattern(String src)
    {
        final int end = src.length();
        StringBuilder sb = new StringBuilder(10 + end);
        for (int i = 0; i < end; ++i) {
            char c = src.charAt(i);
            if (c < 0x0020) {
                switch (c) {
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(String.format("\\U%04x", (int) c));
                    break;
                }
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }
    
    /**
     * @return Indexes of all patterns that matched.
     */
    public int[] match(CharSequence s) {
        int p = 0;
        final int l = s.length();
        for (int i = 0; i < l; ++i) {
            p = automata.step(p, s.charAt(i));
            if (p == -1) {
                return NO_MATCH;
            }
        }
        return automata.accept(p);
    }
}
