DashboardAjaxRefresher = function() {
    var ids = $A();
    var newly_added_group_ids = $A();
    var parentIds = $A();

    function _parent(element) {
        //TODO: This could potential break with Joe's changes. Think of a better way to do this.
        return element.parentNode;
    }

    function _createEmptyContainer(receiverId, parentId, insertion_index, tagAttr) {
        var parentElement = $(parentId);
        addDummyContainer(parentElement, receiverId, insertion_index, parentElement.childElements(), tagAttr);
        newly_added_group_ids.push(receiverId);
    }

    function addDummyContainer(parentElement, receiverId, insertion_index, children, tagAttr) {
        var tempDiv = _tempDiv(receiverId);
        if (!tagAttr) {
            tagAttr = {};
        }
        if (children.length > insertion_index) {
            parentElement.insertBefore(tempDiv, children[insertion_index]);
        } else {
            parentElement.appendChild(tempDiv);
        }
        for (var attr in tagAttr) {
            /* IE craps out for writeAttribute, so setting the class names using prototype addClassName - Sachin */
            if (Prototype.Browser.IE && attr == "class") {
                $(tempDiv).addClassName(tagAttr[attr]);
            }
            else {
                tempDiv.writeAttribute(attr, tagAttr[attr]);
            }
        }
        return tempDiv;
    }

    function _createContainerForNewPipeline(receiverId, parentId, insertion_index, tagAttr) {
        var actualDaddy = $(parentId).select(".pipelines .content_wrapper_outer .content_wrapper_inner").first();
        var children = actualDaddy.select(".pipeline");
        var tempDiv = addDummyContainer(actualDaddy, receiverId, insertion_index, children, tagAttr);
        tempDiv.addClassName("pipeline");
        return true;
    }

    function _tempDiv(id) {
        var tempDiv = $(document.createElement('div'));
        tempDiv.writeAttribute("id", id);
        return tempDiv;
    }

    function _beforeRefresh(receiverId, replacementOptions) {
        var parentId = replacementOptions.parent_id;
        var index = replacementOptions.index;
        var elementType = replacementOptions.type;
        var tagAttr = replacementOptions.tag_attr;

        //don't register me here, exploit after_refresh
        ids.push(receiverId);
        parentIds.push(parentId);
        //----------------------------------

        if ($(receiverId) == null) {
            if ("group_of_pipelines" === elementType) {
                _createEmptyContainer(receiverId, parentId, index, tagAttr);
            } else if ("pipeline" === elementType) {
                _createContainerForNewPipeline(receiverId, parentId, index, tagAttr);
            } else if ("build_cause" === elementType) {
                _createEmptyContainer(receiverId, parentId, index, tagAttr);
                Util.buildCausePopupCreator(receiverId)();
            }
        } else if (hasPipelineMoved(elementType, parentId, receiverId)) {
            //TODO: This is a potential memory leak. Deal with the removal nicely
            $(receiverId).remove();
            _createContainerForNewPipeline(receiverId, parentId, index, tagAttr);
        } else if ("group_of_pipelines" === elementType) {
            return false;
        }
        return true;
    }

    function hasPipelineMoved(elementType, parentId, receiverId) {
        return "pipeline" === elementType && !$(receiverId).descendantOf(parentId);
    }

    function isPresentInSameGroup(id, pipelineParentId, elementId, index) {
        return id === pipelineParentId && parentIds[index] === elementId;
    }

    function _refreshCompleted(isPartialRefresh) {
        if (!isPartialRefresh) {
            $$(this.className).each(function(element) {
                var elementId = _parent(element).id;
                var found = false;
                if (ids.member(elementId)) {
                    removePipelines(element, elementId);
                    found = true;
                }
                if (!found) {
                    $(_parent(element)).remove();
                }
            });
            clearCollections();
        }
        if (this.refreshCompletedCallback) {
            this.refreshCompletedCallback(isPartialRefresh);
        }
        if(!isPartialRefresh) {
            new ElementAligner().alignAll();
        }
    }

    function removePipelines(group, groupId) {
        group.getElementsBySelector(".pipeline").each(function(pipelineElement) {
            var pipelineFound = false;
            var pipelineParentId = pipelineElement.id;
            ids.each(function(id, index) {
                if (isPresentInSameGroup(id, pipelineParentId, groupId, index)) {
                    pipelineFound = true;
                }
            });
            if (!pipelineFound) {
                pipelineElement.remove();
            }
        });
    }

    function clearCollections() {
        ids.clear();
        newly_added_group_ids.clear();
        parentIds.clear();
    }

    function reloadIfPossible() {
        var modal = $('MB_window');
        if (modal === null || modal.style.display == 'none') {
            window.location.reload();
            return true;
        }
        return false;
    }

    function registerMetaRefreshOnIE() {
        if ($j.browser.msie) {
            setTimeout(function() {
                if (!reloadIfPossible()) {
                    setInterval(reloadIfPossible, 60 * 1000);
                }
            }, 7.5 * 60 * 1000);
        }
    }

    return function(url, options) {
        registerMetaRefreshOnIE();
        options = options || {};
        var _options = {
            beforeRefresh: _beforeRefresh,

            refreshCompleted: _refreshCompleted,

            className: options.className,

            refreshCompletedCallback: options.refreshCompleted,

            refreshBegining: options.refreshBegining
        };
        return new AjaxRefresher(url, options.redirectUrl, _options);
    };
}();

