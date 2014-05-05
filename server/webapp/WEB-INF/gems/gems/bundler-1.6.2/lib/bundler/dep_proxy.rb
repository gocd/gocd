module Bundler
  class DepProxy

    attr_reader :required_by, :__platform, :dep

    def initialize(dep, platform)
      @dep, @__platform, @required_by = dep, platform, []
    end

    def hash
      @hash ||= dep.hash
    end

    def ==(o)
      dep == o.dep && __platform == o.__platform
    end

    alias eql? ==

    def type
      @dep.type
    end

    def name
      @dep.name
    end

    def requirement
      @dep.requirement
    end

    def to_s
      "#{name} (#{requirement}) #{__platform}"
    end

  private

    def method_missing(*args)
      @dep.send(*args)
    end

  end
end
