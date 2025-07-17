# Release

* [ ] Merge Open Pull Requests
* [ ] Documentation
    - [ ] Update Documentation in the repo
    - [ ] Update Documentation on readthedocs
    - [ ] Screenshots with demo data
    - [ ] Screencasts
    - [ ] Point to example data (e.g. celltrackingchallenge, Kos data from Elephant paper)
* [ ] Check out the latest main branch
* [ ] Update license information
    * [ ] Open command line and navigate to the project root directory
    * [ ] Run "mvn license:update-file-header" and commit
* [ ] Update release date and version in CITATION.cff file and commit
* [ ] Run release Script (de-snapshots, sets a tag and sets the next snapshot version, generates javadoc, runs unit
  tests)
    * [x] Check, if github action is installed, which copies the release to maven.scijava.org
    * [ ] For reference: Release script https://github.com/scijava/scijava-scripts/blob/main/release-version.sh
    * [ ] Clone https://github.com/scijava/scijava-scripts repo
    * [ ] Ensure that one of the git remotes has the name "origin"
    * [ ] Close IntelliJ, if open
    * [ ] Run sh /path/to/release-version.sh from the geff-java root directory
    * [ ] Confirm version number
    * [ ] The release script pushes to main on github.com
        * This triggers a *github Action* which copies the version to be released to maven.scijava.org.
          cf. https://maven.scijava.org/#nexus-search;quick~geff-java
* [ ] Add Release documentation: https://github.com/mastodon-sc/geff-java/releases
* [ ] Check Release on Zenodo https://zenodo.org/account/settings/github/repository/mastodon-sc/geff-java
