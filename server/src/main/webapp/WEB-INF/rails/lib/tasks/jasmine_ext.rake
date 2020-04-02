task 'jasmine:require' => ['jasmine:do_not_load_css']

namespace :jasmine do
  task :do_not_load_css => [:environment] do
    require 'jasmine/version'
    # raise "Untested on version #{Jasmine::VERSION} of jasmine" unless Jasmine::VERSION == '3.2.0'
    # this is because there's apparently a bug that causes css assets to be compiled even through they are not
    # included in the jasmine config. This causes tests to take forever to load and eventually timeout in the browser
    Rails.application.config.assets.precompile.clear
    Rails.application.config.assets.precompile += %w( application.js lib/d3-3.1.5.min.js)
  end

end
