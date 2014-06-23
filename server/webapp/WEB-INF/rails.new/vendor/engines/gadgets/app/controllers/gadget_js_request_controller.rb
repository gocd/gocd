class GadgetJsRequestController < ApplicationController
  
  def js
    # to avoid return none sense running under mri
    return head :content_type => 'text/javascript' unless defined?(Shindig)
    
    if params[:v] != Shindig::VERSION
      return render :text => 'parameter "v" not a known shindig version', :status => :bad_request
    end
    
    return head :status => :not_modified if request.headers['If-Modified-Since']
    
    feature_registry = Shindig::Guice.instance_of(Shindig::FeatureRegistry)
    
    features = feature_registry.getFeatureResources(Shindig::GadgetContext.new, requested_features, nil)
    
    expires_in 1.year, :public => true
    render :content_type => 'text/javascript', :text => proc { |response, output|
      features.each do |feature|
        output.write( debug? ? feature.debug_content : feature.content)
      end
    }
  end
  
  private
  def debug?
    return false if params[:debug].blank?
    params[:debug] != '0'
  end
  
  def requested_features
    params[:features].split(':')
  end
end
