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

google.load('search', '1', { language : 'en' });

PaginationLinks = Class.create({
  initialize: function(container, searcher) {
    this.container = $(container);
    this.searcher = searcher;
  },

  customize: function() {
    if (!this.container) return;

    var selectedIndex = null;
    var links = this.container.childElements();
    links.each(function(element, index) {
      if (element.hasClassName('gsc-cursor-current-page'))
        selectedIndex = index;
    });

    if (selectedIndex > 0) {
      this.container.insert({ 'top': this._createPrevLink() });
    }

    if (selectedIndex < (links.size() - 1)) {
      this.container.insert({ 'bottom': this._createNextLink() });
    }
  },

  _createPrevLink: function() {
    var prevLink = new Element('div', { 'id': 'prev-link', 'class': 'gsc-cursor-page' }).update('&laquo; Previous Page');
    prevLink.observe('click', function(event) {
      this.searcher.gotoPage(this.searcher.cursor.currentPageIndex - 1);
    }.bind(this));
    return prevLink;
  },

  _createNextLink: function() {
    var nextLink = new Element('div', { 'id': 'next-link', 'class': 'gsc-cursor-page' }).update('Next Page &raquo;');
    nextLink.observe('click', function(event){
      this.searcher.gotoPage(this.searcher.cursor.currentPageIndex + 1);
    }.bind(this));
    return nextLink;
  }

});

LastVisitedTocLink = Class.create({
  initialize: function(tocElement) {
    this.tocElement = tocElement;
    this.memory = null;
  },

  remember: function(){
    var selectedEntry = this.tocElement.select('.current').first();
    if (selectedEntry) {
      this.memory = selectedEntry;
      selectedEntry.removeClassName('current');
    }
  },

  highlightLastRemembered: function(element){
    var selectedEntry = this.tocElement.select('.current').first();
    selectedEntry.removeClassName('current');
    element.addClassName('current');
  }
});

GoHelpSearch = Class.create({
  initialize: function(formDomId, resultsDomId){
      this.formDomId = $(formDomId);
      this.resultsElement = $(resultsDomId);
      var options = new google.search.DrawOptions();
      options.setSearchFormRoot(formDomId);
      var searcher = new google.search.CustomSearchControl('013109281384884948049:gcpiyhasjsk');
      searcher.setResultSetSize(google.search.Search.FILTERED_CSE_RESULTSET);
      searcher.setNoResultsString('Your search did not match any help pages.');
      searcher.setSearchCompleteCallback(this, this.onSearchComplete.bind(this));
      searcher.draw(resultsDomId, options);

      this.welcomeToGoDiv = $('main');
      this.noResultsMessageDiv = $('no_result_message');
    Event.observe(window, 'resize', function(event){
      this.noResultsMessageDiv.style.width = $$('.action-bar').first().getWidth();
    }.bindAsEventListener(this));

      this.searchResultsContainer = $('search_results_container');

      var url = window.location.href;
      var truncated_url = url.substring(url.lastIndexOf('help') + 5, url.length);
      var some = "a[href='" + truncated_url + "']";
      var link = $$(some)[0];

      if (link === undefined) {
          link = $$("a[href='welcome_to_go.html']")[0];
      }
      this.lastSelectedToCLink = new LastVisitedTocLink($('nav').select('.toc').first());
      this.lastSelectedToCLink.highlightLastRemembered(link);

    this._cloneBrandingToDesiredPosition();
  },

  onSearchComplete: function(search, searcher){
    this._addPrevAndNextPagingLinks(searcher);
    this.noResultsMessageDiv.select('.search_term').first().innerHTML = search.input.value.replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;");
    this.searchResultsContainer.setStyle({ display : 'block' });
    this.welcomeToGoDiv.hide();
    this.noResultsMessageDiv.hide();

    this.lastSelectedToCLink.remember();
    if(this.lastSelectedToCLink.memory)
    {
      var backLink = $('hide_search_results');
      backLink.update('Back to ' + this.lastSelectedToCLink.memory.innerHTML);
      backLink.href = this.lastSelectedToCLink.memory.href;
    }
    this._replaceGoogleNoResultsMessage();
  },

  _addPrevAndNextPagingLinks: function(searcher) {
    new PaginationLinks(this.resultsElement.select('.gsc-cursor').first(), searcher).customize();
  },

  _replaceGoogleNoResultsMessage: function(){
    var googleNoResultBox = this.resultsElement.select('.gs-no-results-result').first();
    if (googleNoResultBox) {
      $('no_result_message').clonePosition(googleNoResultBox, { setHeight: false });
      this.noResultsMessageDiv.style.width = $$('.action-bar').first().getWidth();
      googleNoResultBox.hide();
      $('no_result_message').show();
    }
  },

  _cloneBrandingToDesiredPosition: function() {
    var googleBranding = $(this.formDomId).select('.gsc-search-box .gsc-branding').first().cloneNode(true);
    $('branding').appendChild(googleBranding);
  }
});

document.observe('dom:loaded', function() {
  new GoHelpSearch('go_help_search', 'search_results');
});
