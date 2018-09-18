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

describe("pipeline_dashboard_rails_new", function () {
    beforeEach(function () {
        AjaxRefreshers.clear();
        setFixtures("<div class='under_test'>\n" +
            "    <div class=\"dashboard_microcontent_popup dashboard_build_cause_like_microcontent_popups\">\n" +
            "        <div class=\"hidden changes enhanced_dropdown\" id=\"dashboard_build_cause_content\">&nbsp;</div>\n" +
            "    </div>\n" +
            "    <div id='pipeline_groups_container'>\n" +
            "        <div id='pipeline_group_group-1_panel'>\n" +
            "            <div class='pipeline_bundle pipeline_group'>\n" +
            "                <div class='pipelines'>\n" +
            "                    <div class='content_wrapper_outer'>\n" +
            "                        <div class='content_wrapper_inner'><h2 class='entity_title'>group-1</h2>\n" +
            "\n" +
            "                            <div id='pipeline_pipeline-1_panel' class='pipeline'>\n" +
            "                                <div class='pipeline_header'>\n" +
            "                                    <div class='pipeline_actions'>\n" +
            "                                    </div>\n" +
            "                                    <div class='pipeline_name_link'><h3 class='title entity_title '><a href='/tab/pipeline/history/pipeline-1'>pipeline-1</a></h3></div>\n" +
            "                                </div>\n" +
            "                                <div class='alert' id='trigger-result-pipeline-1'></div>\n" +
            "                                <div class='pipeline_instance'>\n" +
            "                                    <div class='status details'>\n" +
            "                                        <div class='label'> Label: <a href='/pipelines/value_stream_map/pipeline-1/5' title='label-1'>label-1</a></div>\n" +
            "                                        <span class='compare_pipeline dashboard'>\n" +
            "                                            <a href='/compare/pipeline-1/4/with/5' title='Compare with the previous build'>Compare</a>\n" +
            "                                        </span>\n" +
            "                                        <div class='pipeline_instance_details'>\n" +
            "                                            <div class='schedule_time' title='REPLACED_DATE_TIME'> (Triggered&nbsp;by&nbsp; <span class='who'>Anonymous</span> &nbsp;<span class='time'/> <input type='hidden' value='REPLACED_DATE_TIME_MILLIS'/> )</div>\n" +
            "                                            <div class='stages'>\n" +
            "                                                <div class='latest_stage'>\n" +
            "                                                    Passed: cruise\n" +
            "                                                </div>\n" +
            "                                                <a href='/pipelines/pipeline-1/5/cruise/10' class='stage'>\n" +
            "                                                    <div class='stage_bar_wrapper last_run_stage'>\n" +
            "                                                        <div class='stage_bar Passed' title='cruise (Passed)' data-stage='cruise' style='width: 19.75em'>\n" +
            "\n" +
            "                                                        </div>\n" +
            "                                                    </div>\n" +
            "                                                </a></div>\n" +
            "                                        </div>\n" +
            "                                    </div>\n" +
            "                                    <div class='previously_wrapper'>\n" +
            "\n" +
            "                                    </div>\n" +
            "                                </div>\n" +
            "                                <div class='pipeline_operations'>\n" +
            "                                    <form accept-charset='UTF-8' action='/api/pipelines/pipeline-1/schedule' method='post'\n" +
            "                                          onsubmit=\"PipelineOperations.onTrigger(this, 'pipeline-1', '/api/pipelines/pipeline-1/schedule'); return false;; \">\n" +
            "                                        <div style=\"margin:0;padding:0;display:inline\"><input name=\"utf8\" type=\"hidden\" value=\"&#x2713;\" /></div>\n" +
            "                                        <div class='operate trigger_wrapper'>\n" +
            "                                            <button class='image submit' id='deploy-pipeline-1' title='Trigger' type='submit' value='Trigger'><span title='Trigger'> </span></button>\n" +
            "                                        </div>\n" +
            "                                    </form>\n" +
            "                                    <form accept-charset='UTF-8' action='/pipelines/show_for_trigger' method='post'\n" +
            "                                          onsubmit=\"PipelineOperations.onTriggerWithOptions(this, 'pipeline-1', 'Trigger', '/pipelines/show_for_trigger'); return false;; \">\n" +
            "                                        <div style=\"margin:0;padding:0;display:inline\"><input name=\"utf8\" type=\"hidden\" value=\"&#x2713;\" /></div>\n" +
            "                                        <input type='hidden' name='pipeline_name' value='pipeline-1'/>\n" +
            "\n" +
            "                                        <div class='operate trigger_with_options_wrapper'>\n" +
            "                                            <button class='image submit' id='deploy-with-options-pipeline-1' title='Trigger with options' type='submit' value='Trigger with options'><span\n" +
            "                                                    title='Trigger with options'> </span></button>\n" +
            "                                        </div>\n" +
            "                                    </form>\n" +
            "                                    <div class='operate pause_wrapper'>\n" +
            "                                        <button class='image submit' id='confirm-pause-pipeline-1'\n" +
            "                                                onclick='Modalbox.show($(&quot;pause-info-pipeline-1&quot;),{title: &quot;Pause pipeline: pipeline-1 &quot;,overlayClose:false})' title='Pause'\n" +
            "                                                type='submit' value='Pause'><span title='Pause'> </span></button>\n" +
            "                                    </div>\n" +
            "                                    <div id='pause-info-pipeline-1' style='display:none'>\n" +
            "                                        <form action='/api/pipelines/pipeline-1/pause' method='post'\n" +
            "                                              onsubmit=\"PipelineOperations.onPause(this, 'pipeline-1', '/api/pipelines/pipeline-1/pause'); return false;\">\n" +
            "                                            <div class='sub_tab_container'>\n" +
            "                                                <div class='pause_reason'> Specify a reason for pausing schedule on pipeline 'pipeline-1': <input type='text' name='pauseCause' maxlength='255'/></div>\n" +
            "                                            </div>\n" +
            "                                            <div class='actions'>\n" +
            "                                                <button class='primary submit' id='pause-pipeline-1' type='submit' value='Ok'><span>OK</span></button>\n" +
            "                                                <button class='submit button' onclick='Modalbox.hide()' type='button' value='Close'><span>CLOSE</span></button>\n" +
            "                                            </div>\n" +
            "                                        </form>\n" +
            "                                    </div>\n" +
            "                                </div>\n" +
            "                            </div>\n" +
            "                            <div class='divider'></div>\n" +
            "                            <div id='pipeline_pipeline-2_panel' class='pipeline'>\n" +
            "                                <div class='pipeline_header'>\n" +
            "                                    <div class='pipeline_actions'>\n" +
            "                                    </div>\n" +
            "                                    <div class='pipeline_name_link'><h3 class='title entity_title '><a href='/tab/pipeline/history/pipeline-2'>pipeline-2</a></h3></div>\n" +
            "                                </div>\n" +
            "                                <div class='alert' id='trigger-result-pipeline-2'></div>\n" +
            "                                <div class='pipeline_instance'>\n" +
            "                                    <div class='status details'>\n" +
            "                                        <div class='label'> Label: <a href='/pipelines/value_stream_map/pipeline-2/5' title='label-2'>label-2</a></div>\n" +
            "                                        <span class='compare_pipeline dashboard'>\n" +
            "                                            <a href='/compare/pipeline-2/4/with/5' title='Compare with the previous build'>Compare</a>\n" +
            "                                        </span>\n" +
            "                                        <div class='pipeline_instance_details'>\n" +
            "                                            <div class='schedule_time' title='REPLACED_DATE_TIME'> (Triggered&nbsp;by&nbsp; <span class='who'>Anonymous</span> &nbsp;<span class='time'/> <input type='hidden' value='REPLACED_DATE_TIME_MILLIS'/> )</div>\n" +
            "                                            <div class='stages'>\n" +
            "                                                <div class='latest_stage'>\n" +
            "                                                    Building: cruise\n" +
            "                                                </div>\n" +
            "                                                <a href='/pipelines/pipeline-2/5/cruise/10' class='stage'>\n" +
            "                                                    <div class='stage_bar_wrapper last_run_stage'>\n" +
            "                                                        <div class='stage_bar Building' title='cruise (Building)' data-stage='cruise' style='width: 19.75em'>\n" +
            "\n" +
            "                                                        </div>\n" +
            "                                                    </div>\n" +
            "                                                </a></div>\n" +
            "                                        </div>\n" +
            "                                    </div>\n" +
            "                                    <div class='previously_wrapper'>\n" +
            "                                        <div class='previously'><span class='label'>Previously: </span> <a class='result' href='/pipelines/pipeline-2/3/cruise/2' title='label-007'> <span\n" +
            "                                                class='color_code_small Failed'> </span> Failed </a></div>\n" +
            "                                    </div>\n" +
            "                                </div>\n" +
            "                                <div class='pipeline_operations'>\n" +
            "                                    <form accept-charset='UTF-8' action='/api/pipelines/pipeline-2/schedule' method='post'\n" +
            "                                          onsubmit=\"PipelineOperations.onTrigger(this, 'pipeline-2', '/api/pipelines/pipeline-2/schedule'); return false;; \">\n" +
            "                                        <div style=\"margin:0;padding:0;display:inline\"><input name=\"utf8\" type=\"hidden\" value=\"&#x2713;\" /></div>\n" +
            "                                        <div class='operate trigger_wrapper'>\n" +
            "                                            <button class='image submit' id='deploy-pipeline-2' title='Trigger' type='submit' value='Trigger'><span title='Trigger'> </span></button>\n" +
            "                                        </div>\n" +
            "                                    </form>\n" +
            "                                    <form accept-charset='UTF-8' action='/pipelines/show_for_trigger' method='post'\n" +
            "                                          onsubmit=\"PipelineOperations.onTriggerWithOptions(this, 'pipeline-2', 'Trigger', '/pipelines/show_for_trigger'); return false;; \">\n" +
            "                                        <div style=\"margin:0;padding:0;display:inline\"><input name=\"utf8\" type=\"hidden\" value=\"&#x2713;\" /></div>\n" +
            "                                        <input type='hidden' name='pipeline_name' value='pipeline-2'/>\n" +
            "\n" +
            "                                        <div class='operate trigger_with_options_wrapper'>\n" +
            "                                            <button class='image submit' id='deploy-with-options-pipeline-2' title='Trigger with options' type='submit' value='Trigger with options'><span\n" +
            "                                                    title='Trigger with options'> </span></button>\n" +
            "                                        </div>\n" +
            "                                    </form>\n" +
            "                                    <div class='operate pause_wrapper'>\n" +
            "                                        <button class='image submit' id='confirm-pause-pipeline-2'\n" +
            "                                                onclick='Modalbox.show($(&quot;pause-info-pipeline-2&quot;),{title: &quot;Pause pipeline: pipeline-2 &quot;,overlayClose:false})' title='Pause'\n" +
            "                                                type='submit' value='Pause'><span title='Pause'> </span></button>\n" +
            "                                    </div>\n" +
            "                                    <div id='pause-info-pipeline-2' style='display:none'>\n" +
            "                                        <form action='/api/pipelines/pipeline-2/pause' method='post'\n" +
            "                                              onsubmit=\"PipelineOperations.onPause(this, 'pipeline-2', '/api/pipelines/pipeline-2/pause'); return false;\">\n" +
            "                                            <div class='sub_tab_container'>\n" +
            "                                                <div class='pause_reason'> Specify a reason for pausing schedule on pipeline 'pipeline-2': <input type='text' name='pauseCause' maxlength='255'/></div>\n" +
            "                                            </div>\n" +
            "                                            <div class='actions'>\n" +
            "                                                <button class='primary submit' id='pause-pipeline-2' type='submit' value='Ok'><span>OK</span></button>\n" +
            "                                                <button class='submit button' onclick='Modalbox.hide()' type='button' value='Close'><span>CLOSE</span></button>\n" +
            "                                            </div>\n" +
            "                                        </form>\n" +
            "                                    </div>\n" +
            "                                </div>\n" +
            "                            </div>\n" +
            "                            <div class='divider'></div>\n" +
            "                            <div id='pipeline_pipeline-3_panel' class='pipeline'>\n" +
            "                                <div class='pipeline_header'>\n" +
            "                                    <div class='pipeline_actions'>\n" +
            "                                    </div>\n" +
            "                                    <div class='pipeline_name_link'><h3 class='title entity_title '><a href='/tab/pipeline/history/pipeline-3'>pipeline-3</a></h3></div>\n" +
            "                                </div>\n" +
            "                                <div class='alert' id='trigger-result-pipeline-3'></div>\n" +
            "                                <div class='pipeline_instance'>\n" +
            "                                    <div class='status details'>\n" +
            "                                        <div class='label'> Label: <a href='/pipelines/value_stream_map/pipeline-3/5' title='label-3'>label-3</a></div>\n" +
            "                                        <span class='compare_pipeline dashboard'> <a href='/compare/pipeline-3/4/with/5' title='Compare with the previous build'>Compare</a> </span>\n" +
            "                                        <div class='pipeline_instance_details'>\n" +
            "                                            <div class='schedule_time' title='REPLACED_DATE_TIME'> (Triggered&nbsp;by&nbsp; <span class='who'>Anonymous</span> &nbsp;<span class='time'/> <input type='hidden' value='REPLACED_DATE_TIME_MILLIS'/> )</div>\n" +
            "                                            <div class='stages'>\n" +
            "                                                <div class='latest_stage'>\n" +
            "                                                    Passed: cruise\n" +
            "                                                </div>\n" +
            "                                                <a href='/pipelines/pipeline-3/5/cruise/10' class='stage'>\n" +
            "                                                    <div class='stage_bar_wrapper last_run_stage'>\n" +
            "                                                        <div class='stage_bar Passed' title='cruise (Passed)' data-stage='cruise' style='width: 19.75em'>\n" +
            "\n" +
            "                                                        </div>\n" +
            "                                                    </div>\n" +
            "                                                </a></div>\n" +
            "                                        </div>\n" +
            "                                    </div>\n" +
            "                                    <div class='previously_wrapper'>\n" +
            "\n" +
            "                                    </div>\n" +
            "                                </div>\n" +
            "                                <div class='pipeline_operations'>\n" +
            "                                    <form accept-charset='UTF-8' action='/api/pipelines/pipeline-3/schedule' method='post'\n" +
            "                                          onsubmit=\"PipelineOperations.onTrigger(this, 'pipeline-3', '/api/pipelines/pipeline-3/schedule'); return false;; \">\n" +
            "                                        <div style=\"margin:0;padding:0;display:inline\"><input name=\"utf8\" type=\"hidden\" value=\"&#x2713;\" /></div>\n" +
            "                                        <div class='operate trigger_wrapper'>\n" +
            "                                            <button class='image submit' id='deploy-pipeline-3' title='Trigger' type='submit' value='Trigger'><span title='Trigger'> </span></button>\n" +
            "                                        </div>\n" +
            "                                    </form>\n" +
            "                                    <form accept-charset='UTF-8' action='/pipelines/show_for_trigger' method='post'\n" +
            "                                          onsubmit=\"PipelineOperations.onTriggerWithOptions(this, 'pipeline-3', 'Trigger', '/pipelines/show_for_trigger'); return false;; \">\n" +
            "                                        <div style=\"margin:0;padding:0;display:inline\"><input name=\"utf8\" type=\"hidden\" value=\"&#x2713;\" /></div>\n" +
            "                                        <input type='hidden' name='pipeline_name' value='pipeline-3'/>\n" +
            "\n" +
            "                                        <div class='operate trigger_with_options_wrapper'>\n" +
            "                                            <button class='image submit' id='deploy-with-options-pipeline-3' title='Trigger with options' type='submit' value='Trigger with options'><span\n" +
            "                                                    title='Trigger with options'> </span></button>\n" +
            "                                        </div>\n" +
            "                                    </form>\n" +
            "                                    <div class='operate pause_wrapper'>\n" +
            "                                        <button class='image submit' id='confirm-pause-pipeline-3'\n" +
            "                                                onclick='Modalbox.show($(&quot;pause-info-pipeline-3&quot;),{title: &quot;Pause pipeline: pipeline-3 &quot;,overlayClose:false})' title='Pause'\n" +
            "                                                type='submit' value='Pause'><span title='Pause'> </span></button>\n" +
            "                                    </div>\n" +
            "                                    <div id='pause-info-pipeline-3' style='display:none'>\n" +
            "                                        <form action='/api/pipelines/pipeline-3/pause' method='post'\n" +
            "                                              onsubmit=\"PipelineOperations.onPause(this, 'pipeline-3', '/api/pipelines/pipeline-3/pause'); return false;\">\n" +
            "                                            <div class='sub_tab_container'>\n" +
            "                                                <div class='pause_reason'> Specify a reason for pausing schedule on pipeline 'pipeline-3': <input type='text' name='pauseCause' maxlength='255'/></div>\n" +
            "                                            </div>\n" +
            "                                            <div class='actions'>\n" +
            "                                                <button class='primary submit' id='pause-pipeline-3' type='submit' value='Ok'><span>OK</span></button>\n" +
            "                                                <button class='submit button' onclick='Modalbox.hide()' type='button' value='Close'><span>CLOSE</span></button>\n" +
            "                                            </div>\n" +
            "                                        </form>\n" +
            "                                    </div>\n" +
            "                                </div>\n" +
            "                            </div>\n" +
            "                            <div class='divider'></div>\n" +
            "                            <div id='pipeline_pipeline-4_panel' class='pipeline'>\n" +
            "                                <div class='pipeline_header'>\n" +
            "                                    <div class='pipeline_actions'>\n" +
            "                                    </div>\n" +
            "                                    <div class='pipeline_name_link'><h3 class='title entity_title '><a href='/tab/pipeline/history/pipeline-4'>pipeline-4</a></h3></div>\n" +
            "                                </div>\n" +
            "                                <div class='alert' id='trigger-result-pipeline-4'></div>\n" +
            "                                <div class='pipeline_instance'>\n" +
            "                                    <div class='status details'>\n" +
            "                                        <div class='label'> Label: <a href='/pipelines/value_stream_map/pipeline-4/5' title='label-4'>label-4</a></div>\n" +
            "                                        <span class='compare_pipeline dashboard'> <a href='/compare/pipeline-4/4/with/5' title='Compare with the previous build'>Compare</a> </span>\n" +
            "                                        <div class='pipeline_instance_details'>\n" +
            "                                            <div class='schedule_time' title='REPLACED_DATE_TIME'> (Triggered&nbsp;by&nbsp; <span class='who'>Anonymous</span> &nbsp;<span class='time'/> <input type='hidden' value='REPLACED_DATE_TIME_MILLIS'/> )</div>\n" +
            "                                            <div class='stages'>\n" +
            "                                                <div class='latest_stage'>\n" +
            "                                                    Passed: cruise\n" +
            "                                                </div>\n" +
            "                                                <a href='/pipelines/pipeline-4/5/cruise/10' class='stage'>\n" +
            "                                                    <div class='stage_bar_wrapper last_run_stage'>\n" +
            "                                                        <div class='stage_bar Passed' title='cruise (Passed)' data-stage='cruise' style='width: 19.75em'>\n" +
            "\n" +
            "                                                        </div>\n" +
            "                                                    </div>\n" +
            "                                                </a></div>\n" +
            "                                        </div>\n" +
            "                                    </div>\n" +
            "                                    <div class='previously_wrapper'>\n" +
            "\n" +
            "                                    </div>\n" +
            "                                </div>\n" +
            "                                <div class='pipeline_operations'>\n" +
            "                                    <form accept-charset='UTF-8' action='/api/pipelines/pipeline-4/schedule' method='post'\n" +
            "                                          onsubmit=\"PipelineOperations.onTrigger(this, 'pipeline-4', '/api/pipelines/pipeline-4/schedule'); return false;; \">\n" +
            "                                        <div style=\"margin:0;padding:0;display:inline\"><input name=\"utf8\" type=\"hidden\" value=\"&#x2713;\" /></div>\n" +
            "                                        <div class='operate trigger_wrapper'>\n" +
            "                                            <button class='image submit' id='deploy-pipeline-4' title='Trigger' type='submit' value='Trigger'><span title='Trigger'> </span></button>\n" +
            "                                        </div>\n" +
            "                                    </form>\n" +
            "                                    <form accept-charset='UTF-8' action='/pipelines/show_for_trigger' method='post'\n" +
            "                                          onsubmit=\"PipelineOperations.onTriggerWithOptions(this, 'pipeline-4', 'Trigger', '/pipelines/show_for_trigger'); return false;; \">\n" +
            "                                        <div style=\"margin:0;padding:0;display:inline\"><input name=\"utf8\" type=\"hidden\" value=\"&#x2713;\" /></div>\n" +
            "                                        <input type='hidden' name='pipeline_name' value='pipeline-4'/>\n" +
            "\n" +
            "                                        <div class='operate trigger_with_options_wrapper'>\n" +
            "                                            <button class='image submit' id='deploy-with-options-pipeline-4' title='Trigger with options' type='submit' value='Trigger with options'><span\n" +
            "                                                    title='Trigger with options'> </span></button>\n" +
            "                                        </div>\n" +
            "                                    </form>\n" +
            "                                    <div class='operate pause_wrapper'>\n" +
            "                                        <button class='image submit' id='confirm-pause-pipeline-4'\n" +
            "                                                onclick='Modalbox.show($(&quot;pause-info-pipeline-4&quot;),{title: &quot;Pause pipeline: pipeline-4 &quot;,overlayClose:false})' title='Pause'\n" +
            "                                                type='submit' value='Pause'><span title='Pause'> </span></button>\n" +
            "                                    </div>\n" +
            "                                    <div id='pause-info-pipeline-4' style='display:none'>\n" +
            "                                        <form action='/api/pipelines/pipeline-4/pause' method='post'\n" +
            "                                              onsubmit=\"PipelineOperations.onPause(this, 'pipeline-4', '/api/pipelines/pipeline-4/pause'); return false;\">\n" +
            "                                            <div class='sub_tab_container'>\n" +
            "                                                <div class='pause_reason'> Specify a reason for pausing schedule on pipeline 'pipeline-4': <input type='text' name='pauseCause' maxlength='255'/></div>\n" +
            "                                            </div>\n" +
            "                                            <div class='actions'>\n" +
            "                                                <button class='primary submit' id='pause-pipeline-4' type='submit' value='Ok'><span>OK</span></button>\n" +
            "                                                <button class='submit button' onclick='Modalbox.hide()' type='button' value='Close'><span>CLOSE</span></button>\n" +
            "                                            </div>\n" +
            "                                        </form>\n" +
            "                                    </div>\n" +
            "                                </div>\n" +
            "                            </div>\n" +
            "                            <div class='divider'></div>\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "        <div id='pipeline_group_group-2_panel'>\n" +
            "            <div class='pipeline_bundle pipeline_group'>\n" +
            "                <div class='pipelines'>\n" +
            "                    <div class='content_wrapper_outer'>\n" +
            "                        <div class='content_wrapper_inner'><h2 class='entity_title'>group-2</h2>\n" +
            "\n" +
            "                            <div id='pipeline_pipeline-2-1_panel' class='pipeline'>\n" +
            "                                <div class='pipeline_header'>\n" +
            "                                    <div class='pipeline_actions'>\n" +
            "                                    </div>\n" +
            "                                    <div class='pipeline_name_link'><h3 class='title entity_title '><a href='/tab/pipeline/history/pipeline-2-1'>pipeline-2-1</a></h3></div>\n" +
            "                                </div>\n" +
            "                                <div class='alert' id='trigger-result-pipeline-2-1'></div>\n" +
            "                                <div class='pipeline_instance'>\n" +
            "                                    <div class='status'><span class='message'> No historical data </span></div>\n" +
            "                                </div>\n" +
            "                                <div class='pipeline_operations'>\n" +
            "                                    <form accept-charset='UTF-8' action='/api/pipelines/pipeline-2-1/schedule' method='post'\n" +
            "                                          onsubmit=\"PipelineOperations.onTrigger(this, 'pipeline-2-1', '/api/pipelines/pipeline-2-1/schedule'); return false;; \">\n" +
            "                                        <div style=\"margin:0;padding:0;display:inline\"><input name=\"utf8\" type=\"hidden\" value=\"&#x2713;\" /></div>\n" +
            "                                        <div class='operate trigger_wrapper'>\n" +
            "                                            <button class='image submit' id='deploy-pipeline-2-1' title='Trigger' type='submit' value='Trigger'><span title='Trigger'> </span></button>\n" +
            "                                        </div>\n" +
            "                                    </form>\n" +
            "                                    <form accept-charset='UTF-8' action='/pipelines/show_for_trigger' method='post'\n" +
            "                                          onsubmit=\"PipelineOperations.onTriggerWithOptions(this, 'pipeline-2-1', 'Trigger', '/pipelines/show_for_trigger'); return false;; \">\n" +
            "                                        <div style=\"margin:0;padding:0;display:inline\"><input name=\"utf8\" type=\"hidden\" value=\"&#x2713;\" /></div>\n" +
            "                                        <input type='hidden' name='pipeline_name' value='pipeline-2-1'/>\n" +
            "\n" +
            "                                        <div class='operate trigger_with_options_wrapper'>\n" +
            "                                            <button class='image submit' id='deploy-with-options-pipeline-2-1' title='Trigger with options' type='submit' value='Trigger with options'><span\n" +
            "                                                    title='Trigger with options'> </span></button>\n" +
            "                                        </div>\n" +
            "                                    </form>\n" +
            "                                    <div class='operate pause_wrapper'>\n" +
            "                                        <button class='image submit' id='confirm-pause-pipeline-2-1'\n" +
            "                                                onclick='Modalbox.show($(&quot;pause-info-pipeline-2-1&quot;),{title: &quot;Pause pipeline: pipeline-2-1 &quot;,overlayClose:false})' title='Pause'\n" +
            "                                                type='submit' value='Pause'><span title='Pause'> </span></button>\n" +
            "                                    </div>\n" +
            "                                    <div id='pause-info-pipeline-2-1' style='display:none'>\n" +
            "                                        <form action='/api/pipelines/pipeline-2-1/pause' method='post'\n" +
            "                                              onsubmit=\"PipelineOperations.onPause(this, 'pipeline-2-1', '/api/pipelines/pipeline-2-1/pause'); return false;\">\n" +
            "                                            <div class='sub_tab_container'>\n" +
            "                                                <div class='pause_reason'> Specify a reason for pausing schedule on pipeline 'pipeline-2-1': <input type='text' name='pauseCause' maxlength='255'/>\n" +
            "                                                </div>\n" +
            "                                            </div>\n" +
            "                                            <div class='actions'>\n" +
            "                                                <button class='primary submit' id='pause-pipeline-2-1' type='submit' value='Ok'><span>OK</span></button>\n" +
            "                                                <button class='submit button' onclick='Modalbox.hide()' type='button' value='Close'><span>CLOSE</span></button>\n" +
            "                                            </div>\n" +
            "                                        </form>\n" +
            "                                    </div>\n" +
            "                                </div>\n" +
            "                            </div>\n" +
            "                            <div class='divider'></div>\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "    <script type=\"text/javascript\">\n" +
            "        Util.on_load(function() {\n" +
            "            var dashboard_ajax_refresher = new DashboardAjaxRefresher('/pipelines?format=json', {redirectUrl: '/auth/login', className: '.pipeline_group', refreshBegining: function(){ PipelineFilter.unhookCheckboxes();}, refreshCompleted: function(){Util.enable('show_pipelines_selector');}});\n" +
            "            AjaxRefreshers.addRefresher(dashboard_ajax_refresher, true);\n" +
            "        });\n" +
            "    </script>\n" +
            "</div>");
    });

    var originalAjaxRequest = jQuery.ajax;
    var element_under_test;
    var newPageUrl = null;
    var util_load_page_fn = null;
    var xhr = null;

    function isBrowserIE() {
        return window.navigator.userAgent.toLowerCase().indexOf("msie") != -1;
    }

    beforeEach(function () {
        element_under_test = $$('.under_test').first().innerHTML;
        util_load_page_fn = Util.loadPage;
        Util.loadPage = function (url) {
            newPageUrl = url;
        };
        xhr = {
            getResponseHeader: function (name) {
                return "holy_cow_new_url_is_sooooo_cool!!!";
            }
        };
    });
    afterEach(function () {
        AjaxRefreshers.clear();
        Util.loadPage = util_load_page_fn;
        jQuery.ajax = originalAjaxRequest;
        $$('.under_test').first().update(element_under_test);
    });

    it("test_add_newly_added_group_to_the_page", function () {
        jQuery.ajax = function (options) {
            options.success({"pipeline_group_group-3_panel": {html: "<div class='pipelines'><div class='content_wrapper_outer'><div class='content_wrapper_inner'>pipeline</div></div></div>", parent_id: "pipeline_groups_container", type: "group_of_pipelines"},
                "pipeline_third_panel": {html: "Pipeline3", parent_id: "pipeline_group_group-3_panel", type: "pipeline"}});
            options.complete(xhr);
        };
        var refresher = new DashboardAjaxRefresher("http//blah/pipelines.html", {redirectUrl: "foo", className: ".pipeline_group"});
        refresher.stopRefresh();
        refresher.restartRefresh();
        assertEquals("Pipeline3", $$("#pipeline_group_group-3_panel .pipelines .content_wrapper_outer .content_wrapper_inner #pipeline_third_panel")[0].innerHTML);
    });

    it("test_should_update_currently_present_pipeline", function () {
        jQuery.ajax = function (options) {
            options.success({"pipeline_group_group-1_panel": {html: "whatever", parent_id: "pipeline_groups_container", type: "group_of_pipelines"},
                "pipeline_pipeline-1_panel": {html: "Updated Pipeline", parent_id: "pipeline_group_group-1_panel", type: "pipeline"}});
            options.complete(xhr);
        };
        var refresher = new DashboardAjaxRefresher("http//blah/pipelines.html", {redirectUrl: "foo", className: ".pipeline_group"});
        refresher.stopRefresh();
        refresher.restartRefresh();
        assertEquals("Updated Pipeline", $$("#pipeline_group_group-1_panel #pipeline_pipeline-1_panel").first().innerHTML);
    });

    it("test_should_honor_tag_attr_for_newly_added_elements", function () {
        jQuery.ajax = function (options) {
            options.success({
                "pipeline_group_group-3_panel": {html: "<div class='pipelines'><div class='content_wrapper_outer'><div class='content_wrapper_inner'>pipeline</div></div></div>", parent_id: "pipeline_groups_container", type: "group_of_pipelines", tag_attr: {className: "group_class", title: "group_title"}},
                "newly_added_pipeline_with_attrs": {html: "First Pipeline", parent_id: "pipeline_group_group-3_panel", index: 0, type: 'pipeline', tag_attr: {className: "pipeline", title: "pipeline_title"}}});
            options.complete(xhr);
        };
        var refresher = new DashboardAjaxRefresher("http//blah/pipelines.html", {redirectUrl: "foo", className: ".pipeline_group"});
        refresher.stopRefresh();
        refresher.restartRefresh();
        assertEquals("must update css class for group div", "group_class", $("pipeline_group_group-3_panel").readAttribute("class"));
        assertEquals("must update title for group div", "group_title", $("pipeline_group_group-3_panel").readAttribute("title"));
        assertEquals("must update css class for pipeline div", "pipeline", $("newly_added_pipeline_with_attrs").readAttribute("class"));
        assertEquals("must update title for pipeline div", "pipeline_title", $("newly_added_pipeline_with_attrs").readAttribute("title"));
    });

    it("test_add_newly_added_group_at_the_given_index", function () {
        jQuery.ajax = function (options) {
            options.success({"pipeline_group_group-1_panel": {html: "<div class='pipeline_group'>group1</div>", parent_id: "pipeline_groups_container", index: 0, type: "group_of_pipelines"},
                "pipeline_group_group-2_panel": {html: "<div class='pipeline_group'>group2</div>", parent_id: "pipeline_groups_container", index: 1, type: "group_of_pipelines"},
                "newly_added_group": {html: "<div class='pipeline_group'>new_text</div>", parent_id: "pipeline_groups_container", index: 2, type: "group_of_pipelines"}});
            options.complete(xhr);
        };
        var refresher = new DashboardAjaxRefresher("http//blah/pipelines.html", {redirectUrl: "foo", className: ".pipeline_group"});
        refresher.stopRefresh();
        refresher.restartRefresh();
        var groups = $$(".pipeline_group");

        assertEquals(3, groups.length);
        assertEquals("new_text", groups[2].innerHTML);
    });

    it("test_remove_removed_group_from_the_page", function () {
        jQuery.ajax = function (options) {
            options.success({"pipeline_group_group-1_panel": {html: "group1", parent_id: "pipeline_groups_container", type: "group_of_pipelines"}});
            options.complete(xhr);
        };
        var refresher = new DashboardAjaxRefresher("http//blah/pipelines.html", {redirectUrl: "foo", className: ".pipeline_group"});
        refresher.stopRefresh();
        refresher.restartRefresh();
        assertTrue($$("#pipeline_group_group-1_panel").length == 1);
        assertTrue($$("#pipeline_group_group-2_panel").length == 0);
    });


    it("test_should_return_false_if_group_already_present", function () {
        jQuery.ajax = function (options) {
            options.success({"pipeline_group_group-1_panel": {html: "group1", parent_id: "pipeline_groups_container", type: "group_of_pipelines"}});
            options.complete(xhr);
        };
        new DashboardAjaxRefresher("http//blah/pipelines.html", {redirectUrl: "foo", className: ".pipeline_group"});
        assertFalse("group1" === $("pipeline_group_group-1_panel").innerHTML);
    });


    it("test_should_add_update_and_remove_pipelines_of_an_existing_group", function () {
        if (!isBrowserIE()) {                    //Fails only in IE. Temporary workaround
            jQuery.ajax = function (options) {
                options.success({
                    "pipeline_group_group-1_panel": { html: "<div class='pipelines'><div class='content_wrapper_outer'><div class='content_wrapper_inner'>pipeline</div></div></div>", parent_id: "pipeline_groups_container", type: 'group_of_pipelines'},
                    "newly_added_pipeline1": {html: "First Pipeline", parent_id: "pipeline_group_group-1_panel", index: 0, type: 'pipeline'},
                    "pipeline_pipeline-1_panel": {html: "New Text", parent_id: "pipeline_group_group-1_panel", index: 1, type: 'pipeline'},
                    "newly_added_pipeline2": {html: "Second Pipeline", parent_id: "pipeline_group_group-1_panel", index: 2, type: 'pipeline'}
                });
                options.complete(xhr);
            };
            var refresher = new DashboardAjaxRefresher("http//blah/pipelines.html", {redirectUrl: "foo", className: ".pipeline_group"});
            refresher.stopRefresh();
            refresher.restartRefresh();
            var pipelines = $$("#pipeline_group_group-1_panel .pipelines .content_wrapper_outer .content_wrapper_inner .pipeline");
            assertEquals(3, pipelines.length);
            assertEquals("newly_added_pipeline1", pipelines[0].id);
            assertEquals("First Pipeline", pipelines[0].innerHTML);

            assertEquals("pipeline_pipeline-1_panel", pipelines[1].id);
            assertEquals("New Text", pipelines[1].innerHTML);

            assertEquals("newly_added_pipeline2", pipelines[2].id);
            assertEquals("Second Pipeline", pipelines[2].innerHTML);

            assertNull("Second group must have been null", $("pipeline_group_group-2_panel"));
            assertNull("pipeline_pipeline-2_panel Must have been null", $("pipeline_pipeline-2_panel"));
            assertNull("pipeline_pipeline-3_panel Must have been null", $("pipeline_pipeline-3_panel"));
            assertNull("pipeline_pipeline-4_panel Must have been null", $("pipeline_pipeline-4_panel"));
        }
    });


    it("test_should_remove_pipelines_of_an_existing_group", function () {
        if (!isBrowserIE()) {                    //Fails only in IE. Temporary workaround
            jQuery.ajax = function (options) {
                options.success({
                    "pipeline_group_group-1_panel": { html: "<div class='pipelines'><div class='content_wrapper_outer'><div class='content_wrapper_inner'>pipeline</div></div></div>", parent_id: "pipeline_groups_container", type: 'group_of_pipelines'},
                    "pipeline_pipeline-2_panel": {html: "New Text", parent_id: "pipeline_group_group-1_panel", index: 0, type: 'pipeline'}
                });
                options.complete(xhr);
            };
            var refresher = new DashboardAjaxRefresher("http//blah/pipelines.html", {redirectUrl: "foo", className: ".pipeline_group"});
            refresher.stopRefresh();
            refresher.restartRefresh();
            var pipelines = $$("#pipeline_group_group-1_panel .pipelines .content_wrapper_outer .content_wrapper_inner .pipeline");
            assertEquals(1, pipelines.length);
            assertEquals("pipeline_pipeline-2_panel", pipelines[0].id);
            assertEquals("New Text", pipelines[0].innerHTML);

            assertNull("Second group must have been null", $("pipeline_group_group-2_panel"));
            assertNull("pipeline_pipeline-1_panel Must have been null", $("pipeline_pipeline-1_panel"));
            assertNull("pipeline_pipeline-3_panel Must have been null", $("pipeline_pipeline-3_panel"));
            assertNull("pipeline_pipeline-4_panel Must have been null", $("pipeline_pipeline-4_panel"));
        }
    });


    it("test_should_update_when_pipelines_are_moved_between_groups", function () {
        jQuery.ajax = function (options) {
            options.success({
                "pipeline_group_group-1_panel": { html: "<div class='pipelines'><div class='content_wrapper_outer'><div class='content_wrapper_inner'>pipeline</div></div></div>", parent_id: "pipeline_groups_container", type: 'group_of_pipelines'},
                "pipeline_group_group-2_panel": { html: "<div class='pipelines'><div class='content_wrapper_outer'><div class='content_wrapper_inner'>pipeline</div></div></div>", parent_id: "pipeline_groups_container", type: 'group_of_pipelines'},
                "pipeline_pipeline-2-1_panel": {html: "New Text", parent_id: "pipeline_group_group-1_panel", index: 0, type: 'pipeline'},
                "pipeline_pipeline-1_panel": {html: "First Pipeline", parent_id: "pipeline_group_group-2_panel", index: 0, type: 'pipeline'}
            });
            options.complete(xhr);
        };
        var refresher = new DashboardAjaxRefresher("http//blah/pipelines.html", {redirectUrl: "foo", className: ".pipeline_group"});
        refresher.stopRefresh();
        refresher.restartRefresh();
        assertTrue("Pipeline from group 2 must have been under group 1", $$("#pipeline_group_group-1_panel .pipelines .content_wrapper_outer .content_wrapper_inner DIV#pipeline_pipeline-2-1_panel").length == 1);
        assertTrue("Pipeline from group 1 must have been under group 2", $$("#pipeline_group_group-2_panel .pipelines .content_wrapper_outer .content_wrapper_inner DIV#pipeline_pipeline-1_panel").length == 1);
    });


    it("test_should_work_with_pipelines_with_dot", function () {
        var html = '<div id="pipeline_group_group.1_panel">' +
            '<div class="pipeline_bundle pipeline_group">' +
            '<h2 class="entity_title">group.1</h2>' +
            '<div class="pipelines">' +
            '<div class="content_wrapper_outer"><div class="content_wrapper_inner"> ' +
            '<div id="pipeline_pipeline.1_panel"  class="pipeline"> <h3 class="title entity_title">pipeline.1' +
            '</h3></div></div></div></div></div></div>';

        $("pipeline_groups_container").update(html);
        jQuery.ajax = function (options) {
            options.success({
                "pipeline_group_group.1_panel": { html: "<div class='pipelines'><div class='content_wrapper_outer'><div class='content_wrapper_inner'>pipeline</div></div></div>", parent_id: "pipeline_groups_container", type: 'group_of_pipelines'},
                "pipeline_pipeline.1_panel": {html: "New Text", parent_id: "pipeline_group_group.1_panel", index: 0, type: 'pipeline'}
            });
            options.complete(xhr);
        };
        var refresher = new DashboardAjaxRefresher("http//blah/pipelines.html", {redirectUrl: "foo", className: ".pipeline_group"});
        refresher.stopRefresh();
        refresher.restartRefresh();
        assertTrue("Pipeline group must be present", $("pipeline_group_group.1_panel") != null);
        assertTrue("Pipeline must be present", $("pipeline_group_group.1_panel").select(".pipelines .content_wrapper_outer .content_wrapper_inner div[id='pipeline_pipeline.1_panel']").length == 1);
    });
});


