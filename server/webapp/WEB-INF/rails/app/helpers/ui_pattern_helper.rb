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

module UiPatternHelper
  
  def truncated(options={})
    truncated_text = options[:text]
    
    truncated_class = ""
    if defined?(options[:limit]) && !options[:limit].nil?
      if truncated_text.length > options[:limit] 
        truncated_class = "has_truncated_tip"
      end
      truncated_text = truncate(truncated_text, :length => (options[:limit]) )
    end
    
    truncated_text = "<span title=\"#{options[:text]}\" class=\"pattern_truncated #{truncated_class}\">" + truncated_text + "</span>"
  end

  def action_icon(options={})
    icon_html = ""
    
    icon_id_html = defined?(options[:id]) && !options[:id].nil? ? "id="+options[:id] : ""
    
    icon_class = "action_icon"
    if defined?(options[:class]) && !options[:class].nil?
      icon_class += " " + options[:class]
    end
    icon_alt_text = ""
    
    case options[:type]
    when "add"
      icon_class += " add_icon"
      icon_alt_text = options[:text] ? "" : "Add"
    when "remove"
      icon_class += " remove_icon"
      icon_alt_text = options[:text] ? "" : "Remove"
    when "delete"
      icon_class += " delete_icon"
      icon_alt_text = options[:text] ? "" : "Delete"
    when "edit"
      icon_class += " edit_icon"
      icon_alt_text = options[:text] ? "" : "Edit"
    when "move"
      icon_class += " move_icon"
      icon_alt_text = options[:text] ? "" : "Move"
    when "clone"
      icon_class += " clone_icon"
      icon_alt_text = options[:text] ? "" : "Clone"
    when "lock"
      icon_class += " lock_icon"
      icon_alt_text = options[:text] ? "" : "Lock"
    else
    end

    if defined?(options[:disabled]) && options[:disabled]
      icon_class += "_disabled"
    end
    
    icon_href = nil
    icon_dom = "span"
    if defined?(options[:href]) && !options[:href].nil?
      icon_href = options[:href]
      icon_dom = "a"
    end
    
    icon_title_text = defined?(options[:title]) && !options[:title].nil? ? options[:title] : icon_alt_text
    

    icon_html += "<#{icon_dom} #{icon_id_html} class=\"#{icon_class}\" title=\"#{icon_title_text}\""
    if !icon_href.nil?
      icon_html += "href=\"#{icon_href}\""
    end
    icon_html += ">"
    icon_html += "<span class=\"icon\"></span>"
    
    if options[:text]
      if options[:menu_link]
        icon_html += "<span class=\"menu_link\">#{options[:text]}</span>"
      else
        icon_html += "<span>#{options[:text]}</span>"
      end
    end
    icon_html += "</#{icon_dom}>"
    
    return icon_html.html_safe
  end
end