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

function initPipelineVisualization() {
    //get pipeline_visualization container
    var container = $('pipeline_visualization');

    //storage of horizontal lines for later creating vertical lines
    var hLines = {
        up:{
            topLine: null,
            bottomLine: null
        },
        current:{
            leftLine: null,
            rightLine: null
        },
        down:{
            topLine: null,
            bottomLine: null
        }
    }

    //get containers: upstream, downstrea, current

    var upContainer = container.getElementsByClassName("upstream")[0];

    var upPipelines = upContainer.getElementsByClassName("pipeline");
    var downContainer = container.getElementsByClassName("downstream")[0];
    var downPipelines = downContainer.getElementsByClassName("pipeline");
    var current = container.getElementsByClassName("current")[0];

    //prep needed info for later use
    var currentPipeline = current.getElementsByClassName("pipeline")[0];
    var currentPipelineLeft = current.positionedOffset().left
    var columnWidth = null;

    if (upPipelines.length > 0) {

        for (i = 0; i < upPipelines.length; i++) {
            var pipeline = upPipelines[i];
            //for each upstream pipeline find it's middle
            var middleV = pipeline.getHeight()/2;

            //find it's top right corner position
            var top = pipeline.positionedOffset().top;
            var right = pipeline.positionedOffset().left+pipeline.getWidth();

            //find length from it's right side to current pipeline's left side
            columnWidth = current.positionedOffset().left - right;

            //draw a horizontal line 1/2 the length of the above measurement
            var hLine = createHorizontalLine();
            var hLineId = "upstream_hLine"+i;
            removeOldLine(hLineId);
            var hLineWidth = columnWidth/2;
            hLine.id= hLineId;
            hLine.setStyle({
                top: top + middleV + "px",
                left: right + "px",
                width: hLineWidth + "px"
            });

            upContainer.insertBefore(hLine, pipeline);

            if( i == 0 ){
                //save styles for making vertical line
                hLines.up.topLine = {
                    top: top + middleV,
                    left: right,
                    width: hLineWidth
                }
            }else if( i == upPipelines.length-1 ){
                hLines.up.bottomLine = {
                    top: top + middleV,
                    left: right,
                    width: hLineWidth
                }
            }
        }
    }

    if(columnWidth == null && downPipelines.length > 0){
        columnWidth = downContainer.positionedOffset().left - (current.positionedOffset().left + current.getWidth());
    }


    //current pipeline lines
    var pipeline = currentPipeline;
    var middleV = pipeline.getHeight()/2;

    //find it's top right corner position
    var top = pipeline.positionedOffset().top;
    var right = pipeline.positionedOffset().left+pipeline.getWidth();

    //find length from it's right side to current pipeline's left side
    columnWidth = current.positionedOffset().left - right;

    if (downPipelines.length > 0) {
        var hLine = createHorizontalLine();
        var hLineId = "current_right_line";
        var hLineWidth = columnWidth/2;
        removeOldLine(hLineId);
        hLine.id= hLineId;
        hLine.setStyle({
            top: top + middleV + "px",
            left: right + "px",
            width: hLineWidth + "px"
        });
        hLines.current.rightLine = {
            top: top + middleV,
            left: right ,
            width: hLineWidth
        }

        hLine = current.insertBefore(hLine, pipeline);
    }
    if (upPipelines.length > 0) {
        var hLine = createHorizontalLine();
        var hLineId = "current_left_line";
        var hLineWidth = columnWidth/2;
        removeOldLine(hLineId);
        hLine.id= hLineId;
        hLine.setStyle({
            top: top + middleV + "px",
            left: -hLineWidth + "px",
            width: hLineWidth + "px"
        });

        hLines.current.leftLine = {
            top: top + middleV,
            left: -hLineWidth,
            width: hLineWidth
        }
        hLine = current.insertBefore(hLine, pipeline);
        hLine.absolutize();
    }






    //get downstream pipelines
    if (downPipelines.length > 0) {
        for (i = 0; i < downPipelines.length; i++) {
            var pipeline = downPipelines[i];
            //for each upstream
            //find it's middle,
            var middleV = pipeline.getHeight()/2;

            //find it's top right corner position
            var top = pipeline.positionedOffset().top;
            var left =-columnWidth/2;


            //draw a horizontal line 1/2 the length of the above measurement
            var hLine = createHorizontalLine();
            var hLineId = "downstream_hLine"+i;
            var hLineWidth = columnWidth/2;
            removeOldLine(hLineId);
            hLine.id= hLineId;
            hLine.setStyle({
                top: top + middleV + "px",
                left: left + "px",
                width: hLineWidth + "px"
            });

            downContainer.insertBefore(hLine, pipeline);

            if( i == 0 ){
                //save styles for making vertical line
                hLines.down.topLine = {
                    top: top + middleV,
                    left: right,
                    width: hLineWidth
                }
            }
            if( i == downPipelines.length-1 ){
                hLines.down.bottomLine = {
                    top: top + middleV,
                    left: right,
                    width: hLineWidth
                }
            }

        }
    }


    //vertical lines
    var mostTopLine = null;
    var mostBottomLine = null;
    var flagMostTopIsCurrent = false;
    var flagMostBottomIsCurrent = false;
    if( hLines.up.topLine != null){
        if (hLines.up.topLine.top < hLines.current.leftLine.top){
//            console.log("topLine = up topLine");
            mostTopLine = hLines.up.topLine;
        }else{
//            console.log("topLine = current leftLine");
            mostTopLine = hLines.current.leftLine;
            flagMostTopIsCurrent = true;
        }
    }
    if( hLines.up.bottomLine != null ){
        if( hLines.up.bottomLine.top < hLines.current.leftLine.top){
            mostBottomLine = hLines.current.leftLine;
//            console.log("bottomLine = current leftLine");
        }else{
            mostBottomLine = hLines.up.bottomLine;
//            console.log("bottomLine = up bottomLine");
        }
    }else{
        if (hLines.up.topLine.top > hLines.current.leftLine.top){
//            console.log("bottomLine = up topLine");
            mostBottomLine = hLines.up.topLine;
        }else{
//            console.log("bottomLine = current leftLine");
            mostBottomLine = hLines.current.leftLine;
            flagMostBottomIsCurrent = true;
        }
    }
    if( mostTopLine != null && mostBottomLine != null){
        //we have more than 1 pipeline in upstream, so we need a vertical line
        //who is higher: upstream pipe 1's horizontal line, or current pipeline's left line?

        //draw vertical line joining all of upstream
        var vLine = createVerticalLine();
        vLine.id = "upstream_vertical_line";
        removeOldLine(vLine.id);
        vLine.setStyle({
            height: mostBottomLine.top - mostTopLine.top + 1 + "px",
            top: mostTopLine.top + "px",
            left: mostBottomLine.left + mostBottomLine.width + "px"
        });

        if(flagMostTopIsCurrent){
            vLine.setStyle({
                left: mostTopLine.left + "px"    
            });
            current.appendChild(vLine);
        }else if(flagMostBottomIsCurrent){
            vLine.setStyle({
                left: mostBottomLine.left + "px"    
            });
            current.appendChild(vLine);
        }else{
            container.appendChild(vLine);
        }
    }


    var mostTopLine = null;
    var mostBottomLine = null;
    if( hLines.down.bottomLine != null){
        mostTopLine = hLines.down.topLine.top < hLines.current.rightLine.top ? hLines.down.topLine : hLines.current.rightLine;
        mostBottomLine = hLines.down.bottomLine;
    }else if(hLines.down.topLine != null && hLines.down.topLine.top != hLines.current.rightLine.top){
        if( hLines.up.topLine.top < hLines.current.leftLine.top){
            mostTopLine = hLines.down.topLine;
            mostBottomLine = hLines.current.rightLine;
        }else{
            mostTopLine = hLines.current.rightLine;
            mostBottomLine = hLines.down.topLine;
        }
    }
    if( mostTopLine != null && mostBottomLine != null){
        //we have more than 1 pipeline in upstream, so we need a vertical line
        //who is higher: upstream pipe 1's horizontal line, or current pipeline's left line?

        //draw vertical line joining all of upstream
        var vLine = createVerticalLine();
        vLine.id = "downstream_vertical_line";
        removeOldLine(vLine.id);

        vLine.setStyle({
            height: hLines.down.bottomLine.top - mostTopLine.top + 1 + "px",
            top: mostTopLine.top + "px",
            left: hLines.current.rightLine.left + hLines.current.rightLine.width + "px"
        });
        current.appendChild(vLine);
    }
}


function removeOldLine(lineId){
    var testLine = $(lineId);
    if(testLine != null){
        testLine.parentNode.removeChild(testLine);
    }
}



function createHorizontalLine(){
    var line = new Element("div");
    line.addClassName("line");
    line.setStyle({
        height: "1px",
        overflow: "hidden",
        position: "absolute",
        top: "0",
        left: "0"
    });

    return line;
}



function createVerticalLine(){
    var line = new Element("div");
    line.addClassName("vertical_line");
    line.setStyle({
        width: "1px",
        position: "absolute",
        top: "0",
        left: "0"
    });
    return line;
}

