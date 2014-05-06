require File.expand_path("../../path.rb", __FILE__)
include Spec::Path

files = [ 'specs.4.8.gz',
          'prerelease_specs.4.8.gz',
          'quick/Marshal.4.8/rcov-1.0-mswin32.gemspec.rz',
          'gems/rcov-1.0-mswin32.gem' ]

# Set up pretend http gem server with FakeWeb
$LOAD_PATH.unshift "#{Dir[base_system_gems.join("gems/fakeweb*/lib")].first}"
require 'fakeweb'

FakeWeb.allow_net_connect = false

files.each do |file|
  FakeWeb.register_uri(:get, "http://localgemserver.test/#{file}",
    :body => File.read("#{gem_repo1}/#{file}"))
end
FakeWeb.register_uri(:get, "http://localgemserver.test/gems/rcov-1.0-x86-mswin32.gem",
  :status => ["404", "Not Found"])

FakeWeb.register_uri(:get, "http://localgemserver.test/api/v1/dependencies",
  :status => ["404", "Not Found"])
