if defined?(Jasmine)
  module Jasmine
    def self.runner_template
      File.read(Rails.root.join("spec/javascripts/run.html.erb"))
    end
  end
end
