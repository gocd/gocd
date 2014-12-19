class GadgetRenderingController < ApplicationController

  def ifr
    # to avoid return none sense running under mri
    return head :status => :ok          unless defined?(Shindig)

    return head :status => :forbidden   if request.headers["X-shindig-dos"]

    gadget_context = Shindig::HttpGadgetContext.new(request)
    renderer = Shindig::Guice.instance_of(Shindig::Renderer)
    result = renderer.render(gadget_context)

    if result.status.name == "OK"
      gadget_context.getIgnoreCache ? expires_now : expires_in(cache_refresh)
      render :text => result.content
    else
      render :text => friendly_error_message(result, gadget_context)
    end
  end


  private

  def friendly_error_message(result, gadget_context)
    message = "<p>There was an error rendering the gadget: "
    
    message +=  if result.getErrorMessage =~ /Missing or malformed url parameter/
                  "Missing or malformed gadget 'url' parameter"
                else
                  "Unable to retrive gadget specification from #{escape_html gadget_context.getUrl.to_s}, or the gadget spec retrived is not valid."
                end
                
    message += "</p>"
    
    message += "<p>#{escape_html result.getErrorMessage}</p>" if gadget_context.getDebug
    message
  end

  def cache_refresh
    if params[:refresh] && params[:refresh] =~ /^([0-9])+$/
      params[:refresh].to_i
    else
      24.hours
    end
  end
  
  def escape_html(str)
    org.apache.commons.lang.StringEscapeUtils.escapeHtml(str)
  end

  def expires_now
    super
    response["Expires"] = Time.at(0).rfc2822
  end

  def expires_in(seconds, options={})
    super
    response["Expires"] = (Gadgets::Clock.now + seconds).rfc2822
  end


end
