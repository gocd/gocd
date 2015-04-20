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

module MaterialsHelper
  include Services

  def attributes_for_material(material)
    material.getSqlCriteria().inject("") do |s, entry|
      key, value = entry.first, entry.last
      value = material.getUriForDisplay() if key == "url"
      "#{s} #{key}=\"#{ERB::Util.h(value)}\""
    end
  end

  def current_modification? modification
    @deployed_revision.isRealRevision() && (modification.getRevision() == @deployed_revision.getRevision())
  end

  def latest_modification? modification
    modification.equals(@modifications.first())
  end

  def has_modification? material
    material_service.hasModificationFor(material)
  end

  def dependency_material? material
    material.getType() == "DependencyMaterial"
  end

  def stage_url_from_identifier locator
    stage_url(:id => stage_service.findStageIdByLocator(locator))
  end

  def render_simple_comment(comment)
    if /\"TYPE\":\"PACKAGE_MATERIAL\"/.match(comment)
      package_comment_map = package_material_display_comment(comment)
      trackback_url = package_comment_map['TRACKBACK_URL'].blank? ? l.string('NOT_PROVIDED') : package_comment_map['TRACKBACK_URL']
      result = package_comment_map['COMMENT'] || "#{l.string('TRACKBACK')}#{trackback_url}"
      return result
    end
    comment || ""
  end

  def render_comment_markup_for(comment, pipeline_name)
    if /\"TYPE\":\"PACKAGE_MATERIAL\"/.match(comment)
      render_comment_for_package_material(comment)
    else
      render_tracking_tool_link_for_comment(comment, pipeline_name).html_safe
    end
  end

  def render_comment(modification, pipeline_name)
    render_comment_markup_for(modification.getComment(), pipeline_name)
  end

  def render_comment_for_package_material(comment)
    package_comment_map = package_material_display_comment(comment)
    "#{get_comment(package_comment_map)}#{l.string('TRACKBACK')}#{get_trackback_url(package_comment_map)}".html_safe
  end

  def render_tracking_tool_link(modification, pipeline_name)
    render_tracking_tool_link_for_comment(modification.getComment(), pipeline_name)
  end

  def render_tracking_tool_link_for_comment(comment, pipeline_name)
    comment_renderer = go_config_service.getCommentRendererFor(pipeline_name)
    old_simple_format comment_renderer.render(comment)
  end


  def package_material_display_comment(comment)
    ActiveSupport::JSON.decode(comment)
  end

  def get_comment(comment_map)
    comment_map['COMMENT'].blank? ? "" : "#{comment_map['COMMENT']}<br>"
  end

  def get_trackback_url(comment_map)
    comment_map['TRACKBACK_URL'].blank? ? l.string('NOT_PROVIDED') : link_to(comment_map['TRACKBACK_URL'], comment_map['TRACKBACK_URL'])
  end

  def material_type_from_class(material)
    material.getClass().getSimpleName().gsub(/MaterialConfig$/, '').downcase
  end

  def admin_material_edit_path(material)
    send("admin_#{material_type_from_class(material)}_edit_path", :finger_print => material.getPipelineUniqueFingerprint())
  end


  private
  # They changed the implementation of this method between Rails 2.3 and Rails 4. Using the older one here.
  # OLD: https://github.com/rails/rails/blob/v2.3.18/actionpack/lib/action_view/helpers/text_helper.rb#L324
  # NEW: https://github.com/rails/rails/blob/v4.0.4/actionpack/lib/action_view/helpers/text_helper.rb#L264
  def old_simple_format(text, html_options={})
    start_tag = tag('p', html_options, true)
    text = text.to_s.dup
    text.gsub!(/\r\n?/, "\n")                    # \r\n and \r -> \n
    text.gsub!(/\n\n+/, "</p>\n\n#{start_tag}")  # 2+ newline  -> paragraph
    text.gsub!(/([^\n]\n)(?=[^\n])/, '\1<br />') # 1 newline   -> br
    text.insert 0, start_tag
    text << "</p>"
  end
end