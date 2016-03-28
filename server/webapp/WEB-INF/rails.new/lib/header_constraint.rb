class HeaderConstraint
  include Services

  def matches? (request)
    return true unless system_environment.isApiSafeModeEnabled

    return false if request.headers['HTTP_CONFIRM'].blank?

    'true'.casecmp(request.headers['HTTP_CONFIRM']).zero?
  end
end