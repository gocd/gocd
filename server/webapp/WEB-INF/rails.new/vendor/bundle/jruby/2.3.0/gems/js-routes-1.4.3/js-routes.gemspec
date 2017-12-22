# coding: utf-8
lib = File.expand_path('../lib', __FILE__)
$LOAD_PATH.unshift(lib) unless $LOAD_PATH.include?(lib)
require 'js_routes/version'

Gem::Specification.new do |s|
  s.name = %q{js-routes}
  s.version = JsRoutes::VERSION

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Bogdan Gusiev"]
  s.description = %q{Generates javascript file that defines all Rails named routes as javascript helpers}
  s.email = %q{agresso@gmail.com}
  s.extra_rdoc_files = [
    "LICENSE.txt"
  ]
  s.files = `git ls-files`.split("\n")
  s.homepage = %q{http://github.com/railsware/js-routes}
  s.licenses = ["MIT"]
  s.require_paths = ["lib"]
  s.summary = %q{Brings Rails named routes to javascript}

  s.add_runtime_dependency(%q<railties>, [">= 3.2"])
  s.add_runtime_dependency(%q<sprockets-rails>)
  s.add_development_dependency(%q<rspec>, [">= 3.0.0"])
  s.add_development_dependency(%q<bundler>, [">= 1.1.0"])
  s.add_development_dependency(%q<coffee-script>, [">= 0"])
  s.add_development_dependency(%q<appraisal>, [">= 0.5.2"])
  if defined?(JRUBY_VERSION)
    s.add_development_dependency(%q<therubyrhino>, [">= 2.0.4"])
  else
    s.add_development_dependency(%q<byebug>)
    s.add_development_dependency(%q<pry-byebug>)
    s.add_development_dependency(%q<therubyracer>, [">= 0.12.3"])
  end
end
