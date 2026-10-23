
package com.apps.tinyprice

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apps.tinyprice.ui.theme.TinyPriceTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val SEARCH_URL =
    "https://rsqpusnsjtfsqdkhfvvb.supabase.co/functions/v1/search-products"

data class ProductOffer(
    val store: String,
    val name: String,
    val price: String,
    val summary: String,
    val sourceUrl: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TinyPriceTheme {
                TinyPriceScreen()
            }
        }
    }
}

@Composable
fun TinyPriceScreen() {
    var query by remember { mutableStateOf("") }
    var offers by remember { mutableStateOf(emptyList<ProductOffer>()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var searched by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            Text(
                text = "TinyPrice",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Encuentra productos y compara fuentes web",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("¿Qué producto buscas?") },
                placeholder = { Text("Ej. Nintendo Switch OLED") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = {
                    if (query.trim().length < 2) {
                        error = "Escribe al menos 2 caracteres."
                        return@Button
                    }

                    scope.launch {
                        loading = true
                        error = null
                        searched = true
                        offers = emptyList()

                        try {
                            offers = searchProducts(query.trim())
                            if (offers.isEmpty()) {
                                error = "No se encontraron resultados para esta búsqueda."
                            }
                        } catch (e: Exception) {
                            error = e.message
                                ?: "No se pudo conectar con el servidor."
                        } finally {
                            loading = false
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (loading) "Buscando..." else "Buscar productos")
            }

            Spacer(Modifier.height(16.dp))

            if (loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("Consultando fuentes web...")
            }

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (searched && !loading && offers.isNotEmpty()) {
                Text(
                    text = "${offers.size} resultados encontrados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(offers) { offer ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = offer.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(6.dp))

                            Text("Fuente: ${offer.store}")
                            Text(
                                text = offer.price,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(offer.summary)

                            if (offer.sourceUrl.startsWith("https://") ||
                                offer.sourceUrl.startsWith("http://")
                            ) {
                                Spacer(Modifier.height(8.dp))

                                TextButton(
                                    onClick = {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(offer.sourceUrl)
                                        )
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Text("Visitar fuente web")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun searchProducts(query: String): List<ProductOffer> =
    withContext(Dispatchers.IO) {
        val connection = (URL(SEARCH_URL).openConnection()
                as HttpURLConnection)

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20000
            connection.readTimeout = 30000
            connection.doOutput = true
            connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=utf-8"
            )
            connection.setRequestProperty("Accept", "application/json")

            val requestBody = JSONObject()
                .put("query", query)
                .toString()

            connection.outputStream.use { output ->
                output.write(requestBody.toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode

            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseText = stream?.bufferedReader()?.use {
                it.readText()
            }.orEmpty()

            val json = try {
                JSONObject(responseText)
            } catch (_: Exception) {
                throw Exception("El servidor devolvió una respuesta no válida.")
            }

            if (status !in 200..299) {
                throw Exception(
                    json.optString(
                        "error",
                        "Error del servidor: HTTP $status"
                    )
                )
            }

            val array = json.optJSONArray("offers")
                ?: return@withContext emptyList()

            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue

                    add(
                        ProductOffer(
                            store = item.optString("store", "Fuente web"),
                            name = item.optString(
                                "productName",
                                "Producto sin título"
                            ),
                            price = item.optString("price", "Ver en fuente"),
                            summary = item.optString(
                                "summary",
                                "Consulta los detalles en la fuente original."
                            ),
                            sourceUrl = item.optString("sourceUrl", "")
                        )
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }
