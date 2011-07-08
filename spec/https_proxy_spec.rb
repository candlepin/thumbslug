require 'net/https'
require 'uri'
require 'thumbslug_common'
require 'timeout'

describe 'HTTPS proxying' do
  include ThumbslugMethods

  around(:each) do |test|
    Timeout::timeout(6) {
      test.run
    }
  end

  before(:all) do
    @https_proc = create_httpd(true)
    @tslugs_pipe = create_thumbslug({:ssl_keystore => 'spec/data/keystore-spec.p12',
                                     :ssl_keystore_password => 'pass'})
    @tslugs_badcdn_pipe = create_thumbslug({:port => '9998',
                                           :cdn_port => '9999'})
  end

  after(:all) do
    #do any cleanup
    Process.kill('INT', @https_proc)
    Process.kill('INT', @tslugs_pipe.pid)
    Process.kill('INT', @tslugs_badcdn_pipe.pid)
    puts "Waiting for forked procs to terminate.."
    Process.waitall()
    print "done"
  end

  it 'validate mocked env' do
    filename = Dir.pwd + '/spec/data/random.10k'
    response = get('https://127.0.0.1:9090/random.10k')

    file_digest = Digest::MD5.hexdigest(File.read(filename))
    uri_digest = Digest::MD5.hexdigest(response.body)

    #ensure that the file we got is the same as what's on disk
    uri_digest.should == file_digest
  end

  it 'pull a file from thumbslug' do
    filename = Dir.pwd + '/spec/data/random.10k'
    response = get('https://127.0.0.1:8088/random.10k')

    file_digest = Digest::MD5.hexdigest(File.read(filename))
    uri_digest = Digest::MD5.hexdigest(response.body)

    #ensure that the file we got is the same as what's on disk
    uri_digest.should == file_digest
  end


  it 'pull a file from thumbslug without a client cert' do
    filename = Dir.pwd + '/spec/data/random.10k'
    uri = URI.parse('https://127.0.0.1:8088/random.10k')
    client = Net::HTTP.new uri.host, uri.port
    client.use_ssl = true
   
    lambda do
      client.request(Net::HTTP::Get.new(uri.path))
    end.should raise_exception(OpenSSL::SSL::SSLError)
  end


  it 'pull a 502 from thumbslug (no open port on CDN)' do
    response = get('https://127.0.0.1:9998/this_will_404')
    response.code.should == '502'
  end

  it 'pull a 404 from the cdn' do
    response = get('https://127.0.0.1:8088/this_will_404')
    response.code.should == '404'
  end

  it 'pull a 500 from the cdn' do
    response = get('https://127.0.0.1:8088/this_will_500')
    response.code.should == '500'
  end

  it 'check that client headers are being passed through' do
    response = get('https://127.0.0.1:8088/trace', {'captain' => 'sub'})
    response.code.should == '200'
    header_idx = response.body =~ /sub/
    header_idx.should > 0
  end

end
