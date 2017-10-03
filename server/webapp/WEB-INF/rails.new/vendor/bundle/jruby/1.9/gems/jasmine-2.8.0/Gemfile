source 'https://rubygems.org'

gemspec

gem 'anchorman', :platform => :mri
gem 'octokit', '< 4.4', :platform => :mri
# during development, do not release
if ENV['TRAVIS']
  gem 'jasmine-core', :git => 'http://github.com/jasmine/jasmine.git'
else
  gem 'jasmine-core', :path => '../jasmine'
end

if ENV['RAILS_VERSION'] == "rails3"
  gem 'rack', '1.4.5'
elsif ENV['RAILS_VERSION'] == "rails4"
  gem 'rack', '1.5.2'
elsif ENV['RAILS_VERSION'] == "pojs"
  gem 'rack', '< 2.0'
else
  gem 'rack', '>= 2.0'
end

gem 'mime-types', '< 3.0', platform: [:mri_19, :jruby]

platform :rbx do
  gem 'json'
  gem 'rubysl'
  gem 'racc'
end
