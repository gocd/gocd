##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################GO-LICENSE-END##################################

module GoUtil
  import org.dom4j.io.SAXReader unless defined? SAXReader
  import java.io.StringBufferInputStream unless defined? StringBufferInputStream
  import java.util.HashMap unless defined? HashMap
  import org.dom4j.DocumentFactory unless defined? DocumentFactory

  def in_params map
    map.each do |key, value|
      controller.params[key] = value
      @request.path_parameters[key] = value
    end
  end

  def dom4j_root_for xml_string
    map = HashMap.new()
    map.put("a", "http://www.w3.org/2005/Atom")
    map.put("go", "http://www.thoughtworks-studios.com/ns/go")
    factory = DocumentFactory.getInstance()
    factory.setXPathNamespaceURIs(map)
    SAXReader.new().read(StringBufferInputStream.new(xml_string)).getRootElement()
  end

  def to_list(pipelines)
    list = java.util.ArrayList.new()
    pipelines.each { |p| list.add(p)}
    list
  end

  def stub_context_path obj
    allow(obj).to receive(:context_path).and_return("/go")
  end
end