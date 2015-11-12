class CatchJsonParseErrors
  def initialize (app)
    @app = app
  end

  def call (env)
    begin
      @app.call(env)
    rescue ActionDispatch::ParamsParser::ParseError => exception
      content_type_is_json?(env) ? build_response(exception) : raise(exception)
    end
  end

  private

  def content_type_is_json? (env)
    env['CONTENT_TYPE'] =~ /application\/json/
  end

  def error_message (exception)
    "Payload data is not valid JSON. Error message: #{exception}"
  end

  def build_response (exception)
    [400, {"Content-Type" => "application/json"}, [{error: error_message(exception)}.to_json]]
  end

end