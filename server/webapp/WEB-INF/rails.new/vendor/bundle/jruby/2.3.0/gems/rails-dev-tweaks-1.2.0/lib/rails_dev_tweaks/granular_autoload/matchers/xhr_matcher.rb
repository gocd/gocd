class RailsDevTweaks::GranularAutoload::Matchers::XhrMatcher

  def call(request)
    request.xhr?
  end

end
