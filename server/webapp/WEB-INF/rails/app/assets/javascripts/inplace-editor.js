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

var Local = Class.create();
Local.InplaceEditor = Class.create();

/*This is the hack for scriptacular Ajax.InPlaceEditor to have a local inplace edtior*/

Local.InplaceEditor.prototype = Object.extend(Ajax.InPlaceEditor.prototype, {
    _methodEnterEditMode : Ajax.InPlaceEditor.prototype.enterEditMode,
    createForm: function() {
        //create span inside
        this._form = $(document.createElement('span'));
        this._form.id = this.options.formId;
        this._form.addClassName(this.options.formClassName);
        this.createEditField();
        this.createControl('cancel', this._boundCancelHandler, 'editor_cancel');
    },
    enterEditMode: function(e) {
        this._methodEnterEditMode(e);
        this._form.select('input')[0].value='';
    }
});










