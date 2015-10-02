# -*- encoding: utf-8 -*-
# stub: sprockets 3.3.4 ruby lib

Gem::Specification.new do |s|
  s.name = "sprockets"
  s.version = "3.3.4"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Sam Stephenson", "Joshua Peek"]
  s.date = "2015-09-01"
  s.description = "Sprockets is a Rack-based asset packaging system that concatenates and serves JavaScript, CoffeeScript, CSS, LESS, Sass, and SCSS."
  s.email = ["sstephenson@gmail.com", "josh@joshpeek.com"]
  s.executables = ["sprockets"]
  s.files = ["bin/sprockets"]
  s.homepage = "https://github.com/rails/sprockets"
  s.licenses = ["MIT"]
  s.require_paths = ["lib"]
  s.required_ruby_version = Gem::Requirement.new(">= 1.9.3")
  s.rubyforge_project = "sprockets"
  s.rubygems_version = "2.1.9"
  s.summary = "Rack-based asset packaging system"

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<rack>, ["~> 1.0"])
      s.add_development_dependency(%q<closure-compiler>, ["~> 1.1"])
      s.add_development_dependency(%q<coffee-script-source>, ["~> 1.6"])
      s.add_development_dependency(%q<coffee-script>, ["~> 2.2"])
      s.add_development_dependency(%q<eco>, ["~> 1.0"])
      s.add_development_dependency(%q<ejs>, ["~> 1.0"])
      s.add_development_dependency(%q<execjs>, ["~> 2.0"])
      s.add_development_dependency(%q<minitest>, ["~> 5.0"])
      s.add_development_dependency(%q<nokogiri>, ["~> 1.3"])
      s.add_development_dependency(%q<rack-test>, ["~> 0.6"])
      s.add_development_dependency(%q<rake>, ["~> 10.0"])
      s.add_development_dependency(%q<sass>, ["~> 3.1"])
      s.add_development_dependency(%q<uglifier>, ["~> 2.3"])
      s.add_development_dependency(%q<yui-compressor>, ["~> 0.12"])
    else
      s.add_dependency(%q<rack>, ["~> 1.0"])
      s.add_dependency(%q<closure-compiler>, ["~> 1.1"])
      s.add_dependency(%q<coffee-script-source>, ["~> 1.6"])
      s.add_dependency(%q<coffee-script>, ["~> 2.2"])
      s.add_dependency(%q<eco>, ["~> 1.0"])
      s.add_dependency(%q<ejs>, ["~> 1.0"])
      s.add_dependency(%q<execjs>, ["~> 2.0"])
      s.add_dependency(%q<minitest>, ["~> 5.0"])
      s.add_dependency(%q<nokogiri>, ["~> 1.3"])
      s.add_dependency(%q<rack-test>, ["~> 0.6"])
      s.add_dependency(%q<rake>, ["~> 10.0"])
      s.add_dependency(%q<sass>, ["~> 3.1"])
      s.add_dependency(%q<uglifier>, ["~> 2.3"])
      s.add_dependency(%q<yui-compressor>, ["~> 0.12"])
    end
  else
    s.add_dependency(%q<rack>, ["~> 1.0"])
    s.add_dependency(%q<closure-compiler>, ["~> 1.1"])
    s.add_dependency(%q<coffee-script-source>, ["~> 1.6"])
    s.add_dependency(%q<coffee-script>, ["~> 2.2"])
    s.add_dependency(%q<eco>, ["~> 1.0"])
    s.add_dependency(%q<ejs>, ["~> 1.0"])
    s.add_dependency(%q<execjs>, ["~> 2.0"])
    s.add_dependency(%q<minitest>, ["~> 5.0"])
    s.add_dependency(%q<nokogiri>, ["~> 1.3"])
    s.add_dependency(%q<rack-test>, ["~> 0.6"])
    s.add_dependency(%q<rake>, ["~> 10.0"])
    s.add_dependency(%q<sass>, ["~> 3.1"])
    s.add_dependency(%q<uglifier>, ["~> 2.3"])
    s.add_dependency(%q<yui-compressor>, ["~> 0.12"])
  end
end
