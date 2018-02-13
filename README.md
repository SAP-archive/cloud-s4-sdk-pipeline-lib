# Pipeline Library for the SAP S/4HANA Cloud SDK
 
 
## Description

 This pipeline library is used by [SAP S/4HANA Cloud SDK Pipeline](https://github.com/SAP/cloud-s4-sdk-pipeline).
 It defines the common steps (functions) in a Jenkins pipeline to build, test and deploy applications to the SAP Cloud Platform.
  
 ## Requirements
 
 To use the pipeline library you must have a git project which uses a pipeline, such as the [SAP S/4HANA Cloud SDK Pipeline](https://github.com/SAP/cloud-s4-sdk-pipeline).
 
 ## Download and Installation
 
 To use the library in a pipeline you have to configure this library as a [globally shared library](https://jenkins.io/doc/book/pipeline/shared-libraries/).
 
 To avoid the manual steps described in that documentation you can use the SAP S/4HANA Cloud SDK Cx Server.
 
 For instantiating the SAP S/4HANA Cloud SDK Cx Server, you need to provide a suitable host with a Linux operating system and Docker installed. Please also ensure that the user with whom you start the Cx Server belongs to the [docker group](https://docs.docker.com/engine/installation/linux/linux-postinstall/).
 
 Your project source files need to be available on a git or GitHub server, which is accessible from the Cx Server host.
 
 The lifecycle of the Cx Server is maintained by a script called `cx-server`.
 It can be found in the same named folder on the root of each SAP S/4HANA Cloud SDK project archetype. Together with the `server.cfg` file, this is all you need for starting your instance of the SAP S/4HANA Cloud SDK Cx Server.
 
 To create a new project using the SDK execute the following command:
 
 ```shell
  mvn archetype:generate -DarchetypeGroupId=com.sap.cloud.s4hana.archetypes -DarchetypeArtifactId=scp-cf-tomee -DarchetypeVersion=RELEASE
 ```
 
 In the new project, there is a folder called `cx-server`.
 This folder needs to be copied to the future host on which the Cx Server is intended to run.
 
 On the host machine execute the following command in the folder `cx-server`.
 This will start the Jenkins server.
 ```shell
  ./cx-server start
 ```

 In Jenkins click on "New Item" and create a new "Multibranch Pipeline" for your repository.
 
 Now you can use the pipeline in a `Jenkinsfile` by adding the following line on top of the file. 
  ```shell
 @Library(['s4sdk-pipeline-library']) _
  ```
  
 If you decide for [SAP S/4HANA Cloud SDK Pipeline](https://github.com/SAP/cloud-s4-sdk-pipeline), this is already done for you. 
 
## Known Issues
Currently, there are no known issues.

## How to obtain support
If you need any support, have any question or have found a bug, please report it as an issue in the repository.

## License
Copyright (c) 2017-2018 SAP SE or an SAP affiliate company. All rights reserved.
This file is licensed under the Apache Software License, v. 2 except as noted otherwise in the [LICENSE file](LICENSE).
