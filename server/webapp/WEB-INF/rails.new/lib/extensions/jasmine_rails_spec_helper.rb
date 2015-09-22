if defined?(JasmineRails)
  module JasmineRails
    module SpecHelper
      # Gives us access to the require_js_include_tag helper
      include RequirejsHelper
    end
  end
end

if defined?(Jasmine)
  if ENV['REQUIRE_JS'] == 'true'
    # elaborate hax to get jasmine running with requirejs
    # based on the blog here -
    # https://www.airpair.com/jasmine/posts/javascriptintegrating-jasmine-with-requirejs-amd
    module Jasmine
      def self.runner_template
        File.read(Rails.root.join("spec/new_javascripts/run.html.erb"))
      end
    end
  end
end
