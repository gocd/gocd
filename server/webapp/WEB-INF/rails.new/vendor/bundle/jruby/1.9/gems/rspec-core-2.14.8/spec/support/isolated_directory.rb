require 'tmpdir'
require 'fileutils'

shared_context "isolated directory", :isolated_directory => true do
  around do |ex|
    Dir.mktmpdir do |tmp_dir|
      Dir.chdir(tmp_dir, &ex)
    end
  end
end
