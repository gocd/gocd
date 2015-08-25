$:.push File.expand_path("../lib", __FILE__)

# Maintain your gem's version:
require "requirejs/rails/version"

# Describe your gem and declare its dependencies:
Gem::Specification.new do |s|
  s.name        = "requirejs-rails"
  s.version     = Requirejs::Rails::Version
  s.authors     = ["John Whitley"]
  s.email       = ["whitley@bangpath.org"]
  s.homepage    = "http://github.com/jwhitley/requirejs-rails"
  s.summary     = "Use RequireJS with the Rails 3+ Asset Pipeline"
  s.description = "This gem provides RequireJS support for your Rails 3 application."

  git_test_files, git_files = `git ls-files`.split("\n").partition { |f| f =~ /^test/ }
  s.test_files = git_test_files
  s.files = git_files
  s.require_path = 'lib'

  s.add_dependency "railties", ">= 3.1.1"
  s.add_development_dependency "rails", ">= 3.1.1"
  s.add_development_dependency "sqlite3"

  s.requirements << "node.js is required for 'rake assets:precompile', used to run the r.js build"
  s.requirements << "If needed, jQuery should be v1.7 or greater (jquery-rails >= 1.0.17)."
end
