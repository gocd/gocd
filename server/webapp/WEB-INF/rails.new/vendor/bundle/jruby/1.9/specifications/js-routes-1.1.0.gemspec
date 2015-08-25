# -*- encoding: utf-8 -*-
# stub: js-routes 1.1.0 ruby lib

Gem::Specification.new do |s|
  s.name = "js-routes"
  s.version = "1.1.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Bogdan Gusiev"]
  s.date = "2015-08-05"
  s.description = "Generates javascript file that defines all Rails named routes as javascript helpers"
  s.email = "agresso@gmail.com"
  s.extra_rdoc_files = ["LICENSE.txt"]
  s.files = ["LICENSE.txt"]
  s.homepage = "http://github.com/railsware/js-routes"
  s.licenses = ["MIT"]
  s.require_paths = ["lib"]
  s.rubygems_version = "2.1.9"
  s.summary = "Brings Rails named routes to javascript"

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<railties>, [">= 3.2"])
      s.add_runtime_dependency(%q<sprockets-rails>, [">= 0"])
      s.add_development_dependency(%q<rspec>, [">= 3.0.0"])
      s.add_development_dependency(%q<bundler>, [">= 1.1.0"])
      s.add_development_dependency(%q<guard>, [">= 0"])
      s.add_development_dependency(%q<guard-coffeescript>, [">= 0"])
      s.add_development_dependency(%q<appraisal>, [">= 0.5.2"])
      s.add_development_dependency(%q<byebug>, [">= 0"])
      s.add_development_dependency(%q<therubyracer>, [">= 0.12.1"])
    else
      s.add_dependency(%q<railties>, [">= 3.2"])
      s.add_dependency(%q<sprockets-rails>, [">= 0"])
      s.add_dependency(%q<rspec>, [">= 3.0.0"])
      s.add_dependency(%q<bundler>, [">= 1.1.0"])
      s.add_dependency(%q<guard>, [">= 0"])
      s.add_dependency(%q<guard-coffeescript>, [">= 0"])
      s.add_dependency(%q<appraisal>, [">= 0.5.2"])
      s.add_dependency(%q<byebug>, [">= 0"])
      s.add_dependency(%q<therubyracer>, [">= 0.12.1"])
    end
  else
    s.add_dependency(%q<railties>, [">= 3.2"])
    s.add_dependency(%q<sprockets-rails>, [">= 0"])
    s.add_dependency(%q<rspec>, [">= 3.0.0"])
    s.add_dependency(%q<bundler>, [">= 1.1.0"])
    s.add_dependency(%q<guard>, [">= 0"])
    s.add_dependency(%q<guard-coffeescript>, [">= 0"])
    s.add_dependency(%q<appraisal>, [">= 0.5.2"])
    s.add_dependency(%q<byebug>, [">= 0"])
    s.add_dependency(%q<therubyracer>, [">= 0.12.1"])
  end
end
