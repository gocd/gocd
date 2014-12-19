require 'tmpdir'

include FileUtils

# Prepares temporary fixture-directories and
# cleans them afterwards.
#
# @param [Fixnum] number_of_directories the number of fixture-directories to make
#
# @yield [path1, path2, ...] the empty fixture-directories
# @yieldparam [String] path the path to a fixture directory
#
def fixtures(number_of_directories = 1)
  current_pwd = pwd
  paths = 1.upto(number_of_directories).map do
    File.expand_path(File.join(pwd, "spec/.fixtures/#{Time.now.to_f.to_s.sub('.', '') + rand(9999).to_s}"))
  end

  # Create the dirs
  paths.each { |p| mkdir_p(p) }

  cd(paths.first) if number_of_directories == 1

  yield(*paths)

ensure
  cd current_pwd
  paths.map { |p| rm_rf(p) if File.exists?(p) }
end