var BuildCauseBinder = function() {
    var changes_link = '';
    var content_dom = '';

    function _bindChangesLink() {
        var popup_shower = Util.namespace('build_cause').get(content_dom);
        popup_shower.cleanup();
        popup_shower.bindShowButton($(changes_link));
    }

    return function(_changes_link, _content_dom){
        changes_link = _changes_link;
        content_dom = _content_dom;
        return _bindChangesLink;
    }

}();

var PipelineOperations = {
    onTrigger: function(form, pipeline_name, url) {
        AjaxRefreshers.disableAjax();
        $('deploy-' + pipeline_name).disabled = true;
        new Ajax.Updater({success:"trigger-result-" + pipeline_name, failure:"trigger-result-" + pipeline_name},
                url,
                {
                    asynchronous:true,
                    evalScripts:true,
                    requestHeaders: {
                      Confirm: 'true'
                    },
                    on401:function() {
                        redirectToLoginPage('/go/auth/login');
                    },
                    onComplete:function() {
                        AjaxRefreshers.enableAjax();
                    },
                    parameters:Form.serialize(form)
                });
        return false;
    },

    onTriggerWithOptions: function(form, pipeline_name, trigger_label, url) {
        AjaxRefreshers.disableAjax();
        var button_id = 'deploy-with-options-' + pipeline_name;
        $(button_id).disable=true;
        new Ajax.Request(url,
                            {
                                asynchronous:true,
                                evalScripts:true,
                                on401: function() {
                                    redirectToLoginPage('/go/auth/login');
                                },
                                onComplete: function() {
                                    AjaxRefreshers.enableAjax();
                                },
                                onSuccess: function(request) {
                                    Modalbox.show(request.responseText,
                                        {
                                            title: pipeline_name + ' - '+ trigger_label,
                                            overlayClose: false,
                                            width: 850,
                                            height: 525,
                                            slideDownDuration: 0,
                                            overlayDuration: 0,
                                            autoFocusing: false
                                        });
                                    $(button_id).enable();},
                                parameters:Form.serialize(form)
                            }
                );
        return false;
    },

    onTriggerWithOptionsDialog: function(form, pipeline_name, url) {
        AjaxRefreshers.disableAjax();
        disable_unchanged();
        var trigger_result = 'trigger-result-' + pipeline_name;
        new Ajax.Updater({success: trigger_result,failure: trigger_result}, url,
                {
                    asynchronous:true,
                    evalScripts:true,
                    requestHeaders: {
                      Confirm: 'true'
                    },
                    on401:function() {
                        redirectToLoginPage('/go/auth/login');
                    },
                    onComplete:function() {
                        AjaxRefreshers.enableAjax();
                        Modalbox.hide()
                    },
                    parameters:Form.serialize(form)
                }
            );
        return false;
    },

    onPause: function(form, pipeline_name, url) {
        AjaxRefreshers.disableAjax();
        $('confirm-pause-' + pipeline_name).disable = true;
        Modalbox.hide();
        new Ajax.Request(url,
                        {
                            asynchronous:true,
                            evalScripts:true,
                            requestHeaders: {
                              Confirm: 'true'
                            },
                            on401:function() {
                                redirectToLoginPage('/go/auth/login');
                            },
                            onComplete:function() {
                                AjaxRefreshers.enableAjax();
                            },
                            parameters:Form.serialize(form)
                        }
                );
        return false;
    },

    onUnPause: function(form, pipeline_name, url) {
        AjaxRefreshers.disableAjax();
        $('unpause-' + pipeline_name).disable = true;
        new Ajax.Request(url,
                        {
                            asynchronous:true,
                            evalScripts:true,
                            requestHeaders: {
                              Confirm: 'true'
                            },
                            on401:function() {
                                redirectToLoginPage('/go/auth/login');
                            },
                            onComplete:function() {
                                AjaxRefreshers.enableAjax();
                            },
                            parameters:Form.serialize(form)
                        }
                );
        return false;
    },

    onPipelineSelector: function(form, url) {
        AjaxRefreshers.disableAjax();
        new Ajax.Request(url,
                            {
                                asynchronous:true,
                                evalScripts:true,
                                on401:function() {
                                    redirectToLoginPage('/go/auth/login');
                                },
                                onComplete:function() {
                                    AjaxRefreshers.enableAjax();
                                    PipelineFilter.close();
                                },
                                parameters:Form.serialize(form)
                            }
                );
        return false;
    }
};