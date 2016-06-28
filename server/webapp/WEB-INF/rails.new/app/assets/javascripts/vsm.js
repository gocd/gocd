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


VSM = function(data, container, renderer, preloader){
    var data = data;
    var renderer = renderer;
    var container = $j(container);
    var preloader = $j(preloader)

    VSM.prototype.render = function() {
        if(data.error != null){
            $j('.page_header').hide();
            preloader.hide();
            var unableToFind = '<div class="pagenotfound"><div class="biggest">:(</div><h3>' + _.escape(data.error) + '</h3><span>Go to <a href="/go/pipelines">Pipelines</a></span></div>';
            $j('#vsm-container').css({margin:0, position:'inherit'});
            container.html(unableToFind);

        }else{
            renderer.invoke(data);
            preloader.hide();
        }
    }



};
