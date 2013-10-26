SK's Minecraft Launcher
=======================

SK's Minecraft Launcher is a versatile open-source custom Minecraft launcher.

* Easy addon and mod installation.
* Supports downloading modpacks for private servers.
* Compatible with Windows, Mac OS X, and Linux.
* Maintained by the creator of WorldEdit, WorldGuard, and more!
* With history going back to 2010!

Update Progress
---------------

**As of October 26, 2013, the launcher is being updated for Minecraft 1.6, 1.7
and beyond.** There are no downloads available yet.

### Roadmap ###

1. Vanilla update and launch mechanism
2. Progress reporting for tasks
3. Completion of new GUI
4. Support for modpacks
5. Self-updater

Compiling
---------

### Project Lombok ###

This project uses the [Project Lombok](http://projectlombok.org/) library,
which does a bit of magic to generate getters, setters, and other code. That
means that if you are using an IDE like NetBeans, Eclipse, or IDEA, you should
install a Project Lombok plugin so that your IDE knows about these automatic
methods.

* IDEA: In *Settings*, under *Plugins*, click *Browse repositories...* and
search for a Project Lombok plugin.
* Eclipse, NetBrains, other IDEs: See http://projectlombok.org/download.html

### With IDEA ####

IntelliJ IDEA is recommended because it has built-in in support for Maven and
other necessary features to make your life easier.

1. [IntelliJ IDEA Community Edition](Get http://www.jetbrains.com/idea/download/)
   for free from the website.
2. Install the Project Lombok support as indicated above.
3. Download the project with Git.
4. Import the project as a Maven project.

### With Eclipse ###

**Tip:** You should try to set up m2eclipse as outlined below, as m2eclipse
will automatically download other projects that SKMCLauncher needs. If not,
you have to manually collect all the dependencies!

If you want to open this project in Eclipse:

1. Download the project with Git.
2. Install [Maven](http://maven.apache.org).
3. Install the [m2eclipse](http://eclipse.org/m2e/download/) for Eclipse if you 
   haven't already. Make sure to set JAVA_HOME and the path to Maven if you
   need to do that.
4. Setup a new project in Eclipse, and make sure to enable Maven support (right 
   click the project, and select "Convert to Maven Project").

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
