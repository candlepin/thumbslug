require 'net/https'
require 'uri'
require 'thumbslug_common'
require 'timeout'

describe 'CA Certificate checking' do
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
    @tslugs_bad_ca_pipe = create_thumbslug({:port => '9997',
                                            :cdn_ssl_ca_keystore => 'spec/data/unknown-ca-pub.pem'})
  end

  after(:all) do
    #do any cleanup
    Process.kill('INT', @https_proc)
    Process.kill('INT', @tslugs_pipe.pid)
    Process.kill('INT', @tslugs_bad_ca_pipe.pid)
    puts "Waiting for forked procs to terminate.."
    Process.waitall()
    print "done"
  end

  it "should return when the cdn's ssl certificate can't be validated" do
    response = get('https://127.0.0.1:9997/this_will_404')
    response.code.should == '502'
  end

  it "should error out when the client's CA is unknown" do
    lambda do
      response = get('https://127.0.0.1:8088/this_will_404', nil,
                     'spec/data/unknown-ca.pem')
    end.should raise_exception(OpenSSL::SSL::SSLError)
  end
end
