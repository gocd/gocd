module Uber
  class Version < Hash
    def initialize(version)
      @version = Gem::Version.new(version)
      major, minor, patch = @version.segments

      self[:major] = major || 0
      self[:minor] = minor || 0
      self[:patch] = patch || 0
    end

    def >=(version)
      major, minor, patch = parse(version)

      self[:major] > major or
        (self[:major] == major and self[:minor] >= minor and self[:patch] >= patch)
    end

    def ~(*versions)
      !! versions.find do |v|
        major, minor, patch = parse(v)

        self[:major] == major and self[:minor] == minor
      end
    end

  private
    def parse(version)
      major, minor, patch = Gem::Version.new(version).segments
      [major||0, minor||0, patch||0]
    end
  end
end