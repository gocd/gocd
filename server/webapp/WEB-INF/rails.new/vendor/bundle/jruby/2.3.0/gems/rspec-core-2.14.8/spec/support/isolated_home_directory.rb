require 'tmpdir'
require 'fileutils'

shared_context "isolated home directory", :isolated_home => true do
  around do |ex|
    Dir.mktmpdir do |tmp_dir|
      original_home = ENV['HOME']
      begin
        ENV['HOME'] = tmp_dir
        ex.call
      ensure
        ENV['HOME'] = original_home
      end
    end
  end
end
