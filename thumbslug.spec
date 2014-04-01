%global _binary_filedigest_algorithm 1
%global _source_filedigest_algorithm 1
%global _binary_payload w9.gzdio
%global _source_payload w9.gzdio

%global selinux_variants mls strict targeted
%global modulename thumbslug
%global use_systemd (0%{?fedora} && 0%{?fedora} >= 17) || (0%{?rhel} && 0%{?rhel} >= 7)

%if %use_systemd
    %global selinux_policy_dir systemd
%else
    %global selinux_policy_dir sysvinit
%endif

Name: thumbslug
Summary: Thumbslug CDN proxy
Group: Internet/Applications
License: GPLv2
Version: 0.0.38
Release: 1%{?dist}
URL: http://fedorahosted.org/thumbslug
Source: %{name}-%{version}.tar.gz
BuildRoot: %{_tmppath}/%{name}-%{version}-%{release}-buildroot
Vendor: Red Hat, Inc.
BuildArch: noarch

Requires(pre): shadow-utils

%if 0%{?rhel} && 0%{?rhel} < 6
Requires: jakarta-commons-codec
%else
Requires: apache-commons-codec
%endif

%if 0%{?fedora}
Requires: java-oauth
%else
Requires: oauth >= 20100601-4
%endif

Requires: jna >= 3.2.4
Requires: log4j >= 1.2
Requires: netty >= 3.2.3
Requires: akuma >= 1.7
Requires: java >= 1.6.0
Requires: jpackage-utils

%if %use_systemd
Requires(post): systemd
Requires(preun): systemd
Requires(postun): systemd
%else
Requires(post): chkconfig
Requires(preun): chkconfig
Requires(preun): initscripts
Requires(postun): initscripts
%endif

# BuildRequires section
BuildRequires: ant >= 1.7.0
BuildRequires: akuma >= 1.7
BuildRequires: jna >= 3.2.4
BuildRequires: log4j >= 1.2
BuildRequires: netty >= 3.2.3

%if 0%{?rhel} && 0%{?rhel} < 6
BuildRequires: jakarta-commons-codec
%else
BuildRequires: apache-commons-codec
%endif

%if 0%{?fedora}
BuildRequires: java-oauth
%else
BuildRequires: oauth >= 20100601-4
%endif

BuildRequires: java-devel >= 1.6.0
BuildRequires: jpackage-utils
%if %use_systemd
# We need the systemd RPM macros
BuildRequires: systemd
%endif

%define __jar_repack %{nil}

%description
Thumbslug is a content and entitlement proxy for on premesis Candlepin installs.


%package selinux
Summary:        SELinux policy module supporting thumbslug
Group:          System Environment/Base
BuildRequires:  checkpolicy
BuildRequires:  selinux-policy-devel
BuildRequires:  /usr/share/selinux/devel/policyhelp
BuildRequires:  hardlink

%{!?_selinux_policy_version: %global _selinux_policy_version %(sed -e 's,.*selinux-policy-\\([^/]*\\)/.*,\\1,' /usr/share/selinux/devel/policyhelp 2>/dev/null)}
%if "%{_selinux_policy_version}" != ""
Requires:      selinux-policy >= %{_selinux_policy_version}
%endif
Requires:       %{name} = %{version}-%{release}
Requires(post):   /usr/sbin/semodule
Requires(post):   /usr/sbin/semanage
Requires(post):   /sbin/restorecon
Requires(postun): /usr/sbin/semodule
Requires(postun): /usr/sbin/semanage
Requires(postun): /sbin/restorecon

%description selinux
%{summary}.


%prep
%setup -q 
%{__mkdir} -p lib
build-jar-repository -s -p lib oauth/oauth-consumer oauth/oauth akuma commons-codec jna log4j netty

%build
ant -Dlibdir=lib clean package

