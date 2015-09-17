require 'fileutils'

# Adds a save_fixture method takes the rendered content
# and stores it in a fixture file to be used with jasmine.
#
# example usage:
#   save_fixture('name_of_fixture_file.html')
#
# jasmine-jquery can be configured to load the generated fixtures by:
#   jasmine.getFixtures().fixturesPath = '/path/to/generated/fixture/directory';
#
# related resources:
# https://gist.github.com/725723
# http://pivotallabs.com/users/jb/blog/articles/1152-javascripttests-bind-reality-
module JasmineRails
  module SaveFixture
    FIXTURE_DIRECTORY = 'spec/javascripts/fixtures/generated'

    # Saves the rendered as a fixture file.
    def save_fixture(file_name, content = rendered)
      fixture_path = File.join(Rails.root, FIXTURE_DIRECTORY, file_name)
      fixture_directory = File.dirname(fixture_path)
      FileUtils.mkdir_p fixture_directory unless File.exists?(fixture_directory)

      File.open(fixture_path, 'w') do |file|
        file.puts(content)
      end

      ignore_generated_fixtures
    end

    private

    # create .gitignore to exclude generated fixtures from repository
    def ignore_generated_fixtures
      ignore_file = File.join(Rails.root, FIXTURE_DIRECTORY, '../.gitignore')
      return if File.exists?(ignore_file)
      File.open(ignore_file, 'w') do |file|
        file.puts('generated')
      end
    end
  end
end
