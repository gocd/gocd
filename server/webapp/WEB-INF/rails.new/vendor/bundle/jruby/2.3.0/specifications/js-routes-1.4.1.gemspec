# -*- encoding: utf-8 -*-
# stub: js-routes 1.4.1 ruby lib

Gem::Specification.new do |s|
  s.name = "js-routes".freeze
  s.version = "1.4.1"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Bogdan Gusiev".freeze]
  s.date = "2017-09-05"
  s.description = "Generates javascript file that defines all Rails named routes as javascript helpers".freeze
  s.email = "agresso@gmail.com".freeze
  s.extra_rdoc_files = ["LICENSE.txt".freeze]
  s.files = ["LICENSE.txt".freeze]
  s.homepage = "http://github.com/railsware/js-routes".freeze
  s.licenses = ["MIT".freeze]
  s.rubygems_version = "2.6.13".freeze
  s.summary = "Brings Rails named routes to javascript".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<railties>.freeze, [">= 3.2"])
      s.add_runtime_dependency(%q<sprockets-rails>.freeze, [">= 0"])
      s.add_development_dependency(%q<rspec>.freeze, [">= 3.0.0"])
      s.add_development_dependency(%q<bundler>.freeze, [">= 1.1.0"])
      s.add_development_dependency(%q<coffee-script>.freeze, [">= 0"])
      s.add_development_dependency(%q<appraisal>.freeze, [">= 0.5.2"])
      s.add_development_dependency(%q<byebug>.freeze, [">= 0"])
      s.add_development_dependency(%q<pry-byebug>.freeze, [">= 0"])
      s.add_development_dependency(%q<therubyracer>.freeze, [">= 0.12.3"])
    else
      s.add_dependency(%q<railties>.freeze, [">= 3.2"])
      s.add_dependency(%q<sprockets-rails>.freeze, [">= 0"])
      s.add_dependency(%q<rspec>.freeze, [">= 3.0.0"])
      s.add_dependency(%q<bundler>.freeze, [">= 1.1.0"])
      s.add_dependency(%q<coffee-script>.freeze, [">= 0"])
      s.add_dependency(%q<appraisal>.freeze, [">= 0.5.2"])
      s.add_dependency(%q<byebug>.freeze, [">= 0"])
      s.add_dependency(%q<pry-byebug>.freeze, [">= 0"])
      s.add_dependency(%q<therubyracer>.freeze, [">= 0.12.3"])
    end
  else
    s.add_dependency(%q<railties>.freeze, [">= 3.2"])
    s.add_dependency(%q<sprockets-rails>.freeze, [">= 0"])
    s.add_dependency(%q<rspec>.freeze, [">= 3.0.0"])
    s.add_dependency(%q<bundler>.freeze, [">= 1.1.0"])
    s.add_dependency(%q<coffee-script>.freeze, [">= 0"])
    s.add_dependency(%q<appraisal>.freeze, [">= 0.5.2"])
    s.add_dependency(%q<byebug>.freeze, [">= 0"])
    s.add_dependency(%q<pry-byebug>.freeze, [">= 0"])
    s.add_dependency(%q<therubyracer>.freeze, [">= 0.12.3"])
  end
end
