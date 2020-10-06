## Context

*If applied, this commit will ...*

## Definition of Done
Please consider all items and remove only if not applicable.

- [ ] I carefully reviewed my own pull request before assigning someone.
- [ ] Changes to the configuration are also documented in the [configuration.md](https://github.com/SAP/jenkins-library/blob/master/documentation/docs/pipelines/cloud-sdk/configuration.md)
- [ ] release-notes-draft.md is updated
- [ ] Pipeline config schema is updated [in schema store](https://github.com/SchemaStore/schemastore/blob/master/src/schemas/json/cloud-sdk-pipeline-config-schema.json)
- [ ] The [feature matrix](https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/doc/pipeline/build-tools.md#feature-matrix) is updated, if a new feature is added for one of the build tools
- [ ] Important design decisions are documented as an [ADR](https://github.com/SAP/cloud-s4-sdk-pipeline/tree/master/doc/architecture/decisions)
- [ ] This change is operations-relevant and I have updated the operations guide correspondingly
- [ ] There are tests covering this change

## Merging
Please use squash merge and provide a good commit message, e.g. inspired by the context above. 
Make sure to remove the reference to this pull request and that this template is not part of the commit message.
Also, please ensure the commit message is not just a list of "WIP" commit messages.
