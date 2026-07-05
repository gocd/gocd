#
# Copyright Thoughtworks, Inc.
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
#

module MaterialsHelper
  include Services
  include ERB::Util

  def dependency_material?(material_type)
    material_type == com.thoughtworks.go.config.materials.dependency.DependencyMaterial::TYPE
  end

  def package_material?(material_type)
    material_type == com.thoughtworks.go.config.materials.PackageMaterial::TYPE
  end

  def render_comment_markup_for(comment, material_type, pipeline_name)
    if package_material?(material_type)
      render_comment_for_package_material(comment)
    else
      render_tracking_tool_link_for_comment(comment, pipeline_name).html_safe
    end
  end

  def render_comment(modification, material_type, pipeline_name)
    render_comment_markup_for(modification.getComment(), material_type, pipeline_name)
  end

  def render_comment_for_package_material(comment)
    return "".html_safe if comment.blank?

    package_comment = ActiveSupport::JSON.decode(comment)
    "#{render_comment_text(package_comment['COMMENT'])}Trackback: #{render_trackback_url(package_comment['TRACKBACK_URL'])}".html_safe
  end

  def render_tracking_tool_link_for_comment(comment, pipeline_name)
    comment_renderer = go_config_service.getCommentRendererFor(pipeline_name)
    old_simple_format comment_renderer.render(comment)
  end

  private

  def render_comment_text(comment_text)
    comment_text.blank? ? "" : "#{html_escape(comment_text)}<br/>"
  end

  def render_trackback_url(trackback_url)
    return 'Not Provided' if trackback_url.blank?

    http_url?(trackback_url) ? link_to(trackback_url, trackback_url, target: 'trackback') : html_escape(trackback_url)
  end

  def http_url?(url)
    url.match?(%r{\Ahttps?://}i) # kept consistent with the same basic check in vsm_renderer.js
  end

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