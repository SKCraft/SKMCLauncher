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

package com.sk89q.mclauncher.config;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.sk89q.mclauncher.Launcher;
import com.sk89q.mclauncher.util.SettingsList;
import com.sk89q.mclauncher.util.SimpleNode;
import com.sk89q.mclauncher.util.Util;
import com.sk89q.mclauncher.util.XMLUtil;

import static com.sk89q.mclauncher.util.XMLUtil.*;

import util.Base64;

/**
 * Stores options for the launcher.
 * 
 * @author sk89q
 */
public class LauncherOptions {
    
    private static final Logger logger = Logger.getLogger(LauncherOptions.class.getCanonicalName());
    
    private File file;
    private String lastConfigName;
    private String lastUsername;
    private File lastInstallDir;
    private ServerHotListManager serverHotList = new ServerHotListManager();
    private ConfigurationsManager configsManager = new ConfigurationsManager();
    private Map<String, String> identities = new HashMap<String, String>();
    private SettingsList defaultSettings = new SettingsList();
    private SettingsList settings = new SettingsList(defaultSettings);
    
    /**
     * Construct the options based off of the given file.
     * 
     * @param file file
     */
    public LauncherOptions(File file) {
        this.file = file;
        
        InputStream in = Launcher.class.getResourceAsStream("/resources/defaults.xml");
        if (in != null) {
            try {
                defaultSettings.read(in);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Could not read default settings", e);
            }
        }
    }
    
    /**
     * Register built-in configurations.
     */
    private void registerBuiltInConfigurations() {
        Constants.register(serverHotList);
        Constants.register(configsManager);
    }
    
    /**
     * Get the configurations manager.
     * 
     * @return configurations manager
     */
    public ConfigurationsManager getConfigurations() {
        return configsManager;
    }
    
    /**
     * Get the server hot list manager.
     * 
     * @return server hot list manager
     */
    public ServerHotListManager getServers() {
        return serverHotList;
    }
    
    /**
     * Get a list of saved usernames.
     * 
     * @return list of usernames
     */
    public Set<String> getSavedUsernames() {
        return identities.keySet();
    }
    
    /**
     * Get a saved password.
     * 
     * @param username username
     * @return password or null if no password is saved
     */
    public String getSavedPassword(String username) {
        return identities.get(username);
    }
    
    /**
     * Remember a given identity.
     * 
     * @param username username
     * @param password password, possibly null to only remember the name
     */
    public void saveIdentity(String username, String password) {
        identities.put(username, password);
    }
    
    /**
     * Forget a user's password but the user him/herself.
     * 
     * @param username username
     */
    public void forgetPassword(String username) {
        identities.put(username, null);
    }
    
    /**
     * Forget a given identity.
     * 
     * @param username username
     */
    public void forgetIdentity(String username) {
        identities.remove(username);
    }

    /**
     * Forget all remembered identities.
     */
    public void forgetAllIdentities() {
        identities.clear();
    }
    
    /**
     * Get the last configuration name.
     * 
     * @return configuration name
     */
    public String getLastConfigName() {
        return lastConfigName;
    }

    /**
     * Set the last configuration name.
     * 
     * @param lastConfigName
     */
    public void setLastConfigName(String lastConfigName) {
        this.lastConfigName = lastConfigName;
    }
    
    /**
     * Get the last configuration.
     * 
     * @return last configuration or null
     */
    public Configuration getLastConfiguration() {
        return configsManager.get(getLastConfigName());
    }
    
    /**
     * Get the last configuration, or default configuration if no last
     * configuration is set (or exists).
     * 
     * @return last configuration or default configuration
     */
    public Configuration getStartupConfiguration() {
        Configuration config = getLastConfiguration();
        if (config == null) {
            return configsManager.getDefault();
        }
        return config;
    }

    /**
     * Get the last used username.
     * 
     * @return username or null
     */
    public String getLastUsername() {
        return lastUsername;
    }
    
    /**
     * Set the last used username.
     * 
     * @param lastUsername username
     */
    public void setLastUsername(String lastUsername) {
        this.lastUsername = lastUsername;
    }
    
