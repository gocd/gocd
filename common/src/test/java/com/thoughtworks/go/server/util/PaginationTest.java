/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.server.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.thoughtworks.go.server.util.Pagination.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PaginationTest {
    @Test
    public void shouldNotCreatePaginationWithMoreThan300Records() {
        assertThatThrownBy(() -> pageByOffset(0, 1000, 301))
            .hasMessageContaining("The max number of perPage is [300].");
    }

    @Test
    public void shouldCreatePaginationWithLessEquals300Records() {
        Pagination pagination = pageByOffset(0, 1000, 300);
        assertThat(pagination.getPageSize()).isEqualTo(300);

        assertThat(pagination.getCurrentPage()).isEqualTo(1);
        assertThat(pagination.getTotalPages()).isEqualTo(3); // This seems wrong - aren't there 4 pages? Matching old behaviour.
    }

    @Test
    public void shouldCreatePaginationProvidingNulls() {
        Pagination pagination = pageByOffsetNullSafe(null, null, null);
        assertThat(pagination.getOffset()).isEqualTo(0);
        assertThat(pagination.getPageSize()).isEqualTo(10);
        assertThat(pagination.getTotal()).isEqualTo(0);

        assertThat(pagination.getCurrentPage()).isEqualTo(1);
        assertThat(pagination.getTotalPages()).isEqualTo(0);
    }

    @Test
    public void shouldHavePage1ForFirst() {
        Pagination pagination = pageByOffset(0, 1000, 10);
        assertThat(pagination.getCurrentPage()).isEqualTo(1);
        assertThat(pagination.getTotalPages()).isEqualTo(100);
    }

    @Test
    public void shouldHavePage1ForEndOfPage() {
        Pagination pagination = pageByOffset(9, 1000, 10);
        assertThat(pagination.getCurrentPage()).isEqualTo(1);
    }

    @Test
    public void shouldHavePage2ForTenth() {
        Pagination pagination = pageByOffset(10, 1000, 10);
        assertThat(pagination.getCurrentPage()).isEqualTo(2);
    }

    @Test
    public void shouldHaveOffsetForPreviousPage() {
        Pagination pagination = pageByOffset(70, 1000, 10);
        assertThat(pagination.getCurrentPage()).isEqualTo(8);
        assertThat(pagination.getPreviousPage()).isEqualTo(7);
        assertThat(pagination.getPreviousOffset()).isEqualTo(60);
        assertThat(pagination.getNextPage()).isEqualTo(9);
        assertThat(pagination.getNextOffset()).isEqualTo(80);
        assertThat(pagination.hasNextPage()).isTrue();
        assertThat(pagination.hasPreviousPage()).isTrue();
    }

    @Test
    public void shouldHaveOffsetsForFinalPage() {
        Pagination pagination = pageByOffset(11, 16, 10);
        assertThat(pagination.getCurrentPage()).isEqualTo(2);
        assertThat(pagination.hasNextPage()).isFalse();
        assertThat(pagination.hasPreviousPage()).isTrue();
    }

    @Test
    public void shouldReturnHasPreviousAndNextPages() {
        Pagination pagination = pageByOffset(10, 16, 10);
        assertThat(pagination.hasPreviousPage()).isTrue();
        assertThat(pagination.hasNextPage()).isFalse();
    }

    @Test
    public void shouldHaveOffsetsForFirstPage() {
        Pagination pagination = pageByOffset(0, 16, 10);
        assertThat(pagination.getCurrentPage()).isEqualTo(1);
        assertThat(pagination.hasNextPage()).isTrue();
        assertThat(pagination.hasPreviousPage()).isFalse();
    }

    @Test
    public void shouldFindPageForAGivenOffset() {
        assertThat(pageByItemNumber(0, 10, 3)).isEqualTo(pageByOffset(0, 10, 3));
        assertThat(pageByItemNumber(1, 10, 3)).isEqualTo(pageByOffset(0, 10, 3));
        assertThat(pageByItemNumber(2, 10, 3)).isEqualTo(pageByOffset(0, 10, 3));
        assertThat(pageByItemNumber(3, 10, 3)).isEqualTo(pageByOffset(3, 10, 3));
        assertThat(pageByItemNumber(4, 10, 3)).isEqualTo(pageByOffset(3, 10, 3));
        assertThat(pageByItemNumber(5, 10, 3)).isEqualTo(pageByOffset(3, 10, 3));
        assertThat(pageByItemNumber(6, 10, 3)).isEqualTo(pageByOffset(6, 10, 3));
    }

    @Test
    public void shouldFindPageForAGivenPage() {
        assertThat(Pagination.pageByNumber(1, 10, 3)).isEqualTo(pageByOffset(0, 10, 3));
        assertThat(Pagination.pageByNumber(2, 10, 3)).isEqualTo(pageByOffset(3, 10, 3));
        assertThat(Pagination.pageByNumber(3, 10, 3)).isEqualTo(pageByOffset(6, 10, 3));
    }

    @Test
    public void shouldUnderstandFirstPage() {
        assertThat(pageByOffset(3, 10, 3).getFirstPage()).isEqualTo(1);
        assertThat(pageByOffset(6, 10, 3).getFirstPage()).isEqualTo(1);
    }

    @Test
    public void shouldUnderstandLastPage() {
        assertThat(pageByOffset(3, 10, 3).getLastPage()).isEqualTo(4);
        assertThat(pageByOffset(6, 10, 3).getLastPage()).isEqualTo(4);
        assertThat(pageByOffset(0, 2, 3).getLastPage()).isEqualTo(1);
        assertThat(pageByOffset(2, 3, 3).getLastPage()).isEqualTo(1);
    }

    @Test
    public void shouldReturnAllPagesIfLessThan9() {
        assertThat(Pagination.pageByNumber(1, 8, 1).getPages()).isEqualTo(List.of(Pagination.currentPage(1), Pagination.page(2), Pagination.page(3), Pagination.page(4), Pagination.page(5), Pagination.page(6), Pagination.page(7), Pagination.page(8), Pagination.page(2, "next")));
        assertThat(Pagination.pageByNumber(1, 3, 1).getPages()).isEqualTo(List.of(Pagination.currentPage(1), Pagination.page(2), Pagination.page(3), Pagination.page(2, "next")));
    }

    @Test
    public void shouldShowDotsIfMoreThan8Pages() {
        assertThat(Pagination.pageByNumber(5, 9, 1).getPages()).isEqualTo(List.of(Pagination.page(4, "prev"), Pagination.page(1), Pagination.PageNumber.DOTS, Pagination.page(3), Pagination.page(4), Pagination.currentPage(5), Pagination.page(6), Pagination.page(7), Pagination.PageNumber.DOTS, Pagination.page(9), Pagination.page(6, "next")));
        assertThat(Pagination.pageByNumber(5, 100, 1).getPages()).isEqualTo(List.of(Pagination.page(4, "prev"), Pagination.page(1), Pagination.PageNumber.DOTS, Pagination.page(3), Pagination.page(4), Pagination.currentPage(5), Pagination.page(6), Pagination.page(7), Pagination.PageNumber.DOTS, Pagination.page(100), Pagination.page(6, "next")));
    }

    @Test
    public void shouldShowDotsAtEndIfOnFirst4Pages() {
        assertThat(Pagination.pageByNumber(1, 100, 1).getPages()).isEqualTo(List.of(Pagination.currentPage(1), Pagination.page(2), Pagination.page(3), Pagination.PageNumber.DOTS, Pagination.page(100), Pagination.page(2, "next")));
        assertThat(Pagination.pageByNumber(2, 100, 1).getPages()).isEqualTo(List.of(Pagination.page(1, "prev"), Pagination.page(1), Pagination.currentPage(2), Pagination.page(3), Pagination.page(4), Pagination.PageNumber.DOTS, Pagination.page(100), Pagination.page(3, "next")));
        assertThat(Pagination.pageByNumber(3, 100, 1).getPages()).isEqualTo(List.of(Pagination.page(2, "prev"), Pagination.page(1), Pagination.page(2), Pagination.currentPage(3), Pagination.page(4), Pagination.page(5), Pagination.PageNumber.DOTS, Pagination.page(100), Pagination.page(4, "next")));
        assertThat(Pagination.pageByNumber(4, 100, 1).getPages()).isEqualTo(List.of(Pagination.page(3, "prev"), Pagination.page(1), Pagination.page(2), Pagination.page(3), Pagination.currentPage(4), Pagination.page(5), Pagination.page(6), Pagination.PageNumber.DOTS, Pagination.page(100), Pagination.page(5, "next")));
    }

    @Test
    public void shouldShowDotsAtStartIfOnLast4Pages() {
        assertThat(Pagination.pageByNumber(97, 100, 1).getPages()).isEqualTo(List.of(Pagination.page(96, "prev"), Pagination.page(1), Pagination.PageNumber.DOTS, Pagination.page(95), Pagination.page(96), Pagination.currentPage(97), Pagination.page(98), Pagination.page(99), Pagination.page(100), Pagination.page(98, "next")));
        assertThat(Pagination.pageByNumber(98, 100, 1).getPages()).isEqualTo(List.of(Pagination.page(97, "prev"), Pagination.page(1), Pagination.PageNumber.DOTS, Pagination.page(96), Pagination.page(97), Pagination.currentPage(98), Pagination.page(99), Pagination.page(100), Pagination.page(99, "next")));
        assertThat(Pagination.pageByNumber(99, 100, 1).getPages()).isEqualTo(List.of(Pagination.page(98, "prev"), Pagination.page(1), Pagination.PageNumber.DOTS, Pagination.page(97), Pagination.page(98), Pagination.currentPage(99), Pagination.page(100), Pagination.page(100, "next")));
        assertThat(Pagination.pageByNumber(100, 100, 1).getPages()).isEqualTo(List.of(Pagination.page(99, "prev"), Pagination.page(1), Pagination.PageNumber.DOTS, Pagination.page(98), Pagination.page(99), Pagination.currentPage(100)));
    }

    @Test
    public void shouldUnderstandDotsPageNumber() {
        assertThat(Pagination.PageNumber.DOTS.isDots()).isTrue();
        assertThat(Pagination.page(1).isDots()).isFalse();
    }

    @Test
    public void shouldUnderstandPageNumberAndLabel() {
        assertThat(Pagination.PageNumber.DOTS.getLabel()).isEqualTo("...");
        assertThat(Pagination.PageNumber.DOTS.getNumber()).isEqualTo(-1);
        assertThat(Pagination.page(5).getLabel()).isEqualTo("5");
        assertThat(Pagination.page(5).getNumber()).isEqualTo(5);
        assertThat(Pagination.page(10, "foo").getLabel()).isEqualTo("foo");
        assertThat(Pagination.page(10, "foo").getNumber()).isEqualTo(10);
    }

    @Test
    public void shouldUnderstandIfCurrent() {
        assertThat(Pagination.currentPage(5).isCurrent()).isTrue();
        assertThat(Pagination.page(5).isCurrent()).isFalse();
    }

}
