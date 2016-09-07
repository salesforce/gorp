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

// Note: Copied from Multiregexp package
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
