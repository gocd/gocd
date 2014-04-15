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

class TocHandler < ElementHandler
  def initialize(html, root)
    super(html, root)
  end
  
  def handle_index(element)
    @html.element('ul', 'class' => 'toc') do
      apply(element)
    end
  end

  def handle_entry(element)
    @html.element('li') do
      ref = element.attributes['reference']
      title = element.attributes['title']
      url = element.attributes['url']
      target = element.attributes['target']
      req_class = element.attributes['reqClass']
      actual_file = File.join(File.dirname(__FILE__), '..', 'topics', "#{ref}.xml") if ref
      is_pro = element.attributes['pro']

      if ref && File.exist?(actual_file)
        unless title
          root = REXML::Document.new(File.new(actual_file)).root
          title = root.attributes['title']
        end
        if @mode && @mode == 'book'
          href = "\##{ref}"
        else
          href = "#{ref}.html"
          anchor = element.attributes['anchor']
          href += "\##{anchor}" if anchor
        end
        @html.element('a', 'href' => href, 'class' => req_class){
          add_entry_text(title, is_pro)
        }
      elsif url
        title ||= url
        href = url
        anchor = element.attributes['anchor']
        href += "\##{anchor}" if anchor
        @html.element('a', 'href' => href,'target'=>target) {
          add_entry_text(title, is_pro)
        }
      else
        title ||= ref.gsub(/_/, ' ').capitalize.gsub(/cruise/, 'Cruise')
        @html.element('div'){
          add_entry_text(title, is_pro)
        }
      end
      
      if element.elements.size > 0
        @html.element('ul') do
          apply(element)
        end
      else
        apply(element)
      end
    end
  end

  def add_entry_text(title, is_pro)
    @html.text(title)
    @html.element('img', :src => 'resources/images/pro.png', :class => 'pro', :title => 'Professional Feature', :alt => 'Professional Feature'){} if is_pro
  end
  
  def setMode(mode)
    @mode = mode
    self
  end
end
