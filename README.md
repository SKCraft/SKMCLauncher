SK's Minecraft Launcher
=======================

SK's Minecraft Launcher is a custom Minecraft launcher.

* Easy addon and mod installation.
* Supports downloading modpacks for private servers.
* Compatible with Windows, Mac OS X, and Linux.
* Maintained by the creator of WorldEdit, WorldGuard, and more!
* With history going back to 2010!

**Download Latest Version:** http://build.sk89q.com/job/SKMCLauncher/

Screenshots
-----------

### Launcher ###

![Screenshot of main launcher](http://i.imgur.com/DJyAf22.png)

### Console Window ###

![Messages and errors](http://i.imgur.com/Af62kPm.png)

Building a Mod Pack
-------------------

1. Build a folder with all the mods/ config/ etc. files. Put in a 
   minecraft.jar, but none of the LWJGL stuff (jinput.jar, bin/natives/ etc.).
2. Open the launcher, and click "Install from URL...".

   ![Install from URL](http://i.imgur.com/w3jKtOJ.png)
3. Click "Build your own package...."

   ![Package builder](http://i.imgur.com/z7yajOd.png)

4. Change the settings appropriately.
   * Package ID should be something like `bobs-modpack`
   * Pick any name for the package, like `Bob's Modpack o' Fun`.
   * Version can be anything, and it changes when you make a new modpack 
   so the launcher knows to update.
   * Source directory is the folder with all the things that you want to 
   install (from step 1).
   * Output directory is where to place the files that you will later have 
   to upload.
   * You don't have to chave the filenames.
5. Click the build button.
6. Upload the contents of the output directory to your website.

Now that you have a URL to update.xml (like 
*http://example.com/modpack/update.xml*), give people that link and they can put it in "Install from URL..." to automatically download!

When you need to update, just upload the new files to the same place. The 
launcher will know how to update, and *will only download changed files!* (except for config/ -- that entire folder gets updated at once).

Advanced Building
-----------------

There are some features not supported by the above GUI process.

* The builder tool lets you zip up config/ so that all the configuration files are in one .zip, which is faster to download. You can't setup your own settings for other folders with the GUI.
* You can't set some files to not overwrite on update. You might do that for "default configuration" files like a list of world waypoints.
* You can't make parts of the update "optional" where players can turn that part on or off.

### How to Make More Advanced Updates ###

You can actually still use the GUI. You just need to make a "builder configuration" (see below) and input into the GUI.

1. Make a copy of [sample_builder_config.xml](sample_builder_config.xml).
2. Edit the file as needed.
3. In the GUI, click "Use Builder Configuration" at the top and select your configuration.
4. Build as normal.

Command Line
------------

You can also do all of this from terminal or command line.

Sample command:

    java -cp SKMCLauncher.jar com.sk89q.lpbuilder.UpdateBuilder -dir "/path/to/files/" -out "/path/to/www/" -config "/path/to/sample_builder_config.xml"`

**Tip:** If you use this with Git and a continuous integration server (Jenkins, TeamCity, Bamboo, etc.), you can push updates to your server's player by just pushing to Git!
    
* `-dir path_of_client_files`
* `-out output_dir`
* `-id id` (replace the ID for the mod pack)
* `-name name` (replace the name of the mod pack)
* `-version version` (set the version of this package)
* `-package-filename filename` (change package.xml to something else)
* `-update-filename filename` (change update.xml to something else)
* `-config config_path`
* `-clean` (delete contents of the output directory first)

Compiling
---------

### With Eclipse ###

If you want to open this project in Eclipse:

1. Download the source code. Use Git if you can, so updating is easier.
2. Install [Maven](http://maven.apache.org).
3. Install the [m2eclipse](http://eclipse.org/m2e/download/) for Eclipse if you 
   haven't already. Make sure to set JAVA_HOME and the path to Maven if you
   need to do that.
4. Setup a new project in Eclipse, and make sure to enable Maven support (right 
   click the project, look at the bottom).
5. m2eclipse will download everything you need. Just run the main class to run
   the launcher from Eclipse!

### Command Line ###

You need to have Maven installed (http://maven.apache.org). Once installed,
simply run:

    mvn package

Maven will automatically download dependencies for you. Note: For that to work, 
be sure to add Maven to your "PATH".


License
-------

The launcher is licensed under the GNU General Public License, version 3.

Contributions by third parties must be dual licensed under the two licenses
described within LICENSE.txt (GNU General Public License, version 3, and the
3-clause BSD license).