    /**
     * Gets the directory of where addons were last installed from.
     * 
     * @return directory, or null if one isn't set
     */
    public File getLastInstallDir() {
        return lastInstallDir;
    }

    /**
     * Set the last directory that addons were installed from.
     * 
     * @param dir directory
     */
    public void setLastInstallDir(File dir) {
        this.lastInstallDir = dir;
    }

    /**
     * Get the settings list.
     * 
     * @return settings
     */
    public SettingsList getSettings() {
        return settings;
    }

    /**
     * Read the configuration.
     * 
     * @throws IOException on I/O error
     */
    public void read() throws IOException {
        identities = new HashMap<String, String>();
        configsManager = new ConfigurationsManager();
        serverHotList = new ServerHotListManager();
        
        InputStream in = null;
        
        try {
            Cipher cipher = Launcher.getInstance().getCipher(Cipher.DECRYPT_MODE, "passwordfile");
            
            in = new BufferedInputStream(new FileInputStream(file));
            
            Document doc = parseXml(in);
            XPath xpath = XPathFactory.newInstance().newXPath();

            lastUsername = getStringOrNull(doc, xpath.compile("/launcher/username"));
            lastConfigName = getStringOrNull(doc,
                    xpath.compile("/launcher/lastConfiguration"));
            lastInstallDir = Util.getClosestDirectory(getStringOrNull(doc,
                    xpath.compile("/launcher/lastInstallDirectory")));

            XPathExpression nameExpr = xpath.compile("name/text()");
            XPathExpression keyExpr = xpath.compile("key/text()");
            
            // Read all the <identity> elements
            for (Node node : getNodes(doc, xpath.compile("/launcher/identities/identity"))) {
                String username = getString(node, nameExpr);
                String key = getString(node, keyExpr);
                String password = null;
                
                if (key.length() > 0) {
                    try {
                        byte[] decrypted = cipher.doFinal(Base64.decode(key));
                        password = new String(decrypted, "UTF-8");
                    } catch (IllegalBlockSizeException e) {
                        e.printStackTrace();
                    } catch (BadPaddingException e) {
                        e.printStackTrace();
                    }
                }
                
                identities.put(username, password);
            }
            
            XPathExpression idExpr = xpath.compile("id/text()");
            XPathExpression appDirExpr = xpath.compile("appDir/text()");
            XPathExpression basePathExpr = xpath.compile("basePath/text()");
            XPathExpression updateURLExpr = xpath.compile("updateURL/text()");
            XPathExpression lastJarExpr = xpath.compile("lastJar/text()");
            XPathExpression settingsExpr = xpath.compile("settings");
            
            // Read all the <configuration> elements
            for (Node node : getNodes(doc, xpath.compile("/launcher/configurations/configuration"))) {
                String id = getString(node, idExpr);
                String name = getString(node, nameExpr);
                String appDir = getStringOrNull(node, appDirExpr);
                String basePath = getStringOrNull(node, basePathExpr);
                String urlString = getStringOrNull(node, updateURLExpr);
                String lastJar = getStringOrNull(node, lastJarExpr);
                
                try {
                    URL updateUrl = urlString != null ? new URL(urlString) : null;
                    Configuration config;
                    if (basePath != null) {
                        config = new Configuration(id, name, new File(basePath), updateUrl);
                    } else {
                        config = new Configuration(id, name, appDir, updateUrl);
                    }
                    
                    Node settingsNode = XMLUtil.getNode(node, settingsExpr);
                    SettingsList settings = new SettingsList();
                    if (settingsNode != null) {
                        settings.read(settingsNode);
                    }
                    config.setSettings(settings);
                    
                    config.setLastActiveJar(lastJar);
                    configsManager.register(config);
                } catch (MalformedURLException e) {
                    logger.log(Level.WARNING, "Could not read configuration '" + id + "', bad URL '" + urlString + "'", e);
                } catch (IllegalArgumentException e) {
                    logger.log(Level.WARNING, "Could not read configuration '" + id + "'", e);
                }
            }
            
            XPathExpression addressExpr = xpath.compile("address");
            
            // Read all the <server> elements
            for (Node node : getNodes(doc, xpath.compile("/launcher/servers/server"))) {
                String name = getString(node, nameExpr);
                String address = getString(node, addressExpr);
                serverHotList.register(name, address, false);
            }

            for (Node node : getNodes(doc, xpath.compile("/launcher/settings"))) {
                settings.read(node);
            }
        } catch (FileNotFoundException e) {
        } catch (InvalidKeyException e) {
            throw new IOException(e);
        } catch (InvalidKeySpecException e) {
            throw new IOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        } catch (NoSuchPaddingException e) {
            throw new IOException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new IOException(e);
        } catch (XPathExpressionException e) {
            throw new IOException(e);
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        } catch (SAXException e) {
            throw new IOException(e);
        } finally {
            registerBuiltInConfigurations();
            Util.close(in);
        }
    }
    
