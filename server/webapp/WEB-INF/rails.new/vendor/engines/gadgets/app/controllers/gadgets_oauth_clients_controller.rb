class GadgetsOauthClientsController < ApplicationController

  include Gadgets::SslHelper
  include Gadgets::TransactionHelper
  include Gadgets::ConfigValidator
  
  transaction_actions :create, :update, :destroy

  def index
    @gadgets_oauth_clients = GadgetsOauthClient.all.sort { |a, b| a.service_name.casecmp(b.service_name) }
  end

  def new
    @gadgets_oauth_client = GadgetsOauthClient.new
  end

  def edit
    @gadgets_oauth_client = GadgetsOauthClient.find(params[:id])
  end

  def create
    @gadgets_oauth_client = GadgetsOauthClient.new(params[:gadgets_oauth_client])

    return unless add_cert_to_store
    return unless verify_ssl_cert_acceptable

    if @gadgets_oauth_client.save
      flash[:notice] = "Gadget provider was successfully created."
      redirect_to :action => :index
    else
      render_error(:new)
    end
  end

  def update
    @gadgets_oauth_client = GadgetsOauthClient.find(params[:id])
    @gadgets_oauth_client.assign_attributes(params[:gadgets_oauth_client])
    return unless add_cert_to_store
    return unless verify_ssl_cert_acceptable

    if @gadgets_oauth_client.save
      flash[:notice] = "Gadget provider was successfully updated."
      redirect_to :action => :index
    else
      render_error(:edit)
    end
  end

  def destroy
    @gadgets_oauth_client = GadgetsOauthClient.find(params[:id])
    @gadgets_oauth_client.destroy
    flash[:notice] = "Gadget provider was successfully deleted."
    redirect_to(gadgets_oauth_clients_url)
  end

  private

  def render_error(action, message = nil)
    flash.now[:error] = (message || @gadgets_oauth_client.errors.full_messages)
    render :action => action
  end

  def parsed_url(url)
    unless uri = Gadgets::URIParser.parse(url)
      render_error_message("Authorization URL is not valid")
      nil
    end
    uri
  end

  def add_cert_to_store
    if params[:accept_certificate] == "accept_certificate"
      url = params[:gadgets_oauth_client][:oauth_authorize_url]
      begin
        Gadgets.accept_cert_for(url, params[:truststore_checksum])
        return true
      rescue => e
        unless uri = parsed_url(url)
          return false
        end
        render_error_message("Could not store certificate for server at #{uri.host}:#{uri.port}. The error was `#{e.cause.message}'")
        return false
      end
    end
    return true
  end

  def verify_ssl_cert_acceptable
    url = params[:gadgets_oauth_client][:oauth_authorize_url]
    unless uri = parsed_url(url)
      return false
    end
    
    cert_check = Gadgets.cert_check(url)

    return true if cert_check.passed
  
    if @cert = cert_check.cert
      render_error_message "The certificate offered by the server at #{uri.host}:#{uri.port} is not trusted. Please verify the details and accept the certificate to add client."
    else
      render_error_message "Could not connect to server #{uri.host}:#{uri.port}. The error was `#{cert_check.errorMessage}'"
    end
    false
  end

  def render_error_message message
    render_error(:new, message) if params[:action] == 'create'
    render_error(:edit, message) if params[:action] == 'update'
  end
end
