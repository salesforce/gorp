/*
 * Copyright (c) 2013 Paul Masurel
 *
 * Licensed under the MIT license.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
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

import dk.brics.automaton.State;

// Note: Originally copied from Multiregexp package (https://github.com/fulmicoton/multiregexp)
class PolyState
{
    private final State[] states;

    public PolyState(State[] states) {
        this.states = states;
    }

    public boolean isNull() {
        for (State state: this.states) {
            if (state != null) {
                return false;
            }
        }
        return true;
    }

    public PolyState step(char token) {
        State[] nextStates = new State[states.length];
        for (int c = 0, clen = states.length; c < clen; ++c) {
            State prevState = states[c];
            nextStates[c] = (prevState == null) ? null : prevState.step(token);
        }
        return new PolyState(nextStates);
    }

    public int[] toAcceptValues() {
        List<Integer> acceptValues = new ArrayList<>();
        for (int stateId = 0, stateCount = states.length; stateId < stateCount; ++stateId) {
            State curState = this.states[stateId];
            if ((curState != null) && (curState.isAccept())) {
                acceptValues.add(stateId);
            }
        }
        int[] acceptValuesArr = new int[acceptValues.size()];
        for (int c=0; c<acceptValues.size(); c++) {
            acceptValuesArr[c] = acceptValues.get(c);
        }
        return acceptValuesArr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PolyState that = (PolyState) o;
        return Arrays.equals(states, that.states);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(states);
    }
}
