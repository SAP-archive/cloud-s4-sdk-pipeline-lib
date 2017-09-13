package com.sap.icd.jenkins

import com.cloudbees.groovy.cps.NonCPS

class ConfigurationMerger {
    @NonCPS
    def static merge(Map parameters, List parameterKeys, Map defaults=[:]) {
        return merge(parameters, parameterKeys, [:], [], defaults)
    }

    @NonCPS
    def static merge(Map parameters, List parameterKeys, Map configurationMap, List configurationKeys, Map defaults=[:]){
        Map merged = [:]
        merged.putAll(defaults)
        merged.putAll(filterByKeyAndNull(configurationMap, configurationKeys))
        merged.putAll(filterByKeyAndNull(parameters, parameterKeys))

        return merged
    }

    @NonCPS
    private static filterByKeyAndNull(Map map, List keys) {
        Map filteredMap = map.findAll {
            if(it.value == null){
                return false
            }
            return true
        }

        if(keys == null) {
            return filteredMap
        }

        return filteredMap.subMap(keys)
    }
}
