module TestIOStreams
  def stderr
    ::TestIOStreams.stderr
  end

  def stdout
    ::TestIOStreams.stdout
  end

  class << self
    attr_accessor :stderr, :stdout

    def set_streams(stderr, stdout)
      self.stderr, self.stdout = stderr, stdout
    end
  end
end
