class RailsDevTweaks::GranularAutoload::Matchers::PathMatcher

  def initialize(regex)
    @regex = regex
  end

  def call(request)
    @regex =~ request.fullpath
  end

end