pushd selinux/%{selinux_policy_dir}
for selinuxvariant in %{selinux_variants}
do
  make NAME=${selinuxvariant} -f /usr/share/selinux/devel/Makefile
  mv %{modulename}.pp %{modulename}.pp.${selinuxvariant}
  make NAME=${selinuxvariant} -f /usr/share/selinux/devel/Makefile clean
done
popd

%install
install -d -m 755 %{buildroot}/%{_javadir}
install -m 644 target/%{name}.jar %{buildroot}/%{_javadir}/%{name}.jar

install -d -m 755 %{buildroot}/%{_bindir}/

%if %use_systemd
    install -d -m 755 %{buildroot}/%{_unitdir}
    install -m 644 conf/%{name}.service %{buildroot}/%{_unitdir}/%{name}.service
    install -d -m 755 %{buildroot}/%{_tmpfilesdir}
    install -m 644 conf/%{name}.conf.tmpfiles %{buildroot}/%{_tmpfilesdir}/%{name}.conf
%else
    install -d -m 755 %{buildroot}/%{_initddir}
    install -m 755 conf/thumbslug.init %{buildroot}/%{_initddir}/%{name}
%endif

install -d -m 755 %{buildroot}/%{_sysconfdir}/thumbslug
install -m 640 conf/thumbslug.conf \
            %{buildroot}/%{_sysconfdir}/thumbslug/thumbslug.conf
install -m 640 cdn-ca.pem \
            %{buildroot}/%{_sysconfdir}/thumbslug/cdn-ca.pem

install -d -m 775 %{buildroot}/%{_var}/log/%{name}
install -d -m 775 %{buildroot}/%{_var}/run/%{name}
install -d -m 775 %{buildroot}/%{_var}/lock/subsys

/bin/touch %{buildroot}/%{_var}/run/%{name}/%{name}.pid
/bin/touch %{buildroot}/%{_var}/lock/subsys/%{name}

%jpackage_script org.candlepin.thumbslug.Main "" "" %{name}:oauth/oauth:oauth/oauth-consumer:akuma:commons-codec:jna:log4j:netty %{name} true

pushd selinux/%{selinux_policy_dir}
for selinuxvariant in %{selinux_variants}
do
  install -d %{buildroot}/%{_datadir}/selinux/${selinuxvariant}
  install -p -m 644 %{modulename}.pp.${selinuxvariant} \
    %{buildroot}/%{_datadir}/selinux/${selinuxvariant}/%{modulename}.pp
done
popd
/usr/sbin/hardlink -cv %{buildroot}/%{_datadir}/selinux

%clean
rm -rf %{buildroot}

%pre
getent group thumbslug >/dev/null || groupadd -r thumbslug
getent passwd thumbslug >/dev/null || \
    useradd -r -g thumbslug -d %{_datadir}/%{name} -s /sbin/nologin \
    -c "thumbslug content and entitlement proxy" thumbslug 
exit 0

%post
%if %use_systemd
    %systemd_post %{name}.service
%else
    /sbin/chkconfig --add %{name}
%endif

%postun
%if %use_systemd
    %systemd_postun_with_restart %{name}.service
%else
    if [ "$1" -ge "1" ] ; then
        /sbin/service %{name} condrestart >/dev/null 2>&1 || :
    fi
%endif

%preun
%if %use_systemd
    %systemd_preun %{name}.service
%else
    if [ $1 -eq 0 ] ; then
        /sbin/service %{name} stop >/dev/null 2>&1
        /sbin/chkconfig --del %{name}
    fi
%endif

%post selinux
for selinuxvariant in %{selinux_variants}
do
  /usr/sbin/semodule -s ${selinuxvariant} -i \
    %{_datadir}/selinux/${selinuxvariant}/%{modulename}.pp &> /dev/null || :
done
/usr/sbin/semanage port -a -t thumbslug_port_t -p tcp 8088 &> /dev/null || :

