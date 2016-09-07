/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.gorp.model;

/**
 * Add-on interface for things onto which {@link DefPiece}s
 * may be appended; typically cooked definitions.
 */
public interface DefPieceAppendable {
    public String getName();
    public Iterable<DefPiece> getParts();

    public void append(DefPiece part);
}
