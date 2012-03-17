/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010, 2011 Albert Pham <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.sk89q.mclauncher.update;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import static com.sk89q.mclauncher.util.XMLUtil.*;

/**
 * Manages a custom update check.
 * 
 * @author sk89q
 */
public class UpdateCheck {

    private URL updateUrl;
    private URL packageUrl;
    private String latestVersion;
    
    /**
     * Construct the check.
     * 
     * @param updateUrl URL to check for updates
     */
    public UpdateCheck(URL updateUrl) {
        this.updateUrl = updateUrl;
    }
    
    /**
     * Attempt to check the update server.
     * 
     * @throws IOException on I/O error
     */
    public void checkUpdateServer() throws IOException {
        HttpURLConnection conn = null;
                
        try {
            conn = (HttpURLConnection) updateUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(false);
            conn.setReadTimeout(5000);

            conn.connect();
            
            if (conn.getResponseCode() != 200) {
                throw new IOException("Did not get expected 200 code");
            }

            Document doc = parseXml(new BufferedInputStream(conn.getInputStream()));
            XPath xpath = XPathFactory.newInstance().newXPath();

            latestVersion = getString(doc, xpath.compile("/update/latest"));
            packageUrl = new URL(getString(doc, xpath.compile("/update/packageurl")));
        } catch (XPathExpressionException e) {
            throw new IOException(e);
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        } catch (SAXException e) {
            throw new IOException(e);
        } finally {
            if (conn != null) conn.disconnect();
            conn = null;
        }
    }

    /**
     * Get the update check URL.
     * 
     * @return url
     */
    public URL getUpdateUrl() {
        return updateUrl;
    }

    /**
     * Get the package definition URL.
     * 
     * @return url
     */
    public URL getPackageDefUrl() {
        return packageUrl;
    }

    /**
     * Get the latest version.
     * 
     * @return version
     */
    public String getLatestVersion() {
        return latestVersion;
    }
    
}
