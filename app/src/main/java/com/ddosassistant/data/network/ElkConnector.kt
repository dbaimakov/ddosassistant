package com.ddosassistant.data.network

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class ElkConnector(
    private val okHttp: OkHttpClient,
    private val gson: Gson = Gson()
) {
    /**
     * Creates a Kibana alerting rule.
     *
     * WARNING:
     * - Kibana rule schemas vary by version and installed plugins.
     * - This function sends a commonly used "es-query" rule template, but you MUST adapt it.
     */
    fun createEsQueryThresholdRule(
        kibanaBaseUrl: String,
        apiKey: String,
        ruleName: String,
        index: String,
        queryString: String,
        threshold: Double,
        timeWindowMinutes: Int = 5
    ): String {
        require(kibanaBaseUrl.isNotBlank()) { "Kibana base URL is blank." }
        require(apiKey.isNotBlank()) { "Kibana API key is blank." }

        val url = kibanaBaseUrl.trimEnd('/') + "/api/alerting/rule"

        // Build an Elasticsearch query JSON as a STRING (many Kibana rule types expect this).
        // You will likely need to adjust this for your own rule type and query style.
        val esQueryJson =
            "{\"query\":{\"bool\":{\"filter\":[{\"range\":{\"@timestamp\":{\"gte\":\"now-${timeWindowMinutes}m\"}}},{\"query_string\":{\"query\":\"${queryString}\"}}]}}}"

        val bodyObj = mapOf(
            "name" to ruleName,
            "tags" to listOf("ddos", "incident-assistant"),
            "rule_type_id" to "es-query",
            "consumer" to "alerts",
            "schedule" to mapOf("interval" to "1m"),
            "enabled" to true,
            "params" to mapOf(
                "index" to listOf(index),
                "timeField" to "@timestamp",
                "esQuery" to esQueryJson,
                "size" to 0,
                "threshold" to listOf(threshold),
                "thresholdComparator" to ">",
                "timeWindowSize" to timeWindowMinutes,
                "timeWindowUnit" to "m",
                "aggType" to "count",
                "groupBy" to "all"
            ),
            "actions" to emptyList<Any>() // Add Kibana connectors/actions here
        )

        val json = gson.toJson(bodyObj)
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody("application/json".toMediaTypeOrNull()))
            .header("kbn-xsrf", "true")
            .header("Authorization", normalizeAuth(apiKey))
            .build()

        okHttp.newCall(request).execute().use { resp ->
            val respBody = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IOException("Kibana rule creation failed: HTTP ${resp.code} ${resp.message}. Body=$respBody")
            }
            return respBody
        }
    }

    private fun normalizeAuth(value: String): String {
        val t = value.trim()
        return when {
            t.startsWith("ApiKey ", ignoreCase = true) -> t
            t.startsWith("Bearer ", ignoreCase = true) -> t
            else -> "ApiKey $t"
        }
    }
}
