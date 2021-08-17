/*
 * Copyright 2021 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.domain.feed;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class AuthorTest {
    @Test
    public void shouldBeEqualToAnotherAuthorWithSameNameAndEmail(){
        Author author1 = new Author("name", "email");
        Author author2 = new Author("name", "email");
        assertThat(author1.equals(author2), is(true));
    }

    @Test
    public void shouldNotBeEqualToAnotherMingleCardWithSameNameButDifferentEmail(){
        Author author1 = new Author("name", "email1");
        Author author2 = new Author("name", "email2");
        assertThat(author1.equals(author2), is(false));
    }

    @Test
    public void shouldNotBeEqualToAnotherMingleCardWithDifferentNameButSameEmail(){
        Author author1 = new Author("name1", "email");
        Author author2 = new Author("name2", "email");
        assertThat(author1.equals(author2), is(false));
    }

    @Test
    public void shouldNotBeEqualToNull(){
        assertNotEquals(new Author("name1", "email"), null);
    }
}
