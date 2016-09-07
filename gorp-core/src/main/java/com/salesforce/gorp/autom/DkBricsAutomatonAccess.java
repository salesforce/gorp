/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.gorp.autom;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dk.brics.automaton.Automaton;

/**
 * Helper class only needed to get around access restrictions, namely that
 * of <code>Automaton</code> not exposing its start points information.
 * Implementation is brittle, but the only alternative would have issues
 * with OSGi.
 */
public class DkBricsAutomatonAccess
{
    private final static DkBricsAutomatonAccess instance = new DkBricsAutomatonAccess();

    private final Method _gspMethod;
    
    private DkBricsAutomatonAccess() {
        Method m = null;
        try {
            m = Automaton.class.getDeclaredMethod("getStartPoints");
        } catch (NoSuchMethodException | SecurityException e) {
            throw new RuntimeException("Internal error: failed to access `getStartPoints()`: "+e.getMessage(), e);
        }
        try {
            m.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Internal error: failed to make `getStartPoints()` accessible: "+e.getMessage(), e);
        }
        _gspMethod = m;
    }

    private char[] startPoints(Automaton a) {
        try {
            return (char[]) _gspMethod.invoke(a);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException("Internal error: failed to call `getStartPoints()`: "+e.getMessage(), e);
        }
    }
    
    public static char[] getStartPoints(Automaton a) {
        return instance.startPoints(a);
    }
}
