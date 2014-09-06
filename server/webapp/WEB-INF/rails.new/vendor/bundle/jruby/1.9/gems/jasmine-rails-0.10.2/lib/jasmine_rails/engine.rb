require 'jasmine-core'

module JasmineRails
  class Engine < Rails::Engine
    isolate_namespace JasmineRails

    initializer :assets do |config|
      [Jasmine::Core.path, JasmineRails.include_dir, JasmineRails.spec_dir].flatten.compact.each do |dir|
        Rails.application.config.assets.paths << dir
      end
      Rails.application.config.assets.precompile += %w(jasmine.css boot.js jasmine-boot.js json2.js jasmine.js jasmine-html.js jasmine-console-shims.js jasmine-console-reporter.js jasmine-specs.js jasmine-specs.css)
    end
  end
end
