package com.salesforce.gorp.autom;

import org.junit.Assert;

import com.salesforce.gorp.TestBase;
import com.salesforce.gorp.autom.PolyMatcher;

public class MultiPatternTest extends TestBase
{
    public void testSimpleStrings() {
        PolyMatcher multiPatternMatcher = PolyMatcher.create(
                "ab+",     // 0
                "abc+",    // 1
                "ab?c",    // 2
                "v",       // 3
                "v.*",     // 4
                "(def)+"   // 5
                );

        _verifyMatch(multiPatternMatcher, "ab", 0);
        _verifyMatch(multiPatternMatcher, "abc", 1, 2);
        _verifyMatch(multiPatternMatcher, "ac", 2);
        _verifyMatch(multiPatternMatcher, "");
        _verifyMatch(multiPatternMatcher, "v", 3, 4);
        _verifyMatch(multiPatternMatcher, "defdef", 5);
        _verifyMatch(multiPatternMatcher, "defde");
        _verifyMatch(multiPatternMatcher, "abbbbb", 0);
    }

    private void _verifyMatch(PolyMatcher matcher, String str, int... vals) {
        Assert.assertArrayEquals(vals, matcher.match(str));
    }
}
