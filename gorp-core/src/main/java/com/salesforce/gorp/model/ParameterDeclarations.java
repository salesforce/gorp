/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.gorp.model;

public class ParameterDeclarations
{
    private final String _types;

    public ParameterDeclarations(String types) {
        _types = types;
    }

    public int size() {
        return _types.length();
    }

    public String getTypes() {
        return _types;
    }
    
    /**
     * @param index 1-based index
     */
    public char getType(int index) {
        if ((index < 1) || (index > _types.length())) {
            throw new IllegalArgumentException("Invalid type index "+index+"; valid indexes [1.."
                    +_types.length()+"]");
        }
        return _types.charAt(index-1);
    }
}
