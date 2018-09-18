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

describe("material", function () {
    var materials_json;

    beforeEach(function () {
        materials_json = [
            {
                scmType: 'Dependency',
                location: 'upstream1/NO_PIPELINE/firstStage/0'
            },
            {
                scmType: 'Subversion',
                location: 'http://localhost/svn/framework/trunk'
            },
            {
                scmType: 'Mercurial',
                location: 'http://localhost/hg/framework/trunk'
            },
            {
                scmType: 'Git',
                location: 'http://localhost/git/framework.git'
            },
            {
                scmType: 'Dependency',
                location: 'downstream1/PIPELINE1/firstStage/1'
            }
        ]
    });

    it("test_material_is_a_dependency_when_scm_type_is_dependency", function () {
        var material_json = {
            scmType: 'Dependency'
        }
        var material = new Material(material_json);
        assert(material.isDependency());

        material.scmType = 'dependency';
        assert(material.isDependency());
    });

    it("test_material_is_not_a_dependency_when_scm_type_is_not_dependency", function () {
        var material_json = {
            scmType: 'Subversion'
        }
        var material = new Material(material_json);
        assert(!material.isDependency());

        material.scmType = 'mercurial';
        assert(!material.isDependency());
    });

    it("test_material_name_should_tell_its_type_and_location", function () {
        var material_json = {
            scmType: 'Subversion',
            location: 'thoughtworks/beijing/cruise'
        }
        var material = new Material(material_json);
        assertEquals('Subversion - thoughtworks/beijing/cruise', material.name());
    });

    it("test_should_not_render_modifications_for_dependency", function () {
        var material_json = {
            scmType: 'Dependency'
        }
        var dependency = new Material(material_json);
        assert(!dependency.shouldRenderModifications());
    });

    it("test_can_access_all_material", function () {
        assertEquals(5, materials_json.size());
        var materials = new MaterialArray(materials_json);
        assert(Object.isArray(materials.all));
        assertEquals(5, materials.all.size());
    });

    it("test_material_should_give_a_readalbe_name", function () {
        var material_json = {
            scmType: 'Dependency',
            location: 'upstream/firstStage'
        }
        var dependency = new Material(material_json);
        assertEquals('pipeline upstream stage firstStage', dependency.readableLocation());
    });

    it("test_dependency_location_in_name_should_use_readableLocation", function () {
        var material_json = {
            scmType: 'Dependency',
            location: 'upstream/firstStage'
        }
        var dependency = new Material(material_json);
        assertEquals('Dependency - pipeline upstream stage firstStage', dependency.name());
    });
});
