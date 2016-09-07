/* 
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.gorp.model;

public interface DefPieceContainer
    extends DefPieceAppendable
{
    public String getName();

    public void appendLiteralPattern(String literal, int offset);
    public void appendLiteralText(String literal, int offset);

    public void appendPatternRef(String name, int offset);
    public TemplateReference appendTemplateRef(String name, int offset);

    public void appendTemplateVariable(String parentId, int varPos, int offset);

    public ExtractorExpression appendExtractor(String name, int offset);

    /**
     * Method for appending extractor whose name is not known, but will be
     * passed in as a template name parameter (variable)
     */
    public ExtractorExpression appendVariableExtractor(int varPos, int offset);
}

