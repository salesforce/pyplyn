/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.processor;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.List;

import org.testng.annotations.Test;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since ${project.version}
 */
public class FilterableTest {
    @Test
    public void testFilter() throws Exception {
        // ARRANGE
        List<BaseType> baseTypes = asList(new TypeA(), new TypeB());
        TypeAFilter filter = new TypeAFilter();

        // ACT
        List<TypeA> filtered = filter.filter(baseTypes);

        // ASSERT
        assertThat("Resulting list should contain TypeA", filtered, hasItem(instanceOf(TypeA.class)));
        assertThat("Resulting list should not contain TypeB", filtered, not(hasItem(instanceOf(TypeB.class))));
    }

    private interface BaseType {}

    private static class TypeA implements BaseType {
    }

    private static class TypeB implements BaseType {
    }

    private static class TypeAFilter implements Filterable<TypeA, BaseType> {
        @Override
        public Class<TypeA> filteredType() {
            return TypeA.class;
        }
    }
}