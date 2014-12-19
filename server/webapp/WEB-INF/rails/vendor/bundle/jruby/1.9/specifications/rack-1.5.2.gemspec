# -*- encoding: utf-8 -*-
# stub: rack 1.5.2 ruby lib

Gem::Specification.new do |s|
  s.name = "rack"
  s.version = "1.5.2"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Christian Neukirchen"]
  s.date = "2013-02-08"
  s.description = "Rack provides a minimal, modular and adaptable interface for developing\nweb applications in Ruby.  By wrapping HTTP requests and responses in\nthe simplest way possible, it unifies and distills the API for web\nservers, web frameworks, and software in between (the so-called\nmiddleware) into a single method call.\n\nAlso see http://rack.github.com/.\n"
  s.email = "chneukirchen@gmail.com"
  s.executables = ["rackup"]
  s.extra_rdoc_files = ["README.rdoc", "KNOWN-ISSUES"]
  s.files = ["bin/rackup", "README.rdoc", "KNOWN-ISSUES"]
  s.homepage = "http://rack.github.com/"
  s.licenses = ["MIT"]
  s.require_paths = ["lib"]
  s.rubyforge_project = "rack"
  s.rubygems_version = "2.1.9"
  s.summary = "a modular Ruby webserver interface"

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<bacon>, [">= 0"])
      s.add_development_dependency(%q<rake>, [">= 0"])
    else
      s.add_dependency(%q<bacon>, [">= 0"])
      s.add_dependency(%q<rake>, [">= 0"])
    end
  else
    s.add_dependency(%q<bacon>, [">= 0"])
    s.add_dependency(%q<rake>, [">= 0"])
  end
end
