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
 *************************GO-LICENSE-END**********************************/

/* This class is used to generate the paginator */
var PageLink = Class.create();
var Paginator = Class.create();

Paginator.prototype = {
    initialize: function(){
        this.reset();
    },

    reset: function(){
        this.parameters = {};
        this.perPage = 10;
        this.start = 0;
    },

    setPageParameters: function(parameters){
        this.parameters = parameters;
    },

    setPerPage: function(perPage){
        this.perPage = perPage;
    },

    setStart: function(start){
        this.start = start;
        this.currentPage = this._getPageNumberOfThisStartOffset(start);
    },

    setCount: function(count){
        this.count = count;
        this.totalPages = this._getTotalPageNumber();
    },

    hasPrevious: function(){
        if(this.currentPage == 1){
            return false;
        }

        return true;
    },

    hasNext: function(){
        if(this.currentPage == this.totalPages){
            return false;
        }

        return true;
    },

    _getTotalPageNumber: function(){
        if(this.count == 0){
            return 1;
        } else {
            return Math.floor((this.count -1)/this.perPage) + 1;
        }
    },

    _getPageNumberOfThisStartOffset: function(startOffset){
        if(startOffset == 0){
            return 1;
        } else {
            return Math.floor(startOffset/this.perPage) + 1;
        }
    },

    setParametersFromJson: function(json){
        try{
            this.setCount(parseInt(json.count));
            this.setPerPage(parseInt(json.perPage));
            this.setStart(parseInt(json.start));
        }catch(e){
            FlashMessageLauncher.error('Cruise got errors when rendering this page', e);
        }
        
        return this.generatePageLinks();
    },

    generatePageLinks: function(){
        var totalPages = this.totalPages;
        var currentPage = this.currentPage;

        if(totalPages <= 11){
            this.pages = this._generatePageLinksFromEnumerable($R(1, this.totalPages));
        } else {
            var afterCurrentPages = totalPages - currentPage;
            var headLinkLimit, tailLinkLimit;

            if(currentPage >= 5 && afterCurrentPages >= 5) {
                headLinkLimit = 5;
                tailLinkLimit = 5;

                //both need add suspension
                this.pages = this._generatePageLinksFromEnumerable(
                    $A([
                        this._pickUpVisiblePageLinksFromHead(currentPage, headLinkLimit),
                        currentPage,
                        this._pickUpVisiblePageLinksFromTail(currentPage, tailLinkLimit, this.totalPages)
                    ]).flatten()
                );
            } else if (currentPage > 5 && afterCurrentPages <5){
                tailLinkLimit = afterCurrentPages;
                headLinkLimit = 10 - tailLinkLimit;

                //need add suspension before
                this.pages = this._generatePageLinksFromEnumerable(
                    $A([
                        this._pickUpVisiblePageLinksFromHead(currentPage, headLinkLimit),
                        $A($R(currentPage, this.totalPages))
                    ]).flatten()
                );
            } else if (currentPage < 5 && afterCurrentPages >5){
                headLinkLimit = currentPage - 1;
                tailLinkLimit = 10 - headLinkLimit;

                //need add suspension after
                this.pages =  this._generatePageLinksFromEnumerable(
                    $A([
                        $A($R(1, currentPage)),
                        this._pickUpVisiblePageLinksFromTail(currentPage, tailLinkLimit, this.totalPages)
                    ]).flatten()
                );
            }
        }

        return this.pages;
    },

    _generatePageLinksFromEnumerable: function(array){
        return array.collect(function(pageNumber){
            return new PageLink(pageNumber);
        })
    },

    _pickUpVisiblePageLinksFromHead: function(currentPage, limit){
        var suspension = false;

        if(currentPage == limit.succ()){
            // currentPage is just the next of return Range's last number
            return $A($R(1, limit));
        } else if(currentPage == limit) {
            // currentPage is same with return Range's last number
            return $A($R(1, limit - 1));
        } else {
            return $R(currentPage - limit + 2, currentPage - 1).inject([1, 2, suspension], function(newArray, value){
                newArray.push(value);
                return newArray;
            });
        }
    },

    _pickUpVisiblePageLinksFromTail: function(currentPage, limit, totalPage){
        var suspension = false;

        if(currentPage.succ() == (totalPage - limit)){
            // currentPage's next is the first of return Range
            return $A($R(totalPage - limit, totalPage));
        } else if(currentPage == (totalPage - limit)){
            // currentPage is same with the first of return Range
            return $A($R(totalPage - limit + 1, totalPage));
        } else {
            return $A([suspension, totalPage -1, totalPage]).inject($A($R(currentPage + 1, currentPage + limit -2)), function(newArray, value){
                newArray.push(value);
                return newArray;
            });
        }
    }
}

PageLink.prototype = {
    initialize: function(pageNumber){
        if(pageNumber){
            this.pageNumber = pageNumber;
            this.linkType = 'link';
        } else {
            this.linkType = 'suspension';
        }
    },
    isLink: function(){
        return this.linkType == 'link';
    },
    isSuspension: function(){
        return this.linkType == 'suspension';
    }
}

/* pagination js end */