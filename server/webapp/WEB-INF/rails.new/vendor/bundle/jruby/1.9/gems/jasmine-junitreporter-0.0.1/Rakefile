require "bundler/gem_tasks"

desc "Update from the given tag"
task :update_from_library, [:tag] do |t, args|
  tag = args[:tag]
  sh "wget https://github.com/shepmaster/jasmine-junitreporter/raw/#{tag}/JUnitReporter.js"
  mv 'JUnitReporter.js', 'vendor/assets/javascripts/JUnitReporter.js'
end
