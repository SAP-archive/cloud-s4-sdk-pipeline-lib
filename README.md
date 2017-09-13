# Pipeline Library for the SAP S/4HANA Cloud SDK
 
 
 ## What is it?

 This pipeline library contains the steps required by the [Pipeline for the SAP S/4HANA Cloud SDK](https://github.com/SAP/cloud-s4-sdk-pipeline).
 
 ## How to use?
 
 To setup the environment you can start a preconfigured Jenkins server using the cx-server script included in the archetypes of the SAP S/4HANA Cloud SDK. 
 
 In order to use the pipeline just load the pipeline within your Jenkinsfile placed in the root of your project repository. You can use the following example code:
 
 ```groovy
 #!/usr/bin/env groovy 
 
 node {
     deleteDir()
     sh "git clone --depth 1 https://github.com/SAP/cloud-s4-sdk-pipeline.git pipelines"
     load './pipelines/s4sdk-pipeline.groovy'
 }
```

## Licence
The library is licensed under [Apache License 2](LICENSE).
