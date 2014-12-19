$:.push File.expand_path("../lib", __FILE__)

# Maintain your gem's version:
require "gadgets/version"

# Describe your gem and declare its dependencies:
Gem::Specification.new do |s|
  s.name        = "gadgets"
  s.version     = Gadgets::VERSION
  s.authors     = ["Go Team"]
  s.email       = ["go-cd-dev@gmail.com"]
  s.homepage    = "http://www.go.cd"
  s.summary     = "Gadget used for rendering mingle card activity."
  s.description = "Mingle Card Activity Gadget."

  s.files = Dir["{app,config,db,lib}/**/*", "MIT-LICENSE", "Rakefile", "README.rdoc"]

  s.add_dependency "rails", "~> 4.0.4"
  s.add_dependency "validatable", ">=1.6.0"
end
