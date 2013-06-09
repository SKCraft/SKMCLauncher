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

package com.sk89q.lpbuilder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import lzma.sdk.lzma.Encoder;
import lzma.streams.LzmaOutputStream;

import com.sk89q.mclauncher.config.Configuration;
import com.sk89q.mclauncher.model.FileGroup;
import com.sk89q.mclauncher.model.PackageFile;
import com.sk89q.mclauncher.model.PackageManifest;
import com.sk89q.mclauncher.model.SingleFile;
import com.sk89q.mclauncher.model.UpdateManifest;
import com.sk89q.mclauncher.util.BasicArgsParser;
import com.sk89q.mclauncher.util.BasicArgsParser.ArgsContext;
import com.sk89q.mclauncher.util.SimpleLogFormatter;
import com.sk89q.mclauncher.util.Util;

/**
 * Builds an update package for SKMCLauncher.
 */
public class UpdateBuilder implements Runnable {

    private static final Logger logger = Logger.getLogger(UpdateBuilder.class
            .getCanonicalName());

    private final File updateDir;
    private final File outputDir;
    private final Map<String, ZipBucket> buckets = new HashMap<String, ZipBucket>();
    
    private UpdateManifest updateManifest;
    private PackageManifest packageManifest;
    private UpdateBuilderConfig config = new UpdateBuilderConfig();
    private String updateFilename = "update.xml";
    private String packageFilename = "package.xml";

    /**
     * Create a new buidler with the given source directory and output directory.
     * 
     * @param updateDir the directory with the source files
     * @param outputDir the output directory
     */
    public UpdateBuilder(File updateDir, File outputDir) {
        this.updateDir = updateDir;
        this.outputDir = outputDir;
        
        outputDir.mkdirs();
        
        setUpdateManifest(new UpdateManifest());
        setPackageManifest(new PackageManifest());
    }
    
    public String getUpdateFilename() {
        return updateFilename;
    }

    public void setUpdateFilename(String updateFilename) {
        this.updateFilename = updateFilename;
    }

    public String getPackageFilename() {
        return packageFilename;
    }

    public void setPackageFilename(String packageFilename) {
        this.packageFilename = packageFilename;
    }

    public UpdateBuilderConfig getConfiguration() {
        return config;
    }

    public void setConfiguration(UpdateBuilderConfig config) {
        this.config = config;
    }

    public UpdateManifest getUpdateManifest() {
        return updateManifest;
    }

    public void setUpdateManifest(UpdateManifest updateManifest) {
        this.updateManifest = updateManifest;
    }

    public PackageManifest getPackageManifest() {
        return packageManifest;
    }

    public void setPackageManifest(PackageManifest packageManifest) {
        this.packageManifest = packageManifest;
        packageManifest.setVersion("1.2");
    }

    /**
     * Load a configuration file for this update builder.
     * 
     * @param file path to file
     * @throws FileNotFoundException if the file cannot be found
     * @throws JAXBException parsing error
     */
    public void loadConfiguration(File file) throws FileNotFoundException, JAXBException {
        JAXBContext context = JAXBContext.newInstance(UpdateBuilderConfig.class);
        Unmarshaller um = context.createUnmarshaller();
        FileReader reader = null;
        UpdateBuilderConfig config;
        try {
            reader = new FileReader(file);
            config = (UpdateBuilderConfig) um.unmarshal(new FileReader(file));;
        } finally {
            Util.close(reader);
        }
        
        UpdateBuilderConfig.Templates templates = config.getTemplates();
        
        setPackageManifest(templates.getPackageManifest());
        setUpdateManifest(templates.getUpdateManifest());
        
        this.config = config;
    }

    /**
     * Collect all the files needed for this update.
     * 
     * <p>Call this once.</p>
     * 
     * @throws IOException on I/O error
     * @throws InterruptedException on interruption
     */
    private void collectFiles() throws IOException, InterruptedException {
        collectFiles(updateDir);
    }

