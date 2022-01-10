# Version history

## Version 4.11.1 (October 11, 2019)

-   Upgrade to OpenClover 4.4.1 bug-fix release, see the [OpenClover 4.4.1 release notes](http://openclover.org/doc/openclover-4.4.1-release-notes.html)
    for more details

## Version 4.11.0 (September 26, 2019)

-   Upgrade to OpenClover 4.4.0, which primarily contains changes in
    Maven integration, see the [OpenClover 4.4.0 release notes](http://openclover.org/doc/openclover-4.4.0-release-notes.html)
    for more details

## Version 4.10.0 (September 22, 2018)

-   Upgrade to OpenClover 4.3.1, which brings support for Java 9
-   **Upgrade notes:** you have to install clover.jar in Ant's classpath
    if you want to use automatic integration in Ant builds.
    See [OpenClover 4.3.0 release notes](http://openclover.org/doc/openclover-4.3.0-release-notes.html) for
    more details.

## Version 4.9.0 (March 6, 2018)

-   Fixed bugs:
    -   [JENKINS-33610](https://issues.jenkins-ci.org/browse/JENKINS-33610) -
        fixed IOException when running Clover on remote agents (it was
        impossible to run Clover on them)
    -   [JENKINS-34439](https://issues.jenkins-ci.org/browse/JENKINS-34439) -
        solved various exceptions thrown when running Clover in
        pipieline builds
    -   [JENKINS-45981](https://issues.jenkins-ci.org/browse/JENKINS-45981) -
        removed empty 'Manage Clover' section in global config
    -   fixed automatic Clover integration not working on Windows on
        newer Jenkins versions (a change in how "cmd.exe /C ant.bat" is
        ran caused that Clover was not appending it's parameters to the
        command); also added additional logging
    -   automatic integration does not attempt to auto-integrate with
        non-Ant tasks in the project (checks for presence of "ant.bat"
        or "ant" in the command)
-   Refreshed look&feel of the 'OpenClover summary report' page
-   Minimum Jenkins version is **1.642.3** (upgraded dependency on
    Wokflow Plugin to 2.0 - aka Pipelines Plugin)

## Version 4.8.0 (June 8, 2017)

-   Plugin uses [OpenClover](http://openclover.org/) - a free and open
    source fork of Atlassian Clover (which is no longer developed by
    Atlassian, see [this blog
    post](https://www.atlassian.com/blog/announcements/atlassian-clover-open-source))
-   You no longer need a license key to run
    Clover ![(smile)](docs/images/smile.svg)

## Version 4.7.1 (December 19, 2016)

-   Fixed plugin crashes when saving configuration
    -   <https://issues.jenkins-ci.org/browse/JENKINS-38956>
    -   <https://issues.jenkins-ci.org/browse/JENKINS-39410>

## Version 4.7.0 (October 11, 2016)

-   Upgrade to Clover 4.1.2
-   New "Pass arguments' values to Ant in double quotes" checkbox.

## Version 4.6.0 (March 4, 2016)

-   Compatibility with the [Pipeline Plugin](https://www.jenkins.io/doc/book/pipeline/).
-   Minimum Jenkins version is **1.580.3**

## Version 4.5.0 (December 9, 2015)

-   Upgrade to new major Clover version which is 4.1.1. This release
    brings bunch of bug fixes and renames Clover Maven plugin
    into `clover-maven-plugin`

## Version 4.4.0 (July 18, 2014)

-   Upgrade to new major Clover release which is 4.0.0. This release
    comes with a completely redesigned HTML report, following the ADG
    (Atlassian Design Guidelines).

## Version 4.3.0 (April 1, 2014)

-   Upgrade to new major Clover release which is 3.3.0. This release
    comes with a dedicated support for Spock framework JUnit4
    parameterized tests.

## Version 4.2.0 (October 23, 2013)

-   Upgrade to new major Clover release which is 3.2.0. This release
    supports instrumentation of Java 8 language.

## Version 4.1.0 (August 13, 2012)

-   Upgrade to Clover 3.1.12.1
-   Minimum Jenkins version is **1.509.2**

## Version 4.0.6 (May 13, 2012)

-   Upgrade to Clover 3.1.5

## Version 4.0.5 (Jan 18, 2012)

-   Upgrade to Clover 3.1.3
    ([JENKINS-12448](https://issues.jenkins-ci.org/browse/JENKINS-12448)).

## Version 4.0.4 (Nov 8, 2011)

-   Upgrade to Clover 3.1.2
    ([JENKINS-11656](https://issues.jenkins-ci.org/browse/JENKINS-11656)).

## Version 4.0.2 (Jun 6, 2011)

-   Upgrade to Clover 3.1.0
    ([JENKINS-9830](https://issues.jenkins-ci.org/browse/JENKINS-9830)).
-   Minimum Jenkins version is **1.412**

## Version 4.0.1 (May 7, 2011)

-   Clover plugin uses HTML in display name
    ([JENKINS-9435](https://issues.jenkins-ci.org/browse/JENKINS-9435)).

## Version 4.0

-   Minimum Jenkins version is **1.399**
-   Fixed the icon path on configuration pages
    ([JENKINS-7795](https://issues.jenkins-ci.org/browse/JENKINS-7795)).
-   Clover Coverage Trend Report Stop Displaying For Failed Build
    ([JENKINS-3918](https://issues.jenkins-ci.org/browse/JENKINS-3918)).
-   Ignore 0/0 Conditional in coverage graph
    ([JENKINS-8198](https://issues.jenkins-ci.org/browse/JENKINS-8198)).
-   i18n & i10n(ja)

## Version 3.0.2

-   Fixed [NoStaplerConstructorException](http://issues.jenkins-ci.org/browse/JENKINS-6769) with recent Jenkins versions.
-   Update to Clover 3.0.2.

## Version 3.0.1

-   Upgrade to Clover 3.0 which has Support for Groovy
-   View [Release Notes](http://confluence.atlassian.com/display/CLOVER/Clover+3.0+Release+Notes)
-   No other changes to the Hudson Clover plugin, apart from its dependency on Clover 3.0
-   Minimum Jenkins version is **1.348**
