SK's Minecraft Launcher
=======================

SK's Minecraft Launcher is a custom Minecraft launcher with support for easy
addon installation and more.


Building a Mod Pack
-------------------

1. Build a folder with all the mods/ config/ etc. files for the package.
2. Make a copy of `sample_builder_config_minimal.xml` and edit the ID and
    name inside the file.
3. Run the command: `java -cp SKMCLauncher.jar com.sk89q.lpbuilder.UpdateBuilder -dir "/path/to/files/" -out "/path/to/www/" -config "/path/to/sample_builder_config.xml"`

You do NOT need to list every file in the modpack. That's what the tool is for!
You can, however, set patterns on certain files to put them into an
"optional component" or into a .zip. See `sample_builder_config.xml`
for a more advanced version.

After that's done, just give the URL to users of update.xml! They can use
"Install from URL..." in the launcher.

### Command Line Options ###
    
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
