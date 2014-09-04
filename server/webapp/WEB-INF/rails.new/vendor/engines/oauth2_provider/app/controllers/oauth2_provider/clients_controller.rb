# Copyright (c) 2010 ThoughtWorks Inc. (http://thoughtworks.com)
# Licenced under the MIT License (http://www.opensource.org/licenses/mit-license.php)

module Oauth2Provider
  class ClientsController < ApplicationController

    include Oauth2Provider::SslHelper
    include Oauth2Provider::TransactionHelper
    
    transaction_actions :create, :update, :destroy
    before_filter :set_layout_and_tab_name
    
    def index
      @oauth_clients = Client.all.sort{|a, b| a.name.casecmp(b.name)}
      respond_to do |format|
        format.html
        format.xml  { render :xml => @oauth_clients.to_xml(:root => 'oauth_clients', :dasherize => false) }
      end
    end

    def show
      @oauth_client = Client.find(params[:id])
      respond_to do |format|
        format.html
        format.xml  { render :xml => @oauth_client.to_xml(:dasherize => false) }
      end
    end

    def new
      @oauth_client = Client.new
    end

    def edit
      @oauth_client = Client.find(params[:id])
    end

    def create
      @oauth_client = Client.new(params[:client])

      respond_to do |format|
        if @oauth_client.save
          flash[:notice] = 'OAuth client was successfully created.'
          format.html { redirect_to oauth_engine.clients_path }
          format.xml  { render :xml => @oauth_client, :status => :created, :location => @oauth_client }
        else
          flash.now[:error] = @oauth_client.errors.full_messages
          format.html { render :action => "new" }
          format.xml  { render :xml => @oauth_client.errors, :status => :unprocessable_entity }
        end
      end
    end

    def update
      @oauth_client = Client.find(params[:id])

      if @oauth_client.update_attributes(params[:client])
        flash[:notice] = 'OAuth client was successfully updated.'
        respond_to do |format|
          format.html { redirect_to oauth_engine.clients_path }
          format.xml  { head :ok }
        end
      else
        flash.now[:error] = @oauth_client.errors.full_messages
        respond_to do |format|
          format.html { render :action => "edit" }
          format.xml  { render :xml => @oauth_client.errors, :status => :unprocessable_entity }
        end
      end
    end

    def destroy
      @oauth_client = Client.find(params[:id])
      @oauth_client.destroy

      respond_to do |format|
        flash[:notice] = 'OAuth client was successfully deleted.'
        format.html { redirect_to oauth_engine.clients_path }
        format.xml  { head :ok }
      end
    end
    
    private
    
    def set_layout_and_tab_name
      @tab_name = 'oauth-clients'
      self.class.layout 'admin'
    end
  end
end
