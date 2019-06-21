# UI5 Lint

The `run-ui5-lint.js` script is intended to be run as part of the [`lint` stage](https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/configuration.md#lint) of the pipeline, but can also be used standalone.
It makes use of the SAPUI5-linter [described in this tutorial](https://developers.sap.com/tutorials/webide-grunt-basic.html).

To use it standalone, run the following commands in the directory of this README file:

```
npm config set @sap:registry https://npm.sap.com
npm install
node run-ui5-lint.js path/to/Component.js
```

If you need to scan multiple UI5 components, a separate invocation per component is required.

After running the script, you'll find two files, one called `[prefix].ui5lint.xml` and one called `[prefix].ui5lint.json`.
Both contain the scan results, but the JSON file has more fields and thus is the authoritative source.
The XML files is provided for convenience in the format produced by Checkstyle, so it can be easily processed in tools that are compatible with Checkstyle.
