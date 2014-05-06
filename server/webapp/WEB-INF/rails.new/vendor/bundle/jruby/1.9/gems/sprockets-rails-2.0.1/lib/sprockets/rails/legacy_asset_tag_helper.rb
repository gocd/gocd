require 'sprockets'

module Sprockets
  module Rails
    # Backports of AssetTagHelper methods for Rails 2.x and 3.x.
    module LegacyAssetTagHelper
      include ActionView::Helpers::TagHelper

      def javascript_include_tag(*sources)
        options = sources.extract_options!.stringify_keys
        sources.uniq.map { |source|
          tag_options = {
            "src" => path_to_javascript(source)
          }.merge(options)
          content_tag(:script, "", tag_options)
        }.join("\n").html_safe
      end

      def stylesheet_link_tag(*sources)
        options = sources.extract_options!.stringify_keys
        sources.uniq.map { |source|
          tag_options = {
            "rel" => "stylesheet",
            "media" => "screen",
            "href" => path_to_stylesheet(source)
          }.merge(options)
          tag(:link, tag_options)
        }.join("\n").html_safe
      end
    end
  end
end
