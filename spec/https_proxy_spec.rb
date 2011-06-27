require 'webrick/https'
require 'net/https'
require 'uri'
require 'thumbslug_common'

include WEBrick

describe 'HTTPS proxying' do
  include ThumbslugMethods
  before(:all) do
    @https_proc = create_secure_httpd
    @tslugs_proc = create_thumbslug(true)
  end

  after(:all) do
    #do any cleanup
    Process.kill('INT', @https_proc)
    Process.kill('INT', @tslugs_proc)
  end


  it 'validate mocked env' do
    filename = Dir.pwd + '/spec/data/random.10k'
    uri = URI.parse('https://127.0.0.1:9443/random.10k')
    https_client = Net::HTTP.new uri.host, uri.port
    https_client.verify_mode = OpenSSL::SSL::VERIFY_NONE
    https_client.use_ssl = true

    response = https_client.request(Net::HTTP::Get.new(uri.path))

    file_digest = Digest::MD5.hexdigest(File.read(filename))
    uri_digest = Digest::MD5.hexdigest(response.body)

    #ensure that the file we got is the same as what's on disk
    uri_digest.should == file_digest
  end

  it 'pull a file from thumbslug' do
    filename = Dir.pwd + '/spec/data/random.10k'
    uri = URI.parse('https://127.0.0.1:8443/random.10k')
    https_client = Net::HTTP.new uri.host, uri.port
    https_client.verify_mode = OpenSSL::SSL::VERIFY_NONE
    https_client.use_ssl = true

    response = https_client.request(Net::HTTP::Get.new(uri.path))

    file_digest = Digest::MD5.hexdigest(File.read(filename))
    uri_digest = Digest::MD5.hexdigest(response.body)

    #ensure that the file we got is the same as what's on disk
    uri_digest.should == file_digest
  end

  it 'pull a 404 from the cdn' do

    uri = URI.parse('https://127.0.0.1:8443/this_will_404')
    https_client = Net::HTTP.new uri.host, uri.port
    https_client.verify_mode = OpenSSL::SSL::VERIFY_NONE
    https_client.use_ssl = true

    response = https_client.request(Net::HTTP::Get.new(uri.path))
    response.code.should == '404'
  end

  it 'pull a 500 from the cdn' do
    uri = URI.parse('https://127.0.0.1:8443/this_will_500')
    https_client = Net::HTTP.new uri.host, uri.port
    https_client.verify_mode = OpenSSL::SSL::VERIFY_NONE
    https_client.use_ssl = true

    response = https_client.request(Net::HTTP::Get.new(uri.path))
    response.code.should == '500'
  end

  it 'check that headers are being passed through' do
    uri = URI.parse('https://127.0.0.1:8443/trace')
    https_client = Net::HTTP.new uri.host, uri.port
    https_client.verify_mode = OpenSSL::SSL::VERIFY_NONE
    https_client.use_ssl = true

    response = https_client.request(Net::HTTP::Get.new(uri.path, {'halifax' => 'sewage'}))
    response.code.should == '200'
    header_idx = response.body =~ /sewage/
    header_idx.should > 0
  end

end
