///*************************GO-LICENSE-START*********************************
// * Copyright 2014 ThoughtWorks, Inc.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *    http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// *************************GO-LICENSE-END**********************************/
//
//describe("stage_detail", function(){
//    beforeEach(function(){
//        setFixtures();
//    });
//});
//
//var sample_json = {
//        "stage" : {
//            "current_status" : "building",
//            "builds" : [
//                {
//                    "agent" : "Not yet assigned",
//                    "current_status" : "scheduled",
//                    "result" : "Unknown",
//                    "name" : "functional",
//                    "build_completed_date" : "N/A",
//                    "id" : "66",
//                    "is_completed" : "false"
//                },
//                {
//                    "agent" : "Not yet assigned",
//                    "current_status" : "scheduled",
//                    "result" : "Unknown",
//                    "name" : "unit",
//                    "build_completed_date" : "N/A",
//                    "id" : "67",
//                    "is_completed" : "false"
//                }
//                ],
//            "id" : "30",
//            "current_label" : "30",
//            "stageName" : "mingle",
//            "pipelineName" : "studios"
//        }
//    }
//
//    function renderHeaderNow(){
//        $('stage-detail-header').innerHTML = $('stage-detail-header-template').value.process({object: sample_json});
//    }
//
//    function renderSidebarNow(){
//        $('stage-sidebar').innerHTML = $('stage-sidebar-template').value.process({object: sample_json});
//    }
//</script>
//<textarea id="stage-detail-header-template" style="display:none;">
//    <div class="build_detail_summary">
//        <h2>
//            <span class="pipeline-name">${object.stage.pipelineName}</span>
//            <span class="stage-name">${object.stage.stageName}</span>
//            <span class="status">${object.stage.current_status}</span>
//            <span class="complete-time">1 day ago</span>
//        </h2>
//    </div>
//</textarea>
//<textarea id="stage-sidebar-template" style="display:none;">
//    <h2 class="failed">
//        <a class="collapse-open" href="javascript:void(0)" onclick="$(this).toggleClassName('collapse-open').toggleClassName('collapse-closed');$('stage-detail-sidebar-content').toggle();">
//            {if object.stage.builds.length > 1}
//            Build plans
//            {else}
//            Build plan
//            {/if}
//        </a>
//    </h2>
//    <div class="actions_bar round-content" id="stage-detail-sidebar-content" style="">
//        <ul class="item-list">
//            {for build in object.stage.builds}
//                <li class="${build.current_status}"><a href="#">${build.name}</a>${build.current_status}</li>
//            {/for}
//        </ul>
//    </div>
//</textarea>
//</head>
//<body>
//<div id="stage-detail-header-example">
//    <div class="build_detail_summary">
//        <h2>
//            <span class="pipeline-name">Pipeline Name</span>
//            <span class="stage-name">Stage Name</span>
//            <span class="status">status</span>
//            <span class="complete-time">1 day ago</span>
//        </h2>
//    </div>
//</div>
//
//<div id="stage-detail-header"></div>
//
//<div class="round-panel" id="stage-sidebar-example">
//    <h2 class="failed">
//        <a class="collapse-open" href="javascript:void(0)" onclick="$(this).toggleClassName('collapse-open').toggleClassName('collapse-closed');$('stage-detail-sidebar-content').toggle();">
//            Build plan
//        </a>
//    </h2>
//    <div class="actions_bar round-content" id="stage-detail-sidebar-content" style="">
//        <ul class="item-list">
//            <li class="passed"><a href="#">Build-plan name</a>passed</li>
//        </ul>
//    </div>
//</div>
//
//<div class="round-panel" id="stage-sidebar"></div>
//
//</body>
//</html>