%postun selinux
if [ $1 -eq 0 ] ; then
  for selinuxvariant in %{selinux_variants}
  do
     /usr/sbin/semodule -s ${selinuxvariant} -r %{modulename} &> /dev/null || :
  done
  /usr/sbin/semanage port -d -t thumbslug_port_t -p tcp 8088 &> /dev/null || :
fi

%files
%defattr(-, root, thumbslug)
%doc README

%if %use_systemd
    %attr(644,root,root) %{_unitdir}/%{name}.service
    %attr(644,root,root) %{_tmpfilesdir}/%{name}.conf
%else
    %attr(755,root,root) %{_initrddir}/%{name}
%endif

%attr(-, root, root) %{_bindir}/%{name}

%dir %{_sysconfdir}/thumbslug
%config(noreplace) %{_sysconfdir}/%{name}/%{name}.conf
%{_sysconfdir}/%{name}/cdn-ca.pem

%{_javadir}/%{name}.jar

%dir %{_var}/log/%{name}
%dir %{_var}/run/%{name}
%ghost %attr(644, thumbslug, thumbslug) %{_var}/run/%{name}/%{name}.pid
%ghost %attr(644, thumbslug, thumbslug) %{_var}/lock/subsys/%{name}

%files selinux
%defattr(-,root,root,0755)
%doc selinux/*
%{_datadir}/selinux/*/%{modulename}.pp

%changelog
* Thu Feb 27 2014 William Poteat <wpoteat@redhat.com> 0.0.38-1
- Correct requires and buildrequires for oauth and use macros correctly.
  (awood@redhat.com)

* Wed Feb 26 2014 William Poteat <wpoteat@redhat.com> 0.0.37-1
- 918759: Update Thumbslug to use systemd. (awood@redhat.com)

* Fri Sep 13 2013 William Poteat <wpoteat@redhat.com> 0.0.36-1
- Update condition to <= 6 for proper build package (wpoteat@redhat.com)

* Fri Sep 13 2013 William Poteat <wpoteat@redhat.com> 0.0.35-1
- 910077: Removed offending Shutting down log statement. (jesusr@redhat.com)

* Tue Sep 03 2013 jesus m. rodriguez <jesusr@redhat.com> 0.0.34-1
- 924349: semanage changes (jesusr@redhat.com)
- 924349: remove restorecon /var/cache/thumbslug (jesusr@redhat.com)
- 996681: be less specific with thumbslug (bkearney@redhat.com)
- remove f16 and f17 from katello-koji releasers (jesusr@redhat.com)
- remove cvs section from tito.props (jesusr@redhat.com)

* Wed Aug 21 2013 jesus m. rodriguez <jesusr@redhat.com> 0.0.33-1
- 996681: Change default group owner (bkearney@redhat.com)

* Mon Jun 03 2013 Bryan Kearney <bkearney@redhat.com> 0.0.32-1
- Update spec file to only use jakarta-commons-codec on RHEL 5
  (bkearney@redhat.com)

* Thu Mar 28 2013 Alex Wood <awood@redhat.com> 0.0.31-1
- Fix PKCS8 private key parsing from recent fix. (dgoodwin@redhat.com)
- Checkstyle fixes. (dgoodwin@redhat.com)
- 916895: Fix use of V3 certificates to the CDN. (dgoodwin@redhat.com)
- Add note on how to generate server keystore. (dgoodwin@redhat.com)

* Tue Mar 12 2013 jesus m. rodriguez <jesusr@redhat.com> 0.0.30-1
- RHEL 5 and 6 require jakarta-commons-codec. (awood@redhat.com)

* Wed Feb 20 2013 jesus m. rodriguez <jesusr@redhat.com> 0.0.29-1
- F18 has java-oauth (jesusr@redhat.com)
- f17 file should use f17 and *not* f15 (jesusr@redhat.com)
- add f18 build configs (jesusr@redhat.com)
- add releaser for katello-koji to build into katello-thirdparty-candlepin* tag (msuchy@redhat.com)

