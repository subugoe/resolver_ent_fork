/*
 * Created on 14.06.2005
 *
 */
package de.unigoettingen.sub.commons.resolver;

/**
 * Class represents a single LPI-block in the response of the underlying LPIR
 * (local resolvers).
 *
 * @author enders
 */
public class ResolvedURL {

    String purl = null;
    String url = null;
    String service = null;
    String servicehome = null;
    String access = null;
    String version = null;

    public ResolvedURL() {
    }

    public String getPurl() {
        return purl;
    }

    public String getUrl() {
        return url;
    }

    public String getService() {
        return service;
    }

    public String getServicehome() {
        return servicehome;
    }

    public String getAccess() {
        return access;
    }

    public String getVersion() {
        return version;
    }

    public void setPurl(String in) {
        purl = in;
    }

    public void setUrl(String in) {
        url = in;
    }

    public void setService(String in) {
        service = in;
    }

    public void setServicehome(String in) {
        servicehome = in;
    }

    public void setAccess(String in) {
        access = in;
    }

    public void setVersion(String in) {
        version = in;
    }
}
