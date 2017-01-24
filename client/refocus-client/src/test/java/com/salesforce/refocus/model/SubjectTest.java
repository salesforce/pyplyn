/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.refocus.model;

import com.salesforce.refocus.model.builder.SubjectBuilder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 *  Subject test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 1.0
 */
public class SubjectTest {
    List<Sample> samples = Collections.emptyList();
    Subject subject;

    @BeforeMethod
    public void setUp() throws Exception {
        subject = createSubject("name");
    }

    @Test
    public void getChildren() throws Exception {
        // ARRANGE
        Subject subj = new SubjectBuilder(subject).withChildren(Collections.singletonList(subject)).build();

        // ACT
        List<Subject> children = subj.children();

        // ASSERT
        assertThat(children, contains(subject));
    }

    @Test
    public void toStringShouldExposeName() throws Exception {
        // ACT
        String subjectString = subject.toString();

        // ASSERT
        assertThat(subjectString, containsString("name"));
        assertThat(subjectString, containsString(subject.name()));
    }

    @Test
    public void otherSettersAndGetters() throws Exception {
        // ARRANGE
        Subject subject = new Subject("id", "parentId", "name", null, null, "description", true,
                null, null, samples, Collections.singletonList(new Link("name", "url")));

        // ASSERT
        assertThat(subject.id(), equalTo("id"));
        assertThat(subject.parentId(), equalTo("parentId"));
        assertThat(subject.description(), equalTo("description"));
        assertThat(subject.absolutePath(), is(nullValue()));
        assertThat(subject.parentAbsolutePath(), is(nullValue()));
        assertThat(subject.isPublished(), equalTo(true));
        assertThat(subject.relatedLinks(), is(instanceOf(Collection.class)));
        assertThat(subject.relatedLinks().size(), equalTo(1)); // Findbugs: PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS - IGNORE
    }

    @Test
    public void subjectEquality() throws Exception {
        // ARRANGE
        Subject subject1 = new SubjectBuilder(subject).withChildren(Collections.singletonList(subject)).build();

        Subject subject2 = createSubject("subject");

        Subject subject3 = createSubject("id2", "subject");

        Subject subject4 = createSubject("id", "subject", false);

        // ASSERT
        assertThat(subject1, not(equalTo(subject2)));
        assertThat(subject1, not(equalTo(subject3)));
        assertThat(subject1, not(equalTo(subject4)));
        assertThat(subject2, not(equalTo(subject3)));
        assertThat(subject3, not(equalTo(subject4)));
        assertThat(subject1.hashCode(), not(equalTo(subject3.hashCode())));
        assertThat(subject2.hashCode(), not(equalTo(subject3.hashCode())));
        assertThat(subject3.hashCode(), not(equalTo(subject4.hashCode())));
    }

    private Subject createSubject(String name) {
        return new Subject("id", "parentId", name, null, null, "description", true,
                null, null, samples, Collections.singletonList(new Link("name", "url")));
    }

    private Subject createSubject(String id, String name) {
        return new Subject(id, "parentId", name, null, null, "description", true,
                null, null, samples, Collections.singletonList(new Link("name", "url")));
    }

    private Subject createSubject(String id, String name, boolean isPublished) {
        return new Subject(id, "parentId", name, null, null, "description", isPublished,
                null, null, samples, Collections.singletonList(new Link("name", "url")));
    }
}