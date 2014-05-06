# coding: utf-8
lib = File.expand_path('../lib', __FILE__)
$LOAD_PATH.unshift(lib) unless $LOAD_PATH.include?(lib)
require '<%=config[:namespaced_path]%>/version'

Gem::Specification.new do |spec|
  spec.name          = <%=config[:name].inspect%>
  spec.version       = <%=config[:constant_name]%>::VERSION
  spec.authors       = [<%=config[:author].inspect%>]
  spec.email         = [<%=config[:email].inspect%>]
<% if config[:ext] -%>
  spec.extensions    = ["ext/<%=config[:underscored_name]%>/extconf.rb"]
<% end -%>
  spec.summary       = %q{TODO: Write a short summary. Required.}
  spec.description   = %q{TODO: Write a longer description. Optional.}
  spec.homepage      = ""
  spec.license       = "MIT"

  spec.files         = `git ls-files -z`.split("\x0")
  spec.executables   = spec.files.grep(%r{^bin/}) { |f| File.basename(f) }
  spec.test_files    = spec.files.grep(%r{^(test|spec|features)/})
  spec.require_paths = ["lib"]

  spec.add_development_dependency "bundler", "~> <%= Bundler::VERSION.split(".")[0..1].join(".") %>"
  spec.add_development_dependency "rake"
<% if config[:ext] -%>
  spec.add_development_dependency "rake-compiler"
<% end -%>
<% if config[:test] -%>
  spec.add_development_dependency "<%=config[:test]%>"
<% end -%>
end
