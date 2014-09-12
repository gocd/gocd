$:.push File.expand_path("../lib", __FILE__)

# Maintain your gem's version:
require "engine_project/version"

# Describe your gem and declare its dependencies:
Gem::Specification.new do |s|
  s.name        = "engine_project"
  s.version     = EngineProject::VERSION
  s.authors     = ["TODO: Your name"]
  s.email       = ["TODO: Your email"]
  s.homepage    = "TODO"
  s.summary     = "TODO: Summary of EngineProject."
  s.description = "TODO: Description of EngineProject."

  s.files = Dir["{app,config,db,lib}/**/*", "MIT-LICENSE", "Rakefile", "README.rdoc"]
  s.test_files = Dir["test/**/*"]

  s.add_dependency "rails", ">= 4.0.0.beta", "< 5.0"
  # s.add_dependency "jquery-rails"

  s.add_development_dependency "sqlite3"
end
