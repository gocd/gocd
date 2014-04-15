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

class TopicHandler < ElementHandler
  
  def initialize(html, root)
    super(html, root)
  end
  
  def handle_chapter(element)
    handle_text_section(element, 1)
  end
  
  def handle_topic(element)
    handle_text_section(element, 2)
  end
  
  def handle_section(element)
    handle_text_section(element, 3)
  end
  
  def handle_subsection(element)
    handle_text_section(element, 4)
  end  
  
  def handle_text_section(element, heading_level, collapsed = false)
    if (element.attributes['file'])
      handle_included_text_section(element, heading_level)
    else
      handle_inline_text_section(element, heading_level, collapsed)
    end
  end
  
  def handle_included_text_section(element, heading_level)
    filename = "topics/#{element.attributes['file']}.xml"
    if File.exist?(filename)
      topic_root = REXML::Document.new(File.new(filename)).root
    else
      missing_section_name = File.basename(filename, '.xml').gsub(/_/, ' ')
      missing_section_name = missing_section_name[0...1].upcase + missing_section_name[1..-1]
      topic_root = REXML::Document.new(%{
        <topic title='#{missing_section_name}'>
          Source file #{filename} missing.
        </topic>
      }).root
    end
  	topic_handler = TopicHandler.new(@html, topic_root)
  	topic_handler.handle_text_section(topic_root, heading_level, element.attributes['collapsed'] == 'true')
  end
  
  def handle_inline_text_section(element, heading_level, collapsed)
    section_is_collapsed = element.attributes['collapsed'] == 'true' || collapsed
    @html.div(element.name) do
      heading_attributes = {'onclick' => 'toggleCollapse($(this));'}
      heading_id = element.attributes['id']
      heading_attributes.merge!({'id' => heading_id}) if heading_id
      if section_is_collapsed
        heading_attributes.merge!({'class' => 'collapsed-heading'})
      else
        heading_attributes.merge!({'class' => 'collapsible-heading'})
      end
      @html.element("h#{heading_level}", heading_attributes) do
        @html.text(element.attributes['title'])
      end
      collapsible_div_attributes = {'class' => 'content-container'}
      collapsible_div_attributes.merge!({'class' => 'content-container collapsed'}) if section_is_collapsed
      @html.element('div', collapsible_div_attributes) do
        apply(element)
      end
    end
  end
    
  def handle_p(element)
    @html.p do
      apply(element)
    end
  end

  def handle_i(element)
    @html.i do
      apply(element)
    end
  end
    
  def handle_note(element)
    handle_box("note", element)
  end

  def handle_warning(element)
    handle_box("warning", element)
  end
  
  def handle_hint(element)
    handle_box("hint", element)
    # @html.div('box hint') do
    #       @html.element('strong'){@html.text('Hint: ')}      
    #       apply(element)
    #     end
  end
  
  def handle_code(element)
    @html.pre('code') do
      apply(element)
    end
  end
  
  def handle_strong(element)
    @html.element('strong') do
      apply(element)
    end
  end
    
  def handle_bullets(element)
    css_class = nil
    
    title = element.attributes['title']
    if title
      @html.h(5, 'bullets-title') do
        @html.text(title)
      end
      css_class = 'titled-bullets'
    end
    
    @html.ul(css_class) do 
      apply(element)
    end
  end
  
  def handle_steps(element)
    css_class = nil
    
    title = element.attributes['title']
    if title
      @html.h(5, 'steps-title') do
        @html.text(title)
      end
      css_class = 'titled-steps'
    end
    
    @html.ol(css_class) do 
      apply(element)
    end
  end
  
  def handle_item(element)
    @html.li do
      apply(element)
    end
  end
  
  def handle_cref(element)
    topic = element.attributes['topic']
    if File.exist?(File.join(File.dirname(__FILE__), "../topics/#{topic}.xml"))
      if(@mode && @mode == 'book') then
        href = "\##{element.attributes['topic']}"
      else
        href = "#{element.attributes['topic']}.html"
	if (element.attributes['anchor']) then
	   href = href + "#" + element.attributes['anchor']
	end
      end
    else
      href = "under_construction.html"
    end
    @html.a_ref(href,nil) do
      apply(element)
    end
  end
  
  def handle_exref(element)
    href = element.attributes['url']
    target = element.attributes['url']
    if element.children.empty?
      @html.a_ref(href,target){@html.text href}
    else
      @html.a_ref(href,target) do
        apply(element)
      end
    end
  end
  
  def handle_video(element)
    handle_exref(element)
  end
  
  def handle_img(element, isIcon = false)
    image_path = element.attributes['src']
    placeholder = !File.exist?(image_path)
    image_path = image_path.gsub(/\/images\//, '/placeholder_images/') unless File.exist?(image_path)
    image_path = 'resources/images/placeholder_image.png' unless File.exist?(image_path)
    attributes = {:src => "#{image_path}"}
    attributes.merge!(:alt => element.attributes['alttext']) if element.attributes['alttext']
    attributes.merge!(:class => 'placeholder') if placeholder
    attributes.merge!(:class => placeholder ? 'placeholder icon' : 'icon') if isIcon
    @html.element('img', attributes){}
  end
  
  def handle_screenshot(element)
    handle_img(element)
  end

  def handle_todo(element)
    # @html.span('todo') do
    #   @html.text 'TODO: '
    #   apply(element)
    # end
  end
  
  def handle_br(element)
    @html.element('br'){apply(element)}
  end
  
  def handle_example(element)
    @html.element('div', 'class' => 'example'){apply(element)}
  end
  
  def handle_markup(element)
    @html.pre('markup'){apply(element)}
  end
  
  def handle_formula(element)
    @html.pre('formula'){apply(element)}
  end
  
  def handle_markup_reference(element)
    @html.element('div', 'class'  => 'markup-reference') do
      @html.p('title'){@html.text(element.attributes['title'])} if element.attributes['title']
      apply(element)
    end
  end
  
  def handle_icon(element)
    handle_img(element, true)
  end
  
  def handle_preview(element)
    @html.p('preview') do
      @html.element('h4', 'class' => 'preview'){
        @html.text 'Preview'
      }
      apply(element)
    end
  end
  
  def handle_subst(element)
    @html.span('subst'){apply(element)}
  end
  
  def handle_table(element)
    init_row_cycle
    @html.element('table') do
        @html.element('caption') {@html.text(element.attributes['caption'])} if element.attributes['caption']
        apply(element)
    end
  end
  
  def handle_header_row(element)
    has_group = false
    element.parent.elements.each do |child|
      if(child.name == 'group') then
        has_group = true
        break
      end
    end
    render_header_row(element) if !has_group
  end
  
  def render_header_row(element)
    @html.element('tr', 'class' => 'table-header' ) { apply element }
  end
  
  def find_header_row(table)
    table.elements.each do |child|
      return child if(child.name == 'header-row')
    end
  end
  
  def handle_row(element)
    @html.element('tr', 'class' => row_cycle ) { apply element }
  end
  
  def handle_col_header(element)
    @html.element('th'){ apply element }
  end
  
  def handle_label(element)
    @html.element('th'){ apply element }
  end
  
  def handle_col(element)
    @html.element('td'){ apply element }
  end

  def handle_youtubesingle(element)
    @html.element('iframe', 'width' => '560', 'height' => '315', 'src' => "http://www.youtube.com/embed/#{element.attributes['videoid']}", 'frameborder' => '0', 'allowfullscreen' => 'true') {apply element}
  end
  
  def handle_youtube(element) 
    @html.element('object', 'width' => '480', 'height' => '385') do
      @html.element('param', 'name' => 'movie', 'value' => "http://www.youtube.com/p/#{element.attributes['playlist']}"){ apply element }
      @html.element('param', 'name' => 'allowFullScreen', 'value' => 'true'){ apply element }
      @html.element('param', 'name' => 'allowscriptaccess', 'value' => 'always'){ apply element }
      @html.element('embed', 'src' => "http://www.youtube.com/p/#{element.attributes['playlist']}",
        'type' => 'application/x-shockwave-flash', 'width' => '480', 'height' => '385', 'allowscriptaccess' => 'always',
        'allowfullscreen' => 'true'){ apply element }
    end
  end
  
  def handle_group(element)
    @html.element('tr', 'class' => 'group-head') do
      colspan = 1
      element.parent.elements.each do |child|
        if(child.name == 'header-row') then
          colspan = child.elements.size()
          break
        end
      end
      @html.element('th', {'colspan' => colspan, 'class' => 'group-header'}) { @html.text element.attributes['title'] }
      render_header_row(find_header_row(element.parent)) if element.attributes['col-headers'] && element.attributes['col-headers'] == 'true'
    end
    apply(element)
  end
  
  def setMode(mode)
    @mode = mode
    self
  end
  
  private
  
  def row_cycle
    if @row_cycle_status == 'odd' then
      @row_cycle == 'even'
    else
      @row_cycle == 'odd'
    end
    
    @row_cycle
  end
  
  def init_row_cycle
    @row_cycle = 'odd'
  end
  
  def handle_box(box_type, element)
    @html.text(%{
      <div class="#{box_type}-box" style="padding: 0px 0pt 0pt">
    	  <div class="ab-bg">
      		<span class="ab-corner lvl1"></span>
      		<span class="ab-corner lvl2"></span>
      		<span class="ab-corner lvl3"></span>
      		<span class="ab-corner lvl4"></span>
      	</div>
      	<div class="box-content">
    })
    
    @html.p('box'){apply(element)}
    	
    @html.text(%{	
        </div>
    	  <div class="ab-bg">
      		<span class="ab-corner lvl4"></span>
      		<span class="ab-corner lvl3"></span>
      		<span class="ab-corner lvl2"></span>
      		<span class="ab-corner lvl1"></span>
      	</div>
      </div>
    })  
  end

end
