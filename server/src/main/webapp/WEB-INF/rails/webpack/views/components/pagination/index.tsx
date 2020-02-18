/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import classnames from "classnames";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import linkStyles from "views/components/link/index.scss";
import {Pagination} from "views/components/pagination/models/pagination";
import styles from "./index.scss";

interface PaginationAttrs {
  pagination: Pagination;
  onPageChange: (newPage: number) => void;
}

export class PaginationWidget extends MithrilViewComponent<PaginationAttrs> {
  view(vnode: m.Vnode<PaginationAttrs>) {
    const pagination = vnode.attrs.pagination;
    if (pagination.totalNumberOfPages() === 1) {
      return;
    }

    return (
      <div>
        <div class={styles.paginationContainer}
             data-test-id={`pagination-showing-${pagination.currentPageNumber()}-of-${pagination.totalNumberOfPages()}`}>
          <a role="button"
             class={classnames(linkStyles.inlineLink,
               styles.paginationLink,
               {[styles.disabled]: !pagination.hasPreviousPage()})}
             href={"#"}
             data-test-id={"pagination-previous-page"}
             disabled={!pagination.hasPreviousPage()}
             onclick={() => {
               if (pagination.hasPreviousPage()) {
                 vnode.attrs.onPageChange(pagination.previousPageNumber());
               }
               return false;
             }}>
            Previous
          </a>
          {this.pageNumberLinks(pagination, vnode)}
          <a role="button"
             class={classnames(linkStyles.inlineLink,
               styles.paginationLink,
               {[styles.disabled]: !pagination.hasNextPage()})}
             href={"#"}
             data-test-id={"pagination-next-page"}
             disabled={!pagination.hasNextPage()}
             onclick={() => {
               if (pagination.hasNextPage()) {
                 vnode.attrs.onPageChange(pagination.nextPageNumber());
               }
               return false;
             }}>
            Next
          </a>
        </div>
      </div>
    );
  }

  private pageNumberLinks(pagination: Pagination, vnode: m.Vnode<PaginationAttrs>) {
    return pagination.getVisiblePageNumbers().map((pageNumber, index, visiblePageNumbers) => {
      const paginationLink = <a role="button"
                                class={classnames(linkStyles.inlineLink,
                                  styles.paginationLink,
                                  pagination.currentPageNumber() === pageNumber ? styles.currentPage : undefined)}
                                href={"#"}
                                data-test-id={`pagination-page-${pageNumber}`}
                                data-test-current-page={pageNumber === pagination.currentPageNumber()}
                                onclick={() => {
                                  vnode.attrs.onPageChange(pageNumber);
                                  return false;
                                }}>{pageNumber}</a>;

      if (visiblePageNumbers[index - 1] + 2 < pageNumber) {
        return [
          <a role="button"
             class={classnames(linkStyles.inlineLink, styles.paginationLink)}
             href={"#"}
             data-test-id={`pagination-page-dummy-spacer`}
             onclick={() => false}>
            ...
          </a>,
          paginationLink
        ];
      } else {
        return paginationLink;
      }
    });
  }
}
