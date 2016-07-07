# -*- encoding: utf-8 -*-
# stub: sprockets-es6 0.9.0 ruby lib

Gem::Specification.new do |s|
  s.name = "sprockets-es6"
  s.version = "0.9.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Joshua Peek"]
  s.date = "2016-02-10"
  s.description = "    A Sprockets transformer that converts ES6 code into vanilla ES5 with Babel JS.\n"
  s.email = "josh@joshpeek.com"
  s.homepage = "https://github.com/TannerRogalsky/sprockets-es6"
  s.licenses = ["MIT"]
  s.require_paths = ["lib"]
  s.rubygems_version = "2.1.9"
  s.summary = "Sprockets ES6 transformer"

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<babel-transpiler>, [">= 0"])
      s.add_runtime_dependency(%q<babel-source>, [">= 5.8.11"])
      s.add_runtime_dependency(%q<sprockets>, [">= 3.0.0"])
      s.add_development_dependency(%q<rake>, [">= 0"])
      s.add_development_dependency(%q<minitest>, [">= 0"])
    else
      s.add_dependency(%q<babel-transpiler>, [">= 0"])
      s.add_dependency(%q<babel-source>, [">= 5.8.11"])
      s.add_dependency(%q<sprockets>, [">= 3.0.0"])
      s.add_dependency(%q<rake>, [">= 0"])
      s.add_dependency(%q<minitest>, [">= 0"])
    end
  else
    s.add_dependency(%q<babel-transpiler>, [">= 0"])
    s.add_dependency(%q<babel-source>, [">= 5.8.11"])
    s.add_dependency(%q<sprockets>, [">= 3.0.0"])
    s.add_dependency(%q<rake>, [">= 0"])
    s.add_dependency(%q<minitest>, [">= 0"])
  end
end
