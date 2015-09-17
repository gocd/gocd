namespace :spec do

  desc "run test with phantomjs"
  task :javascript => [:environment] do
    require 'jasmine_rails/runner'
    spec_filter = ENV['SPEC']
    reporters = ENV.fetch('REPORTERS', 'console')
    JasmineRails::Runner.run spec_filter, reporters
  end

  # alias
  task :javascripts => :javascript
end
