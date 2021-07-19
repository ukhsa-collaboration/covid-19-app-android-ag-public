#!/usr/bin/env kotlin

@file:DependsOn("org.json:json:20210307")

import org.json.JSONObject
import java.io.File

main(args)

fun main(args: Array<String>) {
    val inputFolder = args[0]
    val outputFolder = args[1]
    val paths = File(inputFolder).listFiles { file -> file.isDirectory }!!
    val filenameToFile = mutableMapOf<String, MutableSet<File>>()
    paths.forEach { path ->
        path.listFiles { file ->
            file.extension == "json"
        }?.forEach { file ->
            filenameToFile.getOrPut(file.name) { mutableSetOf() }.add(file)
        }
    }
    File(outputFolder).mkdirs()
    filenameToFile.forEach { entry ->
        val jsonObjects = entry.value.map { JSONObject(it.readText()) }
        val mergedObject = jsonObjects.reduce { acc, jsonObject -> mergeScreenshots(acc, jsonObject) }
        File(outputFolder, entry.key).writeText(mergedObject.toString(4))
    }
}

fun mergeScreenshots(json0: JSONObject, json1: JSONObject): JSONObject {
    val steps0 = nameAndDescriptionToStepObject(json0)
    val steps1 = nameAndDescriptionToStepObject(json1)
    steps1.forEach { entry ->
        val file0step = steps0[entry.key]!!
        file0step.getJSONArray("screenshots").putAll(entry.value.getJSONArray("screenshots"))
    }
    return json0
}

fun nameAndDescriptionToStepObject(jsonObject: JSONObject) = jsonObject.getJSONArray("steps").associate { s ->
    val step = s as JSONObject
    val id = step.getString("name") + "|" + step.getString("description")
    id to step
}
