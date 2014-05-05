# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "ffi"
  s.version = "1.9.3"
  s.platform = "java"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Wayne Meissner"]
  s.date = "2013-10-30"
  s.description = "Ruby FFI library"
  s.email = "wmeissner@gmail.com"
  s.homepage = "http://wiki.github.com/ffi/ffi"
  s.licenses = ["BSD"]
  s.require_paths = ["lib"]
  s.rubygems_version = "1.8.24"
  s.summary = "Ruby FFI"

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
    else
    end
  else
  end
end
