class RailsDevTweaks::GranularAutoload::Matchers::ForcedMatcher

  def call(request)
    request.headers['Force-Autoload'] || request.params.has_key?(:force_autoload)
  end

end
