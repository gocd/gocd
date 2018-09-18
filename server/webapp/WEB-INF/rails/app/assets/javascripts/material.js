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

var Material = Class.create({
    initialize: function(material){
        return Object.extend(this, material);
    },
    isDependency: function(){
        return this.scmType.toLowerCase() == 'dependency';
    },
    isPackage: function() {
        return this.scmType.toLowerCase() == 'package';
    },
    name: function(){
        if (this.materialName) {
            return this.scmType + ' - ' + this.materialName + ' - ' + this.readableLocation();    
        } else {
            return this.scmType + ' - ' + this.readableLocation();
        }
    },
    shouldRenderModifications: function(){
        return !this.isDependency();
    },
    readableLocation: function(){
        if(!this.isDependency()){
            return this.location;
        } else {
            try{
                var paths = this.location.split(/\//ig);
                return 'pipeline ' + paths[0] + ' stage ' + paths[1];
            } catch(e){
                return this.location;
            }
        }
    }
});

var MaterialArray = Class.create({
    initialize: function(matrialsArray){
        this.all = $A(matrialsArray).map(function(material){
            return new Material(material);
        });
    }
});