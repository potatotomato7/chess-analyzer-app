package com.example.myapplication.network

import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class LLMHelper(private val apiKey: String) {

    private val client = OkHttpClient()

    fun askChatGPT(prompt: String, callback: (String) -> Unit) {
        val url = "https://api.openai.com/v1/chat/completions"

        val json = JSONObject()
        json.put("model", "gpt-3.5-turbo")
        json.put("messages", listOf(
            mapOf("role" to "system", "content" to "You are a friendly chess expert that explains engine moves like a casual sounding coach."),
            mapOf("role" to "user", "content" to prompt)
        ))

        val body = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("Failed to contact ChatGPT: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonData = response.body?.string()
                try {
                    val jsonObject = JSONObject(jsonData!!)
                    val message = jsonObject
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                    callback(message.trim())
                } catch (e: Exception) {
                    callback("Error parsing ChatGPT response")
                }
            }
        })
    }
}