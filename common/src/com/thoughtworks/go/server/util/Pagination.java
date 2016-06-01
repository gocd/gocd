/*
 * Copyright 2015 ThoughtWorks, Inc.
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

import java.util.ArrayList;
import java.util.List;

import java.util.LinkedHashMap;
import java.util.Map;

public class Pagination {
    private Integer pageSize;
    private Integer offset;
    private Integer total;

    private static final int DEFAULT_PER_PAGE = 10;
    private static final int MAXIMUM_LIMIT = 300;
    public static final Pagination ONE_ITEM = pageStartingAt(0, 1, 1);

    private static final int NUMBER_OF_NEIGHBOURS = 2;

    private Pagination(Integer offset, Integer total, Integer pageSize) {
        setPageSize(pageSize);
        setOffset(offset);
        setTotal(total);
    }

    private void setPageSize(Integer pageSize) {
        if (pageSize != null && pageSize > MAXIMUM_LIMIT) {
            throw new RuntimeException("The max number of perPage is [" + MAXIMUM_LIMIT + "].");
        }
        this.pageSize = (pageSize == null || pageSize == 0) ? DEFAULT_PER_PAGE : pageSize;

    }

    private void setOffset(Integer offset) {
        this.offset = (offset == null) ? 0 : offset;
    }

    private void setTotal(Integer total) {
        this.total = (total == null) ? 0 : total;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public Integer getOffset() {
        return offset;
    }

    public Integer getTotal() {
        return total;
    }

    public Map toJsonMap() {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("count", total);
        json.put("start", offset);
        json.put("perPage", pageSize);
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Pagination that = (Pagination) o;

        if (pageSize != null ? !pageSize.equals(that.pageSize) : that.pageSize != null) {
            return false;
        }
        if (offset != null ? !offset.equals(that.offset) : that.offset != null) {
            return false;
        }
        if (total != null ? !total.equals(that.total) : that.total != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = pageSize != null ? pageSize.hashCode() : 0;
        result = 31 * result + (offset != null ? offset.hashCode() : 0);
        result = 31 * result + (total != null ? total.hashCode() : 0);
        return result;
    }

    public int getCurrentPage() {
        return offset / pageSize + 1;
    }

    public int getTotalPages() {
        return total / pageSize;
    }

    public int getPreviousPage() {
        return getCurrentPage() - 1;
    }

    public int getPreviousOffset() {
        return (getPreviousPage() - 1) * pageSize;
    }

    public int getNextPage() {
        return getCurrentPage() + 1;
    }

    public int getNextOffset() {
        return (getNextPage() - 1) * pageSize;
    }

    public boolean hasNextPage() {
        return offset + pageSize <= total;
    }

    public boolean hasPreviousPage() {
        return offset - pageSize >= 0;
    }

    @Override public String toString() {
        return "Pagination{" +
                "pageSize=" + pageSize +
                ", offset=" + offset +
                ", total=" + total +
                '}';
    }

    public static Pagination pageFor(int currentItem, int totalCount, int pageSize) {
        return pageStartingAt((currentItem / pageSize) * pageSize, totalCount, pageSize);
    }

    public static Pagination pageStartingAt(Integer offset, Integer total, Integer pageSize) {
        return new Pagination(offset, total, pageSize);
    }

    public static Pagination pageByNumber(int pageNumber, int total, int pageSize) {
        return pageStartingAt((pageNumber - 1) * pageSize, total, pageSize);
    }

    public int getFirstPage() {
        return 1;
    }

    public int getLastPage() {
        return (total - 1) / pageSize + 1;
    }

    public List<PageNumber> getPages() {
        ArrayList<PageNumber> pages = new ArrayList<>();
        if (getCurrentPage() > getFirstPage()) {
            pages.add(new PageNumber(getCurrentPage() - 1, "prev"));
        }
        if (getLastPage() < showAllPagesThreshold()) {
            for (int i = 1; i <= getLastPage(); i++) {
                addPage(pages, i);
            }
        } else {
            if (getCurrentPage() <= endLength()) {
                for(int i = 1; i <= getCurrentPage() + NUMBER_OF_NEIGHBOURS; i++) {
                    addPage(pages, i);
                }
                pages.add(PageNumber.DOTS);
                addPage(pages, getLastPage());
            } else if(getCurrentPage() > getLastPage() - endLength()) {
                addPage(pages, getFirstPage());
                pages.add(PageNumber.DOTS);
                for(int i = getCurrentPage() - NUMBER_OF_NEIGHBOURS; i <= getLastPage(); i++) {
                    addPage(pages, i);
                }
            } else {
                addPage(pages, getFirstPage());
                pages.add(PageNumber.DOTS);
                for (int i = getCurrentPage() - NUMBER_OF_NEIGHBOURS; i <= getCurrentPage() + NUMBER_OF_NEIGHBOURS; i++) {
                    addPage(pages, i);
                }
                pages.add(PageNumber.DOTS);
                addPage(pages, getLastPage());
            }
        }
        if (getLastPage() > getCurrentPage()) {
            pages.add(new PageNumber(getCurrentPage() + 1, "next"));
        }

        return pages;
    }

    private void addPage(ArrayList<PageNumber> pages, int i) {
        pages.add(new PageNumber(i, i == getCurrentPage()));
    }

    private int showAllPagesThreshold() {
        return endLength() + 5;
    }

    private int endLength() {
        return NUMBER_OF_NEIGHBOURS * 2;
    }

    public static PageNumber page(int i) {
        return new PageNumber(i);
    }

    public static PageNumber page(int i, String label) {
        return new PageNumber(i, label);
    }

    public static PageNumber currentPage(int i) {
        return new PageNumber(i, true);
    }

    public static class PageNumber {
        public static final PageNumber DOTS = new PageNumber(-1, "...") {
            @Override public String toString() {
                return "...";
            }

            @Override public boolean isDots() {
                return true;
            }
        };

        private int page;
        private String label;
        private boolean current;

        public PageNumber(int page) {
            this(page, String.valueOf(page));
        }

        public PageNumber(int page, String label) {
            this.page = page;
            this.label = label;
        }

        public PageNumber(int page, boolean current) {
            this(page);
            this.current = current;
        }

        public boolean isDots() {
            return false;
        }

        public String getLabel() {
            return label;
        }

        public int getNumber() {
            return page;
        }

        public boolean isCurrent() {
            return current;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            PageNumber that = (PageNumber) o;

            if (current != that.current) {
                return false;
            }
            if (page != that.page) {
                return false;
            }
            if (label != null ? !label.equals(that.label) : that.label != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = page;
            result = 31 * result + (label != null ? label.hashCode() : 0);
            result = 31 * result + (current ? 1 : 0);
            return result;
        }

        @Override public String toString() {
            return "" + page +
                    "(" + label + ')'
                    + (current ? "current" : "");
        }
    }
}