    /**
     * Write to disk.
     * 
     * @throws IOException on I/O error
     */
    public void write() throws IOException {
        try {
            Cipher cipher = Launcher.getInstance().getCipher(Cipher.ENCRYPT_MODE, "passwordfile");
            
            Document doc = newXml();
            SimpleNode root = start(doc, "launcher");
            
            root.addNode("username").addValue(lastUsername);
            root.addNode("lastConfiguration").addValue(lastConfigName);
            root.addNode("lastInstallDirectory").addValue(
                    lastInstallDir != null ? lastInstallDir
                            .getAbsolutePath() : null);

            SimpleNode identitiesNode = root.addNode("identities");
            for (Map.Entry<String, String> entry : identities.entrySet()) {
                SimpleNode identityNode = identitiesNode.addNode("identity");
                identityNode.addNode("name").addValue(entry.getKey());
                
                // Save encrypted password
                if (entry.getValue() != null) {
                    byte[] encrypted = cipher.doFinal(entry.getValue().getBytes());
                    identityNode.addNode("key").addValue(Base64.encodeToString(encrypted, false));
                }
            }
            
            SimpleNode configurationsNode = root.addNode("configurations");
            for (Configuration config : configsManager) {
                SimpleNode configurationNode = configurationsNode.addNode("configuration");
                File f = config.getCustomBasePath();
                configurationNode.addNode("id").addValue(config.getId());
                configurationNode.addNode("name").addValue(config.getName());
                configurationNode.addNode("appDir").addValue(config.getAppDir());
                configurationNode.addNode("basePath").addValue(f != null ? f.getPath() : null);
                configurationNode.addNode("updateURL").addValue(config.getUpdateUrl() != null ?
                        config.getUpdateUrl().toString() : null);
                configurationNode.addNode("lastJar").addValue(config.getLastActiveJar());
                config.getSettings().write(configurationNode.addNode("settings").getNode());
            }
            
            SimpleNode serversNode = root.addNode("servers");
            for (String name : serverHotList.getServerNames()) {
                if (!serverHotList.isBuiltIn(name)) {
                    SimpleNode serverNode = serversNode.addNode("server");
                    serverNode.addNode("name").addValue(name);
                    serverNode.addNode("address").addValue(serverHotList.get(name));
                }
            }
            
            SimpleNode settingsNode = root.addNode("settings");
            settings.write(settingsNode.getNode());
            
            writeXml(doc, file);
        } catch (InvalidKeyException e) {
            throw new IOException(e);
        } catch (InvalidKeySpecException e) {
            throw new IOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        } catch (NoSuchPaddingException e) {
            throw new IOException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new IOException(e);
        } catch (TransformerException e) {
            throw new IOException(e);
        } catch (IllegalBlockSizeException e) {
            throw new IOException(e);
        } catch (BadPaddingException e) {
            throw new IOException(e);
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
    }
    
    /**
     * Load the configuration.
     * 
     * @return true if successful
     */
    public boolean load() {
        try {
            read();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Save the configuration.
     * 
     * @return true if successful
     */
    public boolean save() {
        try {
            write();
            return true;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to load options", e);
            return false;
        }
    }
    
}
