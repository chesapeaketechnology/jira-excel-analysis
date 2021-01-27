The JIRA Excel Analysis Tool
============================
The JIRA Excel Analysis Tool provides users with the ability to query JIRA for information about tickets across a 
variety of teams and projects to produce an excel file containing analytics about the team's performance. At the highest 
level the tools provides views of tickets in a hierarchical fashion allowing Initiatives to be viewed in the same page
 as their corresponding issues. Developers can also view the `Team Metrics` excel tab within the the generated reports 
 to visually identify trends in their ability to accurately estimate their workloads across sprints. 

**Readme Contents**
- [Sample usage](#sample-usage)
- [Build and development instructions](#build-and-development-instructions)
- [Change log](#change-log)
- [Contact](#contact)


Sample usage
-----------------------------------
Headless

1. Define a configuration file specifying characteristics of your JIRA projects and any filters that you would like 
applied to the project.

2. Build the jar using `gradlew build`

3. Set the following environment variables:
    - `NEXUS_USERNAME` - Set the value equal to the username used to login into JIRA
    - `NEXUS_PASSWORD` - Set the value equal to the password used to login into JIRA

4. Run the jar - From the command line run `java -jar {pathToConfiguration.conf}`
    - Replace `{pathToConfiguration.conf}` with the path to your configuration file.
         - [Example Configuration File](src/main/resources/example.conf)


Build and development instructions
-----------------------------------
To build the project:

 - Clone the repository
 - Add your JIRA credentials as environment variables
     - `NEXUS_USERNAME` - Set the value equal to the username used to login into JIRA
     - `NEXUS_PASSWORD` - Set the value equal to the password used to login into JIRA
 - Execute `gradlew build` 
 - Copy [jira-excel-analysis.jar](build/libs/jira-excel-analysis-1.0.0-SNAPSHOT.jar) to your target destination.


Changelog
-----------------------------------
1.0.0 : Initial release of JIRA Excel Analysis Tool


Contact
-----------------------------------
Corrigan R. Johnson <CJohnson@ctic-inc.com>
