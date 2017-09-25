require 'bundler/setup'
require 'bundler/gem_tasks'
require 'rspec/core/rake_task'

RSpec::Core::RakeTask.new(:rspec)

desc 'Run the test suite'
task :default => :rspec

namespace :assets do
  desc 'Update Foundation for Sites assets'
  task update: :clean do
    sh 'bower install'
    sh 'cp -R bower_components/foundation-sites/js/* vendor/assets/js/'
    sh 'cp -R bower_components/foundation-sites/scss/* vendor/assets/scss/'
    sh 'cp -R bower_components/foundation-sites/scss/settings/_settings.scss lib/generators/foundation/templates'
    sh 'cp -R bower_components/motion-ui/src/* vendor/assets/scss/motion-ui'

    js_files = Dir['vendor/assets/js/*.js'].sort
    # Move foundation.core.js to beginning of js_files
    js_files.insert(0, js_files.delete(js_files.find { |file| file[/foundation.core.js/] }))
    manifest = js_files.map { |file| "//= require #{File.basename(file)}" }.join("\n")
    File.write('vendor/assets/js/foundation.js', manifest)

    Dir['vendor/assets/js/*.js'].each do |file|
      sh "mv #{file} #{file}.es6"
    end

    puts "\n*********************\n** ASSETS UPDATED! **\n*********************\n"
  end

  desc 'Remove old Foundation for Sites assets'
  task :clean do
    sh 'rm -rf vendor'
    sh 'mkdir -p vendor/assets/js/ vendor/assets/scss vendor/assets/scss/motion-ui'
  end

end