    /**
     * Collect all the files needed for this update in the given folder.
     * 
     * @param dir the directory
     * @throws IOException on I/O error
     * @throws InterruptedException on interruption
     */
    private void collectFiles(File dir) throws IOException, InterruptedException {
        String relative = getRelative(updateDir, dir);
        
        logger.info("Collecting files in '" + dir.getAbsolutePath() + "'");
        
        FileGroup group = new FileGroup();
        group.setDest(relative);
        group.setSource(relative);
        
        for (File f : dir.listFiles()) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            
            if (f.isDirectory()) {
                collectFiles(f);
            } else {
                String fileRelative = getRelative(updateDir, f);
                logger.info("-> " + fileRelative);
                
                SingleFile singleFile = createSingleFile(f);
                if (singleFile != null) {
                    singleFile.setSize(f.length());
                    singleFile.setVersion(getVersionString(f));
                    singleFile.setFilename(f.getName());
                    copyFile(f, new File(outputDir, fileRelative));
                    group.getFiles().add(singleFile);
                }
            }
        }
        
        if (group.getFiles().size() > 0) {
            packageManifest.getFileGroups().add(group);
        }
    }
    
    /**
     * Create the {@link SingleFile} for a given file, otherwise return null if the
     * file needs to go into an archive.
     * 
     * @param path the path
     * @return a package file, or null if it's not a {@link SingleFile}
     */
    private SingleFile createSingleFile(File file) {
        String path = getRelative(updateDir, file);
        
        SingleFile singleFile = new SingleFile();
        String archiveName = null;

        // First, match all patterns and apply properties
        List<FilePattern> filePatterns = config.getFilePatterns();
        if (filePatterns != null) {
            for (FilePattern pattern : filePatterns) {
                if (pattern.matchesPath(path)) {
                    singleFile.inheritGenericProperties(pattern);
                    
                    // We want to .zip this file up!
                    if (pattern.getArchiveName() != null) {
                        archiveName = pattern.getArchiveName();
                    }
                }
            }
        }
        
        // We are .zipping this file up, so we do things differently
        if (archiveName != null) {
            storeFileInArchive(archiveName, file, singleFile);
            return null;
        } else {
            return singleFile;
        }
    }
    
    /**
     * Store a file into an archive.
     * 
     * @param archiveName the archive name
     * @param file the file
     * @param packageFile file to inherit archive properties from
     */
    private void storeFileInArchive(String archiveName, File file, PackageFile packageFile) {
        archiveName = archiveName.trim().toLowerCase();
        ZipBucket bucket = buckets.get(archiveName);
        if (bucket == null) {
            bucket = new ZipBucket();
            buckets.put(archiveName, bucket);
        }
        bucket.inheritGenericProperties(packageFile);
        bucket.queue(file);
    }

    /**
     * Commit all the buckets.
     * 
     * <p>Call this once.</p>
     * 
     * @throws IOException on I/O error
     * @throws InterruptedException on interruption
     */
    private void commitBuckets() throws IOException, InterruptedException {
        logger.info("Writing ZIPs that are to be extracted later...");
        
        FileGroup group = new FileGroup();
        group.setDest(".");
        group.setSource("");
        
        for (Map.Entry<String, ZipBucket> entry : buckets.entrySet()) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            
            String filename = entry.getKey();
            File target = new File(outputDir, filename);
            logger.info("-> " + filename);
            
            ZipBucket bucket = entry.getValue();
            bucket.writeContents(updateDir, target); // This also sets a version
            bucket.setSize(target.length());;
            bucket.setFilename(filename);

            // Match patterns and apply properties
            List<FilePattern> filePatterns = config.getFilePatterns();
            if (filePatterns != null) {
                for (FilePattern pattern : filePatterns) {
                    if (pattern.matchesPath(filename)) {
                        logger.info("    Pattern: " + filename + ": " + pattern);
                        bucket.inheritGenericProperties(pattern);
                    }
                }
            }
            
            group.getFiles().add(bucket);
        }
        
        if (group.getFiles().size() > 0) {
            packageManifest.getFileGroups().add(group);
        }
    }

    /**
     * Generate the version digest for a file.
     * 
     * <p>Currently, this generates an MD5 digest.</p>
     * 
     * @param file the file
     * @return a version digest
     * @throws IOException on I/O exception
     */
    public static byte[] getVersionDigest(File file) throws IOException {
        InputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            MessageDigest complete = MessageDigest.getInstance("MD5");
            int numRead;
            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    complete.update(buffer, 0, numRead);
                }
            } while (numRead != -1);
            return complete.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        } finally {
            Util.close(fis);
        }
    }

    /**
     * Generate the version string for a file.
     * 
     * <p>Currently, this generates an MD5 hash.</p>
     * 
     * @param file the file
     * @return a version string
     * @throws IOException on I/O exception
     */
    public static String getVersionString(File file) throws IOException {
        return Util.getHexString(getVersionDigest(file));
    }
    
    /**
     * Build the package.
     * 
     * @throws JAXBException on XML error
     * @throws IOException on I/O error
     * @throws InterruptedException on interruption
     */
    public void build() throws JAXBException, IOException, InterruptedException {
        logger.info("Output directory: " + outputDir.getAbsolutePath());
        
        collectFiles();
        commitBuckets();
        
        getUpdateManifest().setPackageURL(getPackageFilename());
        
        JAXBContext context = JAXBContext.newInstance(
                PackageManifest.class, UpdateManifest.class);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        
        File packageFile = new File(outputDir, getPackageFilename());
        File updateFile = new File(outputDir, getUpdateFilename());
        marshal(m, getPackageManifest(), packageFile);
        marshal(m, getUpdateManifest(), updateFile);

        logger.info("Package manifest: " + packageFile.getAbsolutePath());
        logger.info("Update manifest: " + updateFile.getAbsolutePath());

        logger.info("------------------------------------------------------------------------");
        logger.info("Update package created!");

        logger.info("(1) Upload the entirety of '" + outputDir.getAbsolutePath() + 
                "' to somewhere on the Internet.");
        logger.info("(2) Install in the launcher with:");
        logger.info("    http://YOUR_DOMAIN.com/WHERE_YOU_UPLOADED_IT/" + updateFilename);

        logger.info("------------------------------------------------------------------------");
    }

    @Override
    public void run() {
        try {
            build(this);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Build cancelled!", e);
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "An error has occurred: " + e.getMessage(), e);
        }
    }
    
    private void marshal(Marshaller m, Object object, File file) 
            throws JAXBException, IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            BufferedOutputStream buf = new BufferedOutputStream(fos);
            m.marshal(object, buf);
            buf.close();
        } finally {
            Util.close(fos);
        }
    }

    @SuppressWarnings("unused")
    private static void copyFileLzma(File sourceFile, File destFile) throws IOException {
        destFile.getParentFile().mkdirs();

        FileInputStream fis = null;
        BufferedInputStream bis = null;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        LzmaOutputStream compress = null;
        try {
            fis = new FileInputStream(sourceFile);
            bis = new BufferedInputStream(fis);
            fos = new FileOutputStream(destFile);
            bos = new BufferedOutputStream(fos);
            compress = new LzmaOutputStream(bos, new Encoder());

            byte[] buffer = new byte[1024 * 8];
            int length;

            while ((length = bis.read(buffer)) > 0) {
                compress.write(buffer, 0, length);
            }
        } finally {
            Util.close(bis);
            Util.close(fis);
            Util.close(compress);
            Util.close(bos);
            Util.close(fos);
        }
    }

    private static void copyFile(File sourceFile, File destFile) throws IOException {
        destFile.getParentFile().mkdirs();
        
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            Util.close(source);
            Util.close(destination);
        }
    }

    private static String getRelative(File base, File path) {
        return base.toURI().relativize(path.toURI()).getPath();
    }
    
    private static void checkArgSet(Object obj, String message) {
        if (obj == null) {
            logger.warning(message);
            System.exit(1);
        }
    }
    
    public static void clean(File dir) throws InterruptedException {
        logger.info("");
        logger.info("---------------------------------------------------");
        logger.info("Cleaning target directory");
        logger.info("---------------------------------------------------");
        
        Util.cleanDir(dir);

        logger.info("Deleted the contents of " + dir.getAbsolutePath());
    }

    public static void build(UpdateBuilder builder) 
            throws JAXBException, IOException, InterruptedException {
        logger.info("");
        logger.info("---------------------------------------------------");
        logger.info("Building update package");
        logger.info("---------------------------------------------------");
        
        builder.build();
    }
    
    public static void main(String[] args) throws Throwable {
        SimpleLogFormatter.setAsFormatter();

        logger.info("Easy Update Builder for SKMCLauncher");
        logger.info("http://github.com/sk89q/skmclauncher");

        BasicArgsParser parser = new BasicArgsParser();
        parser.addValueArg("dir");
        parser.addValueArg("out");
        parser.addValueArg("id");
        parser.addValueArg("name");
        parser.addValueArg("version");
        parser.addValueArg("package-filename");
        parser.addValueArg("update-filename");
        parser.addValueArg("config");
        parser.addFlagArg("clean");
        
        ArgsContext context;
        try {
            context = parser.parse(args);
        } catch (IllegalArgumentException e) {
            logger.warning("Error: " + e.getMessage());
            System.exit(1);
            return;
        }
        
        String filesDirStr = context.get("dir");
        checkArgSet(filesDirStr, "Use -dir to set a directory containing the files");
        
        String outputDirStr = context.get("out");
        checkArgSet(outputDirStr, "Use -output to set an output directory");

        String id = context.get("id");
        String name = context.get("name");
        String version = context.get("version");
        String packageFilename = context.get("package-filename");
        String updateFilename = context.get("update-filename");
        String configPath = context.get("config");
        
        File updateDir = new File(filesDirStr);
        File outputDir = new File(outputDirStr);
        
        if (updateDir.equals(outputDir)) {
            logger.warning("Cannot use the same source dir as the output dir");
            System.exit(2);
        }
        
        if (context.has("clean")) {
            clean(outputDir);
        }
        
        UpdateBuilder builder = new UpdateBuilder(updateDir, outputDir);
        
        if (configPath != null) {
            builder.loadConfiguration(new File(configPath));
        } else {
        }
        
        if (packageFilename != null) {
            builder.setPackageFilename(packageFilename);
        }
        
        if (updateFilename != null) {
            builder.setUpdateFilename(updateFilename);
        }

        UpdateManifest updateManifest = builder.getUpdateManifest();
        
        if (id != null) {
            updateManifest.setId(id);
        }
        
        if (name != null) {
            updateManifest.setName(name);
        }
        
        if (updateManifest.getId() == null) {
            logger.warning("Update ID not set! Use -config and specify a configuration. " +
                    "Now using default ID of 'my-modpack'");
            builder.getUpdateManifest().setId("my-modpack");
        }
        
        if (!Configuration.isValidId(updateManifest.getId())) {
            logger.warning("Invalid update ID! ID must match ^[A-Za-z0-9\\-_\\.]+$ " +
            		"with recommended length <= 30 chars");
            System.exit(3);
        }
        
        if (updateManifest.getName() == null) {
            logger.warning("Update name not set! Use -config and specify a configuration. " +
            		"Now using default name of 'My ModPack'");
            builder.getUpdateManifest().setName("My ModPack");
        }
        
        if (version != null) {
            updateManifest.setLatestVersion(version);
        } else {
            updateManifest.setLatestVersion((new Date()).toString());
        }

        build(builder);
    }

}
