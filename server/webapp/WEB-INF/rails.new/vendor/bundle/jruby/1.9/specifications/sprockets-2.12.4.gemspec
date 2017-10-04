# -*- encoding: utf-8 -*-
# stub: sprockets 2.12.4 ruby lib

Gem::Specification.new do |s|
  s.name = "sprockets"
  s.version = "2.12.4"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Sam Stephenson", "Joshua Peek"]
  s.date = "2015-06-26"
  s.description = "Sprockets is a Rack-based asset packaging system that concatenates and serves JavaScript, CoffeeScript, CSS, LESS, Sass, and SCSS."
  s.email = ["sstephenson@gmail.com", "josh@joshpeek.com"]
  s.executables = ["sprockets"]
  s.files = ["bin/sprockets"]
  s.homepage = "http://getsprockets.org/"
  s.licenses = ["MIT"]
  s.rubyforge_project = "sprockets"
  s.rubygems_version = "2.4.8"
  s.summary = "Rack-based asset packaging system"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<hike>, ["~> 1.2"])
      s.add_runtime_dependency(%q<multi_json>, ["~> 1.0"])
      s.add_runtime_dependency(%q<rack>, ["~> 1.0"])
      s.add_runtime_dependency(%q<tilt>, ["!= 1.3.0", "~> 1.1"])
      s.add_development_dependency(%q<closure-compiler>, [">= 0"])
      s.add_development_dependency(%q<coffee-script>, ["~> 2.0"])
      s.add_development_dependency(%q<coffee-script-source>, ["~> 1.2"])
      s.add_development_dependency(%q<eco>, ["~> 1.0"])
      s.add_development_dependency(%q<ejs>, ["~> 1.0"])
      s.add_development_dependency(%q<execjs>, ["~> 1.0"])
      s.add_development_dependency(%q<json>, [">= 0"])
      s.add_development_dependency(%q<rack-test>, [">= 0"])
      s.add_development_dependency(%q<rake>, [">= 0"])
      s.add_development_dependency(%q<sass>, ["~> 3.1"])
      s.add_development_dependency(%q<uglifier>, [">= 0"])
      s.add_development_dependency(%q<yui-compressor>, [">= 0"])
    else
      s.add_dependency(%q<hike>, ["~> 1.2"])
      s.add_dependency(%q<multi_json>, ["~> 1.0"])
      s.add_dependency(%q<rack>, ["~> 1.0"])
      s.add_dependency(%q<tilt>, ["!= 1.3.0", "~> 1.1"])
      s.add_dependency(%q<closure-compiler>, [">= 0"])
      s.add_dependency(%q<coffee-script>, ["~> 2.0"])
      s.add_dependency(%q<coffee-script-source>, ["~> 1.2"])
      s.add_dependency(%q<eco>, ["~> 1.0"])
      s.add_dependency(%q<ejs>, ["~> 1.0"])
      s.add_dependency(%q<execjs>, ["~> 1.0"])
      s.add_dependency(%q<json>, [">= 0"])
      s.add_dependency(%q<rack-test>, [">= 0"])
      s.add_dependency(%q<rake>, [">= 0"])
      s.add_dependency(%q<sass>, ["~> 3.1"])
      s.add_dependency(%q<uglifier>, [">= 0"])
      s.add_dependency(%q<yui-compressor>, [">= 0"])
    end
  else
    s.add_dependency(%q<hike>, ["~> 1.2"])
    s.add_dependency(%q<multi_json>, ["~> 1.0"])
    s.add_dependency(%q<rack>, ["~> 1.0"])
    s.add_dependency(%q<tilt>, ["!= 1.3.0", "~> 1.1"])
    s.add_dependency(%q<closure-compiler>, [">= 0"])
    s.add_dependency(%q<coffee-script>, ["~> 2.0"])
    s.add_dependency(%q<coffee-script-source>, ["~> 1.2"])
    s.add_dependency(%q<eco>, ["~> 1.0"])
    s.add_dependency(%q<ejs>, ["~> 1.0"])
    s.add_dependency(%q<execjs>, ["~> 1.0"])
    s.add_dependency(%q<json>, [">= 0"])
    s.add_dependency(%q<rack-test>, [">= 0"])
    s.add_dependency(%q<rake>, [">= 0"])
    s.add_dependency(%q<sass>, ["~> 3.1"])
    s.add_dependency(%q<uglifier>, [">= 0"])
    s.add_dependency(%q<yui-compressor>, [">= 0"])
  end
end
