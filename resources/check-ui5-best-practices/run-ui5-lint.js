const fs = require('fs')
const path = require('path')

const jsValidator = require("@sap/di.code-validation.js")
const xmlValidator = require("@sap/di.code-validation.xml")
const coreValidator = require("@sap/di.code-validation.core")
const ValidationMetadata = coreValidator.validationMetadata

if (process.argv.length < 3) {
    throw "Expected one argument with path to Component.js file.\nUsage: node check-ui5-best-practices.js my/path/to/Component.js\n"
}

const componentJs = process.argv[2]

if (!componentJs.toLowerCase().endsWith("component.js")) {
    throw "Argument must be a path to a Component.js file\n"
}

if (process.argv.length === 4) {
    const esLanguageLevel = process.argv[3];

    console.log(`[INFO] Setting ES language level to ${esLanguageLevel}.`);

    const basicDefaultConfig = require('./node_modules/@sap/di.code-validation.js/src/defaultConfig/basicDefaultConfig/.eslintrc.json');
    basicDefaultConfig['env'][esLanguageLevel] = true;
    fs.writeFileSync('./node_modules/@sap/di.code-validation.js/src/defaultConfig/basicDefaultConfig/.eslintrc.json', JSON.stringify(basicDefaultConfig));

    const fioriCustomRules = require('./node_modules/@sap/di.code-validation.js/src/defaultConfig/fioriCustomRules/.eslintrc.json');
    fioriCustomRules['env'][esLanguageLevel] = true;
    fs.writeFileSync('./node_modules/@sap/di.code-validation.js/src/defaultConfig/fioriCustomRules/.eslintrc.json', JSON.stringify(fioriCustomRules));
}

const projectPath = path.dirname(componentJs)
const validationMetadata = new ValidationMetadata(projectPath)

const jsResults = jsValidator.validateFiles(validationMetadata).issues
const xmlResults = xmlValidator.validateFiles(validationMetadata).issues

const mergedResults = jsResults.concat(xmlResults)

// Use a set to get rid of duplicate entries
const setOfFiles = [...new Set(mergedResults.map(issue => issue.path).sort())]

let checkstyleString = '<?xml version="1.0" encoding="UTF-8"?>\n<checkstyle version="8.16">\n'
setOfFiles.forEach(file => {
    checkstyleString += fileXmlBlock(file)
})
checkstyleString += '</checkstyle>\n'

const resultFileName = projectPath.replace(/\//g, '_').replace(/\s+/g, '_')
fs.writeFileSync(resultFileName + ".ui5lint.xml", checkstyleString)

fs.writeFileSync(resultFileName + ".ui5lint.json", JSON.stringify({ "issues": mergedResults }, null, 4))

function fileXmlBlock(file) {
    let result = `  <file name="${escapeXml(file)}">\n`
    const findingIsInCurrentFile = it => it.path === file
    mergedResults.filter(findingIsInCurrentFile).forEach(finding => {
        result += findingXmlLine(finding)
    })
    result += '  </file>\n'
    return result
}

function findingXmlLine(finding) {
    return `    <error line="${escapeXml(finding.line)}" severity="${escapeXml(finding.severity)}" message="${escapeXml(finding.message)}" source="${escapeXml(finding.source)}"/>\n`
}

function escapeXml(input) {
    return input && input.toString().replace(/"/g, "&quot;").replace(/'/g, "&apos;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/&/g, "&amp;")
}
