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

package com.sk89q.mclauncher.model;

import java.awt.Window;
import java.io.IOException;
import java.net.URL;

import javax.swing.SwingUtilities;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlTransient;

import com.sk89q.mclauncher.WebpageDialog;
import com.sk89q.mclauncher.WebpagePanel;
import com.sk89q.mclauncher.update.Phase;
import com.sk89q.mclauncher.update.UpdateCache;
import com.sk89q.mclauncher.util.LauncherUtils;
import com.sk89q.skmcl.swing.SwingHelper;

public class Message {
    
    public static enum ContentType {
        @XmlEnumValue("url")
        URL,
        @XmlEnumValue("html")
        HTML
    }
    
    private String id;
    private String title;
    private Phase phase;
    private boolean agreement;
    private String version;
    private ContentType type;
    private String content;

    @XmlAttribute
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    @XmlElement
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    
    @XmlTransient
    public String getValidatedTitle() {
        if (title == null || title.trim().isEmpty()) {
            return agreement ? "Agreement" : "Information";
        }
        return title;
    }

    /**
     * Get the internal ID used for the update cache.
     * 
     * @return the ID
     */
    @XmlTransient
    public String getCacheId() {
        return "/$/com.sk89q.mclauncher.model.Message/" + getId();
    }

    @XmlElement
    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    @XmlElement
    public boolean isAgreement() {
        return agreement;
    }

    public void setAgreement(boolean agreement) {
        this.agreement = agreement;
    }

    @XmlAttribute
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @XmlElement
    public ContentType getType() {
        return type;
    }

    public void setType(ContentType type) {
        if (type == null) {
            type = ContentType.URL;
        }
        this.type = type;
    }

    @XmlElement
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
    
    /**
     * Check if this messages needs to be shown, and also mark this message into the
     * update cache so that it is versioned.
     * 
     * @param cache
     * @return
     */
    public boolean mark(UpdateCache cache) {
        if (getId() == null || getVersion() == null) {
            return true;
        }
        
        String id = getCacheId();
        String oldVersion = cache.getFileVersion(id);
        if (oldVersion != null && oldVersion.equals(getVersion())) {
            cache.touch(id);
            return false;
        }
        cache.setFileVersion(id, getVersion());
        return true;
    }
    
    public WebpagePanel createPanel(URL baseUrl) throws IOException {
        switch (getType()) {
        case URL:
            URL url = LauncherUtils.concat(baseUrl, getContent());
            return WebpagePanel.forURL(url, false);
        case HTML:
            return WebpagePanel.forHTML(getContent());
        }
        throw new IOException("Invalid content type");
    }
    
    public WebpageDialog createDialog(Window owner, URL baseUrl) throws IOException {
        return new WebpageDialog(
                owner, getValidatedTitle(), createPanel(baseUrl), isAgreement());
    }
    
    public boolean showDialog(Window owner, URL baseUrl) throws IOException {
        final WebpageDialog dialog = createDialog(owner, baseUrl);
        if (SwingUtilities.isEventDispatchThread()) {
            dialog.setVisible(true);
        } else {
            SwingHelper.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    dialog.setVisible(true);
                }
            });
        }
        return !isAgreement() || dialog.hasAgreed();
    }

}