* Wed Dec 19 2012 jesus m. rodriguez <jesusr@redhat.com> 0.0.28-1
- require apache-commons-codec instead of jakarta-commons-codec (jesusr@redhat.com)
- 880662: Add missing semanage dep for selinux pkg (jbowes@redhat.com)

* Thu Nov 15 2012 James Bowes <jbowes@redhat.com> 0.0.27-1
- 875876: Fix oauth classpath in init script (jbowes@redhat.com)
- 871586: Add a ping function to Thumbslug. (awood@redhat.com)
- 817599: Fix closing connections when in a bad state (jbowes@redhat.com)
- 865841: Improve concurrency properties of thumbslug (jbowes@redhat.com)
- 829791: Add support for configuring logging in thumbslug.conf
  (alikins@redhat.com)
* Fri Nov 02 2012 William Poteat <wpoteat@redhat.com> 0.0.26-1
- Add findbugs target (alikins@redhat.com)
- 868290: Verify signature on client and CDN X.509 certificates
  (jbowes@redhat.com)
- Target java 1.6 (jbowes@redhat.com)
- Go back to a megajar for use with the buildfile (jbowes@redhat.com)
- Allow Thumbslug to operate with V3 entitlement certs (wpoteat@redhat.com)
- adding internal koji repo (jesusr@redhat.com)
- Add back 5Server and 6Server (jbowes@redhat.com)
- update releasers to f17 (jbowes@redhat.com)
- include require for emma so emma target works (alikins@redhat.com)

* Wed Jul 11 2012 Chris Duryee (beav) <cduryee@redhat.com>
- 837386: do not package jars with thumbslug (cduryee@redhat.com)
- fixups for ruby 1.9 and java 1.7 (cduryee@redhat.com)
- adding test config with fake values to test Config (jmrodri@gmail.com)

* Tue May 08 2012 Chris Duryee (beav) <cduryee@redhat.com>
- 819662: missing default for cdn.proxy value (cduryee@redhat.com)

* Wed Apr 25 2012 Chris Duryee (beav) <cduryee@redhat.com>
- Initial proxy support for thumbslug (cduryee@redhat.com)
- Exclude manifest.mf files for sub-jars from being included in thumbslug.jar
  (cduryee@redhat.com)
- print shutdown message during error conditions (cduryee@redhat.com)

* Mon Apr 09 2012 Chris Duryee (beav) <cduryee@redhat.com>
- just build x86_64 in fedora for now (cduryee@redhat.com)
- jar inclusion and log fix (cduryee@redhat.com)
- do not package the jar if deps are missing (cduryee@redhat.com)
- 790802: thumbslug deps not built in brew (cduryee@redhat.com)
- Rework try/catch block on config file reading (cduryee@redhat.com)
- Modify the mock target to use the mirror lists (bkearney@redhat.com)

* Wed Dec 21 2011 Bryan Kearney <bkearney@redhat.com> 0.0.21-1
- Add initial selinux policy (jbowes@redhat.com)
- 759598: teach thumbslug init how to show status and stop (jbowes@redhat.com)
- bunch of minor whitespace cleanup (jmrodri@gmail.com)
- remove all trailing whitespace, configure checkstyle to verify.
  (jmrodri@gmail.com)
- add emma tasks to buildfile for code coverage (jmrodri@gmail.com)

* Tue Dec 06 2011 James Bowes <jbowes@redhat.com> 0.0.20-1
- Fix accept type and order of operations for candlepin communication
  (jbowes@redhat.com)

* Mon Dec 05 2011 James Bowes <jbowes@redhat.com> 0.0.19-1
- 759607: update url for subscriptions handler (jbowes@redhat.com)
- update readme to reference changing rhsm ca certs (jbowes@redhat.com)
- Check entitlement against candlepin to ensure it is not revoked
  (cduryee@redhat.com)

* Mon Nov 28 2011 James Bowes <jbowes@redhat.com> 0.0.18-1
- Add chkconfig initscript lines (jbowes@redhat.com)

