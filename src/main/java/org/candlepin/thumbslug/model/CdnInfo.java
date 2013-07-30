/**
 * Copyright (c) 2011 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.thumbslug.model;


/**
 * CdnInfo represents the CDN information returned from Candlepin which
 * includes the upstream subscription certificate and the CDN url and
 * certificate.
 */
public class CdnInfo {
    private Cdn cdn;
    private String subCert;

    public CdnInfo() {
        cdn = null;
        subCert = null;
    }

    public Cdn getCdn() {
        return cdn;
    }

    public String getCdnUrl() {
        // FIXME should actually make the ctor construct
        // a Cdn
	if (cdn == null) {
            return null;
        }
        return cdn.getUrl();
    }

    public String getCdnCert() {
	if (cdn == null) {
            return null;
        }
        return cdn.getCert();
    }

    public String getSubCert() {
        return subCert;
    }
}
