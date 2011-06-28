require 'openssl'

module ThumbslugMethods

  def get(url_str, headers = nil)
    uri = URI.parse(url_str)
    client = Net::HTTP.new uri.host, uri.port
    if uri.scheme == 'https':
      client.use_ssl = true
      client.verify_mode = OpenSSL::SSL::VERIFY_NONE #TODO: verify server
      client.ca_file = "CA/cacert.pem"
      client.key = OpenSSL::PKey::RSA.new(File.read("spec/data/spec/spec-client_keypair.pem"))
      client.cert = OpenSSL::X509::Certificate.new(File.read("spec/data/spec/cert_spec-client.pem"))
    end
    return client.request(Net::HTTP::Get.new(uri.path, headers))
  end

  def create_httpd(secure=false)
    privkey = OpenSSL::PKey::RSA.new(File.read("spec/data/webrick/webrick-server_keypair.pem"), '5678')
    server_cert = OpenSSL::X509::Certificate.new(File.read("spec/data/webrick/cert_webrick-server.pem"))
    ca_cert = "spec/data/CA/cacert.pem" 
    if secure
      config = {
        :Port => 9443,
        :BindAddress => '127.0.0.1',
        :DocumentRoot => Dir.pwd + '/spec/data/',
        :Debugger => true,
        :Verbose => true,
        :SSLEnable => true,
        :SSLVerifyClient => OpenSSL::SSL::VERIFY_PEER | OpenSSL::SSL::VERIFY_FAIL_IF_NO_PEER_CERT,
        :SSLPrivateKey => privkey,
        :SSLCertificate => server_cert,
        :SSLCACertificateFile => ca_cert,
        #comment out these two lines to enable webrick logging
        :Logger => WEBrick::Log.new('/dev/null'),
        :AccessLog => [nil, nil],
      }
    else
      config = {
        :Port => 9090,
        :BindAddress => '127.0.0.1',
        :DocumentRoot => Dir.pwd + '/spec/data/',
        #comment out these two lines to enable webrick logging
        :Logger => WEBrick::Log.new("/dev/null"),
        :AccessLog => [nil, nil],
      }
    end
    
    pid = fork {
      $stderr = File.open('/dev/null', 'w')
      server = HTTPServer.new(config)
      trap('INT') { server.stop }
      server.mount "/this_will_500", FiveHundred 
      server.mount "/trace", Trace
      server.start
    }
    #TODO: this should read the forked pid and wait for the "I am ready" message
    sleep 3
    return pid
  end

  def create_thumbslug(params={})
    #these need to all be strings
    config = {
     :port => '8088',
     :ssl => 'true',
     :ssl_keystore => 'spec/data/keystore.p12',
     :ssl_keystore_password => 'password',
     :cdn_port => '9443',
     :cdn_host => 'localhost',
     :cdn_ssl => 'true',
     :sendTSHeader => 'false'
    }

    params.each_pair do |key, value|
      config[key] = value
    end

    tslug_exec_string = "java " + 
                 " -Dport=" + config[:port] +
                 " -Dssl=" + config[:ssl] +
                 " -Dssl.keystore=" + config[:ssl_keystore] +
                 " -Dssl.keystore.password=" + config[:ssl_keystore_password] +
                 " -Dcdn.port=" + config[:cdn_port] +
                 " -Dcdn.host=" + config[:cdn_host] +
                 " -Dcdn.ssl=" + config[:cdn_ssl] +
                 " -DsendTSheader=" + config[:sendTSHeader] +
                 " -jar " +  Dir.pwd + "/target/thumbslug-1.0.0.jar"
    pp tslug_exec_string 
    pid = fork {
      exec(tslug_exec_string)
    }
    #TODO: this should read the forked pid and wait for the "I am ready" message
    sleep 1
    Process.detach(pid)
    return pid
  end
end


class FiveHundred < WEBrick::HTTPServlet::AbstractServlet

  def do_GET(request, response)
    response.status = 500 
    response['Content-Type'] = "text/plain"
    response.body = 'Error! a 500 error'
  end

end

class Trace < WEBrick::HTTPServlet::AbstractServlet
  #ersatz trace that implements GET, not TRACE verb

  def do_GET(request, response)
    response.status = 200
    response['Content-Type'] = "text/plain"
    headers = ""
    request.raw_header.each {|r| headers += r }
    response.body = headers
  end

end
