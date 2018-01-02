/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.util;

import java.util.Arrays;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class PaginationTest {
    @Test
    public void shouldNotCreatePaginationWithMoreThan300Records() {
        try {
            Pagination.pageStartingAt(0, 1000, 301);
        } catch (Exception e) {
            assertThat(e.getMessage(), is("The max number of perPage is [300]."));
        }
    }

    @Test
    public void shouldCreatePaginationWithLessEquals300Records() {
        try {
            Pagination.pageStartingAt(0, 1000, 300);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void shouldCreatePaginationProvidingNull() {
        try {
            Pagination.pageStartingAt(0, 1000, null);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void shouldHavePage1ForFirst() {
        Pagination pagination = Pagination.pageStartingAt(0, 1000, 10);
        assertThat(pagination.getCurrentPage(), is(1));
        assertThat(pagination.getTotalPages(), is(100));
    }

    @Test
    public void shouldHavePage1ForEndOfPage() {
        Pagination pagination = Pagination.pageStartingAt(9, 1000, 10);
        assertThat(pagination.getCurrentPage(), is(1));
    }

    @Test
    public void shouldHavePage2ForTenth() {
        Pagination pagination = Pagination.pageStartingAt(10, 1000, 10);
        assertThat(pagination.getCurrentPage(), is(2));
    }

    @Test
    public void shouldHaveOffsetForPreviousPage() {
        Pagination pagination = Pagination.pageStartingAt(70, 1000, 10);
        assertThat(pagination.getCurrentPage(), is(8));
        assertThat(pagination.getPreviousPage(), is(7));
        assertThat(pagination.getPreviousOffset(), is(60));
        assertThat(pagination.getNextPage(), is(9));
        assertThat(pagination.getNextOffset(), is(80));
        assertThat(pagination.hasNextPage(), is(true));
        assertThat(pagination.hasPreviousPage(), is(true));
    }

    @Test
    public void shouldHaveOffsetsForFinalPage() {
        Pagination pagination = Pagination.pageStartingAt(11, 16, 10);
        assertThat(pagination.getCurrentPage(), is(2));
        assertThat(pagination.hasNextPage(), is(false));
        assertThat(pagination.hasPreviousPage(), is(true));
    }

    @Test
    public void shouldReturnHasPreviousAndNextPages() {
       Pagination pagination = Pagination.pageStartingAt(10, 16, 10);
       assertThat(pagination.hasPreviousPage(), is(true));
       assertThat(pagination.hasNextPage(), is(false));
    }

    @Test
    public void shouldHaveOffsetsForFirstPage() {
        Pagination pagination = Pagination.pageStartingAt(0, 16, 10);
        assertThat(pagination.getCurrentPage(), is(1));
        assertThat(pagination.hasNextPage(), is(true));
        assertThat(pagination.hasPreviousPage(), is(false));
    }

    @Test
    public void shouldFindPageForAGivenOffset() {
        assertThat(Pagination.pageFor(0, 10, 3), is(Pagination.pageStartingAt(0, 10, 3)));
        assertThat(Pagination.pageFor(1, 10, 3), is(Pagination.pageStartingAt(0, 10, 3)));
        assertThat(Pagination.pageFor(2, 10, 3), is(Pagination.pageStartingAt(0, 10, 3)));
        assertThat(Pagination.pageFor(3, 10, 3), is(Pagination.pageStartingAt(3, 10, 3)));
        assertThat(Pagination.pageFor(4, 10, 3), is(Pagination.pageStartingAt(3, 10, 3)));
        assertThat(Pagination.pageFor(5, 10, 3), is(Pagination.pageStartingAt(3, 10, 3)));
        assertThat(Pagination.pageFor(6, 10, 3), is(Pagination.pageStartingAt(6, 10, 3)));
    }

    @Test
    public void shouldFindPageForAGivenPage() {
        assertThat(Pagination.pageByNumber(1, 10, 3), is(Pagination.pageStartingAt(0, 10, 3)));
        assertThat(Pagination.pageByNumber(2, 10, 3), is(Pagination.pageStartingAt(3, 10, 3)));
        assertThat(Pagination.pageByNumber(3, 10, 3), is(Pagination.pageStartingAt(6, 10, 3)));
    }

    @Test
    public void shouldUnderstandFirstPage() {
        assertThat(Pagination.pageStartingAt(3, 10, 3).getFirstPage(), is(1));
        assertThat(Pagination.pageStartingAt(6, 10, 3).getFirstPage(), is(1));
    }

    @Test
    public void shouldUnderstandLastPage() {
        assertThat(Pagination.pageStartingAt(3, 10, 3).getLastPage(), is(4));
        assertThat(Pagination.pageStartingAt(6, 10, 3).getLastPage(), is(4));
        assertThat(Pagination.pageStartingAt(0, 2, 3).getLastPage(), is(1));
        assertThat(Pagination.pageStartingAt(2, 3, 3).getLastPage(), is(1));
    }

    @Test
    public void shouldReturnAllPagesIfLessThan9() {
        assertThat(Pagination.pageByNumber(1, 8, 1).getPages(), is(Arrays.asList(Pagination.currentPage(1), Pagination.page(2), Pagination.page(3), Pagination.page(4), Pagination.page(5), Pagination.page(6), Pagination.page(7), Pagination.page(8), Pagination.page(2, "next"))));
        assertThat(Pagination.pageByNumber(1, 3, 1).getPages(), is(Arrays.asList(Pagination.currentPage(1), Pagination.page(2), Pagination.page(3), Pagination.page(2, "next"))));
    }

    @Test
    public void shouldShowDotsIfMoreThan8Pages() {
        assertThat(Pagination.pageByNumber(5, 9, 1).getPages(), is(Arrays.asList(Pagination.page(4, "prev"), Pagination.page(1), Pagination.PageNumber.DOTS, Pagination.page(3), Pagination.page(4), Pagination.currentPage(5), Pagination.page(6), Pagination.page(7), Pagination.PageNumber.DOTS, Pagination.page(9), Pagination.page(6, "next"))));
        assertThat(Pagination.pageByNumber(5, 100, 1).getPages(), is(Arrays.asList(Pagination.page(4, "prev"), Pagination.page(1), Pagination.PageNumber.DOTS, Pagination.page(3), Pagination.page(4), Pagination.currentPage(5), Pagination.page(6), Pagination.page(7), Pagination.PageNumber.DOTS, Pagination.page(100), Pagination.page(6, "next"))));
    }

    @Test
    public void shouldShowDotsAtEndIfOnFirst4Pages() {
        assertThat(Pagination.pageByNumber(1, 100, 1).getPages(), is(Arrays.asList(Pagination.currentPage(1), Pagination.page(2), Pagination.page(3), Pagination.PageNumber.DOTS, Pagination.page(100), Pagination.page(2, "next"))));
        assertThat(Pagination.pageByNumber(2, 100, 1).getPages(), is(Arrays.asList(Pagination.page(1, "prev"), Pagination.page(1), Pagination.currentPage(2), Pagination.page(3), Pagination.page(4), Pagination.PageNumber.DOTS, Pagination.page(100), Pagination.page(3, "next"))));
        assertThat(Pagination.pageByNumber(3, 100, 1).getPages(), is(Arrays.asList(Pagination.page(2, "prev"), Pagination.page(1), Pagination.page(2), Pagination.currentPage(3), Pagination.page(4), Pagination.page(5), Pagination.PageNumber.DOTS, Pagination.page(100), Pagination.page(4, "next"))));
        assertThat(Pagination.pageByNumber(4, 100, 1).getPages(), is(Arrays.asList(Pagination.page(3, "prev"), Pagination.page(1), Pagination.page(2), Pagination.page(3), Pagination.currentPage(4), Pagination.page(5), Pagination.page(6), Pagination.PageNumber.DOTS, Pagination.page(100), Pagination.page(5, "next"))));
    }

    @Test
    public void shouldShowDotsAtStartIfOnLast4Pages() {
        assertThat(Pagination.pageByNumber(97, 100, 1).getPages(), is(Arrays.asList(Pagination.page(96, "prev"), Pagination.page(1), Pagination.PageNumber.DOTS, Pagination.page(95), Pagination.page(96), Pagination.currentPage(97), Pagination.page(98), Pagination.page(99), Pagination.page(100), Pagination.page(98, "next"))));
        assertThat(Pagination.pageByNumber(98, 100, 1).getPages(), is(Arrays.asList(Pagination.page(97, "prev"), Pagination.page(1), Pagination.PageNumber.DOTS, Pagination.page(96), Pagination.page(97), Pagination.currentPage(98), Pagination.page(99), Pagination.page(100), Pagination.page(99, "next"))));
        assertThat(Pagination.pageByNumber(99, 100, 1).getPages(), is(Arrays.asList(Pagination.page(98, "prev"), Pagination.page(1), Pagination.PageNumber.DOTS, Pagination.page(97), Pagination.page(98), Pagination.currentPage(99), Pagination.page(100), Pagination.page(100, "next"))));
        assertThat(Pagination.pageByNumber(100, 100, 1).getPages(), is(Arrays.asList(Pagination.page(99, "prev"), Pagination.page(1), Pagination.PageNumber.DOTS, Pagination.page(98), Pagination.page(99), Pagination.currentPage(100))));
    }

    @Test
    public void shouldUnderstandDotsPageNumber() {
        assertThat(Pagination.PageNumber.DOTS.isDots(), is(true));
        assertThat(Pagination.page(1).isDots(), is(false));
    }

    @Test
    public void shouldUnderstandPageNumberAndLabel() {
        assertThat(Pagination.PageNumber.DOTS.getLabel(), is("..."));
        assertThat(Pagination.PageNumber.DOTS.getNumber(), is(-1));
        assertThat(Pagination.page(5).getLabel(), is("5"));
        assertThat(Pagination.page(5).getNumber(), is(5));
        assertThat(Pagination.page(10, "foo").getLabel(), is("foo"));
        assertThat(Pagination.page(10, "foo").getNumber(), is(10));
    }

    @Test
    public void shouldUnderstandIfCurrent() {
        assertThat(Pagination.currentPage(5).isCurrent(), is(true));
        assertThat(Pagination.page(5).isCurrent(), is(false));
    }

}
