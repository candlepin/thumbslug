%global _binary_filedigest_algorithm 1
%global _source_filedigest_algorithm 1
%global _binary_payload w9.gzdio
%global _source_payload w9.gzdio

%global selinux_variants mls strict targeted
%global selinux_policyver %(%{__sed} -e 's,.*selinux-policy-\\([^/]*\\)/.*,\\1,' /usr/share/selinux/devel/policyhelp || echo 0.0.0)
%global modulename thumbslug


Name: thumbslug
Summary: Thumbslug CDN proxy
Group: Internet/Applications
License: GPLv2
Version: 0.0.29
Release: 1%{?dist}
URL: http://fedorahosted.org/thumbslug
Source: %{name}-%{version}.tar.gz
BuildRoot: %{_tmppath}/%{name}-%{version}-%{release}-buildroot
Vendor: Red Hat, Inc.
BuildArch: noarch

Requires(pre): shadow-utils
%if 0%{?rhel} && 0%{?rhel} <= 6
Requires: jakarta-commons-codec
%else
Requires: apache-commons-codec
%endif
Requires: jna >= 3.2.4
Requires: log4j >= 1.2
Requires: netty >= 3.2.3
Requires: akuma >= 1.7
%if 0%{?fedora} && 0%{?fedora} > 17
Requires: java-oauth
%else
Requires: oauth
%endif
Requires: java >= 1.6.0

BuildRequires: ant >= 1.7.0
BuildRequires: akuma >= 1.7
BuildRequires: jna >= 3.2.4
BuildRequires: log4j >= 1.2
BuildRequires: netty >= 3.2.3
%if 0%{?rhel} && 0%{?rhel} <= 6
BuildRequires: jakarta-commons-codec
%else
BuildRequires: apache-commons-codec
%endif
%if 0%{?fedora} && 0%{?fedora} > 17
BuildRequires: java-oauth
%else
BuildRequires: oauth
%endif
BuildRequires: java-devel >= 1.6.0

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

%if "%{selinux_policyver}" != ""
Requires:       selinux-policy >= %{selinux_policyver}
%endif
Requires:       %{name} = %{version}-%{release}
Requires(post):   /usr/sbin/semodule
Requires(post):   /usr/sbin/semanage
Requires(post):   /sbin/restorecon
Requires(postun): /usr/sbin/semodule
Requires(postun): /usr/sbin/semanage
Requires(postun): /sbin/restorecon


%description selinux
SELinux policy module supporting thumbslug


%prep
%setup -q 

%build
ant -Dlibdir=/usr/share/java clean package

cd selinux
for selinuxvariant in %{selinux_variants}
do
  make NAME=${selinuxvariant} -f /usr/share/selinux/devel/Makefile
  mv %{modulename}.pp %{modulename}.pp.${selinuxvariant}
  make NAME=${selinuxvariant} -f /usr/share/selinux/devel/Makefile clean
done
cd -


%install
install -d -m 755 $RPM_BUILD_ROOT/%{_datadir}/%{name}/
install -m 644 target/%{name}.jar $RPM_BUILD_ROOT/%{_datadir}/%{name}

install -d -m 755 $RPM_BUILD_ROOT/%{_bindir}/
install -m 755 %{name}.bin $RPM_BUILD_ROOT/%{_bindir}/%{name}

install -d -m 755 $RPM_BUILD_ROOT/%{_initddir}
install -m 755 thumbslug.init $RPM_BUILD_ROOT/%{_initddir}/%{name}

install -d -m 755 $RPM_BUILD_ROOT/%{_sysconfdir}/thumbslug
install -m 640 thumbslug.conf \
            $RPM_BUILD_ROOT/%{_sysconfdir}/thumbslug/thumbslug.conf
install -m 640 cdn-ca.pem \
            $RPM_BUILD_ROOT/%{_sysconfdir}/thumbslug/cdn-ca.pem

install -d -m 775 $RPM_BUILD_ROOT/%{_var}/log/thumbslug
install -d -m 775 $RPM_BUILD_ROOT/%{_var}/run/thumbslug

cd selinux
for selinuxvariant in %{selinux_variants}
do
  install -d $RPM_BUILD_ROOT/%{_datadir}/selinux/${selinuxvariant}
  install -p -m 644 %{modulename}.pp.${selinuxvariant} \
    $RPM_BUILD_ROOT/%{_datadir}/selinux/${selinuxvariant}/%{modulename}.pp
done
cd -
/usr/sbin/hardlink -cv $RPM_BUILD_ROOT/%{_datadir}/selinux

%clean
rm -rf $RPM_BUILD_ROOT


%pre
getent group thumbslug >/dev/null || groupadd -r thumbslug
getent passwd thumbslug >/dev/null || \
    useradd -r -g thumbslug -d %{_datadir}/%{name} -s /sbin/nologin \
    -c "thumbslug content and entitlement proxy" thumbslug 
exit 0


%post
/sbin/chkconfig --add %{name}


%postun
if [ "$1" -ge "1" ] ; then
    /sbin/service %{name} condrestart >/dev/null 2>&1 || :
fi


%preun
if [ $1 -eq 0 ] ; then
    /sbin/service %{name} stop >/dev/null 2>&1
    /sbin/chkconfig --del %{name}
fi


%post selinux
for selinuxvariant in %{selinux_variants}
do
  /usr/sbin/semodule -s ${selinuxvariant} -i \
    %{_datadir}/selinux/${selinuxvariant}/%{modulename}.pp &> /dev/null || :
done
/sbin/restorecon %{_localstatedir}/cache/thumbslug || :
/usr/sbin/semanage port -a -t thumbslug_port_t -p tcp 8088 || :

%postun selinux
if [ $1 -eq 0 ] ; then
  for selinuxvariant in %{selinux_variants}
  do
     /usr/sbin/semodule -s ${selinuxvariant} -r %{modulename} &> /dev/null || :
  done
  [ -d %{_localstatedir}/cache/thumbslug ]  && \
    /sbin/restorecon -R %{_localstatedir}/cache/thumbslug &> /dev/null || :
  /usr/sbin/semanage port -a -t thumbslug_port_t -p tcp 8088 || :
fi


%files
%defattr(-, root, thumbslug)
%doc README
%{_initddir}/%{name}
%{_bindir}/%{name}

%dir %{_sysconfdir}/thumbslug
%config(noreplace) %{_sysconfdir}/%{name}/%{name}.conf
%{_sysconfdir}/%{name}/cdn-ca.pem

%dir %{_datadir}/%{name}
%{_datadir}/%{name}/thumbslug.jar

%dir %{_var}/log/thumbslug
%dir %{_var}/run/thumbslug
%ghost %attr(660, thumbslug, thumbslug) %{_var}/run/thumbslug/thumbslug.pid
%ghost %attr(660, thumbslug, thumbslug) %{_var}/lock/subsys/thumbslug

%files selinux
%defattr(-,root,root,0755)
%doc selinux/*
%{_datadir}/selinux/*/%{modulename}.pp


%changelog
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
