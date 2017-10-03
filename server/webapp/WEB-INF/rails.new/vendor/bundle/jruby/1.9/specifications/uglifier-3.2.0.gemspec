# -*- encoding: utf-8 -*-
# stub: uglifier 3.2.0 ruby lib

Gem::Specification.new do |s|
  s.name = "uglifier"
  s.version = "3.2.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Ville Lautanala"]
  s.date = "2017-04-09"
  s.description = "Uglifier minifies JavaScript files by wrapping UglifyJS to be accessible in Ruby"
  s.email = ["lautis@gmail.com"]
  s.extra_rdoc_files = ["LICENSE.txt", "README.md", "CHANGELOG.md", "CONTRIBUTING.md"]
  s.files = ["CHANGELOG.md", "CONTRIBUTING.md", "LICENSE.txt", "README.md"]
  s.homepage = "http://github.com/lautis/uglifier"
  s.licenses = ["MIT"]
  s.required_ruby_version = Gem::Requirement.new(">= 1.9.3")
  s.rubygems_version = "2.4.8"
  s.summary = "Ruby wrapper for UglifyJS JavaScript compressor"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<execjs>, ["< 3", ">= 0.3.0"])
      s.add_development_dependency(%q<rspec>, ["~> 3.0"])
      s.add_development_dependency(%q<rake>, ["~> 12.0"])
      s.add_development_dependency(%q<bundler>, ["~> 1.3"])
      s.add_development_dependency(%q<sourcemap>, ["~> 0.1.1"])
    else
      s.add_dependency(%q<execjs>, ["< 3", ">= 0.3.0"])
      s.add_dependency(%q<rspec>, ["~> 3.0"])
      s.add_dependency(%q<rake>, ["~> 12.0"])
      s.add_dependency(%q<bundler>, ["~> 1.3"])
      s.add_dependency(%q<sourcemap>, ["~> 0.1.1"])
    end
  else
    s.add_dependency(%q<execjs>, ["< 3", ">= 0.3.0"])
    s.add_dependency(%q<rspec>, ["~> 3.0"])
    s.add_dependency(%q<rake>, ["~> 12.0"])
    s.add_dependency(%q<bundler>, ["~> 1.3"])
    s.add_dependency(%q<sourcemap>, ["~> 0.1.1"])
  end
end
