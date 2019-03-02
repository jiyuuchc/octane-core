# octane-core
===============

Octane-core is a JAVA libray for single-molecule localization image analyses.

It also include an optional command line interface (CLI)  that is meant to be used on HPC, where parrellel processing of a large amunt of image data is preferred.

To see the syntax of the CLI, try

java -jar \<ocatne-core jar file\> -h   

  or

java -jar \<octane-core jar file\> \<command\> -h

The available commands are:  analyze track drift cvs 

To visualize the analyses result, use the octane-view plugin in ImageJ.

The CLI depends on mmmstudio (another of my repositories). You don't need it if you are not using the CLI.

To build a single jar file with all dependencies, use 

 mvn assembly:single