* Wed Nov 23 2011 James Bowes <jbowes@redhat.com> 0.0.17-1
- add rpm scriptlets for init script (jbowes@redhat.com)
- add a note about client configuration in the readme (jbowes@redhat.com)
- update to latest akuma (jbowes@redhat.com)
- change lock file (jbowes@redhat.com)
- print ssl error to log file (jbowes@redhat.com)
- fix ant packaging (jbowes@redhat.com)
- add restart and status init targets (jbowes@redhat.com)
- Add config file for etc (jbowes@redhat.com)
- Set up thumbslug user (jbowes@redhat.com)
- update spec to install init file (jbowes@redhat.com)
- Allow overriding config values from thumbslug.conf (jbowes@redhat.com)
- fill in spec description (jbowes@redhat.com)
- update config documentation (jbowes@redhat.com)

* Wed Nov 16 2011 jesus m. rodriguez <jesusr@redhat.com> 0.0.16-1
- ibiblio moved. (jesusr@redhat.com)

* Wed Nov 16 2011 jesus m. rodriguez <jesusr@redhat.com> 0.0.15-1
- Support downloading entitlement certificates from candlepin (jbowes@redhat.com)
- Switch to PEM format files for thumbslug to CDN connection (jbowes@redhat.com)

* Wed Oct 26 2011 jesus m. rodriguez <jesusr@redhat.com> 0.0.14-1
- respin thumbslug (jesusr@redhat.com)

* Tue Oct 25 2011 jesus m. rodriguez <jesusr@redhat.com> 0.0.13-1
- WIP ts -> candlepin code for chris to pick up (jbowes@redhat.com)
- remove TODO about getSink (jbowes@redhat.com)
- add testing servlets for spec tests (cduryee@redhat.com)
- use correct dist-cvs branch (jesusr@redhat.com)
- Don't bother encoding/decoding the request/response bodies (jbowes@redhat.com)

* Tue Oct 11 2011 jesus m. rodriguez <jesusr@redhat.com> 0.0.12-1
- adding proper branch (jesusr@redhat.com)

* Tue Oct 11 2011 jesus m. rodriguez <jesusr@redhat.com> 0.0.11-1
- allow dist-cvs building (jesusr@redhat.com)

* Mon Oct 10 2011 jesus m. rodriguez <jesusr@redhat.com> 0.0.10-1
- update gitignore (jbowes@redhat.com)
- checkstyle fixup (jbowes@redhat.com)

* Wed Jul 13 2011 Chris Duryee (beav) <cduryee@redhat.com>
- ant jar stuff (cduryee@redhat.com)
- Merge commit 'ed410d78475813f9bd8e28530408c60280c7a000' (cduryee@redhat.com)
- first cut at a thumbslug init file (cduryee@redhat.com)

* Tue Jul 12 2011 Chris Duryee (beav) <cduryee@redhat.com>
- pull the jar into the rpm (cduryee@redhat.com)

* Tue Jul 12 2011 Chris Duryee (beav) <cduryee@redhat.com>
- make the jar file have a consistent name (cduryee@redhat.com)

* Tue Jul 12 2011 Chris Duryee (beav) <cduryee@redhat.com>
- dont generate schema (we dont have any) (cduryee@redhat.com)

* Tue Jul 12 2011 Chris Duryee (beav) <cduryee@redhat.com>
- include ant (cduryee@redhat.com)

* Mon Jul 11 2011 Chris Duryee (beav) <cduryee@redhat.com>
- try to make an rpm (cduryee@redhat.com)

* Mon Jul 11 2011 Chris Duryee (beav) <cduryee@redhat.com>
- bump the version 

* Mon Jul 11 2011 Chris Duryee (beav) <cduryee@redhat.com>
- new package built with tito

* Mon Jul 11 2011 Chris Duryee <cduryee@redhat.com> 0.0.1-1
- first cut (cduryee@redhat.com)
