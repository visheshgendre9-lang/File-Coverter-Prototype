package com.example.engine

import android.content.Context
import android.net.Uri
import com.example.model.FileFormat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object DataEngine {

    fun convertData(
        context: Context,
        inputUri: Uri,
        inputFormat: FileFormat,
        targetFormat: FileFormat,
        outputDir: File,
        onProgress: (Float) -> Unit
    ): File {
        onProgress(0.2f)
        val text = context.contentResolver.openInputStream(inputUri)?.bufferedReader()?.use { it.readText() } ?: ""
        onProgress(0.5f)

        val outputFile = File(outputDir, "data_${System.currentTimeMillis()}.${targetFormat.extension}")

        val resultString = when (inputFormat.extension) {
            "json" -> when (targetFormat.extension) {
                "xml" -> jsonToXml(text)
                "yaml" -> jsonToYaml(text)
                "csv" -> jsonToCsv(text)
                "sql" -> jsonToSql(text)
                else -> text
            }
            "csv" -> when (targetFormat.extension) {
                "json" -> csvToJson(text)
                "xml" -> jsonToXml(csvToJson(text))
                "yaml" -> jsonToYaml(csvToJson(text))
                "sql" -> csvToSql(text)
                else -> text
            }
            "xml" -> when (targetFormat.extension) {
                "json" -> xmlToJson(text)
                "yaml" -> jsonToYaml(xmlToJson(text))
                else -> text
            }
            "yaml" -> when (targetFormat.extension) {
                "json" -> yamlToJson(text)
                "xml" -> jsonToXml(yamlToJson(text))
                else -> text
            }
            else -> text
        }

        onProgress(0.85f)
        outputFile.writeText(resultString)
        onProgress(1.0f)
        return outputFile
    }

    private fun jsonToXml(jsonStr: String): String {
        return try {
            val sb = StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root>\n")
            if (jsonStr.trim().startsWith("[")) {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    sb.append("  <item>\n")
                    val item = array.optJSONObject(i)
                    if (item != null) {
                        for (key in item.keys()) {
                            sb.append("    <$key>${item.get(key)}</$key>\n")
                        }
                    } else {
                        sb.append("    <value>${array.get(i)}</value>\n")
                    }
                    sb.append("  </item>\n")
                }
            } else {
                val obj = JSONObject(jsonStr)
                for (key in obj.keys()) {
                    sb.append("  <$key>${obj.get(key)}</$key>\n")
                }
            }
            sb.append("</root>")
            sb.toString()
        } catch (e: Exception) {
            "<error>Failed to parse JSON: ${e.message}</error>"
        }
    }

    private fun jsonToYaml(jsonStr: String): String {
        return try {
            val sb = StringBuilder()
            if (jsonStr.trim().startsWith("[")) {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    sb.append("- \n")
                    val item = array.optJSONObject(i)
                    if (item != null) {
                        for (key in item.keys()) {
                            sb.append("    $key: ${item.get(key)}\n")
                        }
                    } else {
                        sb.append("    item: ${array.get(i)}\n")
                    }
                }
            } else {
                val obj = JSONObject(jsonStr)
                for (key in obj.keys()) {
                    val value = obj.get(key)
                    if (value is JSONObject) {
                        sb.append("$key:\n")
                        for (subKey in value.keys()) {
                            sb.append("  $subKey: ${value.get(subKey)}\n")
                        }
                    } else {
                        sb.append("$key: $value\n")
                    }
                }
            }
            sb.toString()
        } catch (e: Exception) {
            "# YAML conversion\nerror: ${e.message}"
        }
    }

    private fun jsonToCsv(jsonStr: String): String {
        return try {
            if (!jsonStr.trim().startsWith("[")) {
                return "key,value\n" + JSONObject(jsonStr).let { obj ->
                    obj.keys().asSequence().map { "$it,${obj.get(it)}" }.joinToString("\n")
                }
            }
            val array = JSONArray(jsonStr)
            if (array.length() == 0) return ""

            val headers = mutableListOf<String>()
            val first = array.optJSONObject(0)
            if (first != null) {
                first.keys().forEach { headers.add(it) }
            }

            val sb = StringBuilder(headers.joinToString(",") + "\n")
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val row = headers.map { key ->
                    val v = item.optString(key, "")
                    if (v.contains(",") || v.contains("\"")) "\"${v.replace("\"", "\"\"")}\"" else v
                }
                sb.append(row.joinToString(",") + "\n")
            }
            sb.toString()
        } catch (e: Exception) {
            "error\n${e.message}"
        }
    }

    private fun csvToJson(csvStr: String): String {
        val lines = csvStr.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return "[]"

        val headers = lines[0].split(",").map { it.trim().trim('"') }
        val array = JSONArray()

        for (i in 1 until lines.size) {
            val tokens = lines[i].split(",").map { it.trim().trim('"') }
            val obj = JSONObject()
            for (j in headers.indices) {
                val key = headers[j]
                val value = tokens.getOrNull(j) ?: ""
                obj.put(key, value)
            }
            array.put(obj)
        }

        return array.toString(2)
    }

    private fun csvToSql(csvStr: String): String {
        val lines = csvStr.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return ""

        val tableName = "imported_records"
        val headers = lines[0].split(",").map { it.trim().trim('"') }
        val sb = StringBuilder("CREATE TABLE $tableName (\n")
        sb.append(headers.joinToString(",\n") { "  $it VARCHAR(255)" })
        sb.append("\n);\n\n")

        for (i in 1 until lines.size) {
            val tokens = lines[i].split(",").map { it.trim().trim('"').replace("'", "''") }
            val values = tokens.joinToString(", ") { "'$it'" }
            sb.append("INSERT INTO $tableName VALUES ($values);\n")
        }

        return sb.toString()
    }

    private fun jsonToSql(jsonStr: String): String {
        val csv = jsonToCsv(jsonStr)
        return csvToSql(csv)
    }

    private fun xmlToJson(xmlStr: String): String {
        // Simple tag-based parser to JSON
        val obj = JSONObject()
        val regex = Regex("<([a-zA-Z0-9_-]+)>([^<]+)</\\1>")
        regex.findAll(xmlStr).forEach { match ->
            val tag = match.groupValues[1]
            val value = match.groupValues[2].trim()
            obj.put(tag, value)
        }
        return if (obj.length() > 0) obj.toString(2) else "{\"content\": \"Parsed XML content\"}"
    }

    private fun yamlToJson(yamlStr: String): String {
        val obj = JSONObject()
        for (line in yamlStr.lines()) {
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim().removePrefix("-").trim()
                val value = parts[1].trim()
                if (key.isNotBlank()) {
                    obj.put(key, value)
                }
            }
        }
        return if (obj.length() > 0) obj.toString(2) else "{}"
    }
}
