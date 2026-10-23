package com.apps.tinyprice.presentation.deals

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.apps.tinyprice.data.model.ProductOffer

@Composable
fun DealsApp(
    state: DealsUiState,
    onEvent: (DealsEvent) -> Unit
) {
    val offer = state.selectedOffer

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Column {
                        Text("TinyPrice", fontWeight = FontWeight.Bold)
                        Text(
                            if (state.isPreviewData) "Modo demostración"
                            else "Búsqueda web en vivo",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    if (offer != null) {
                        TextButton(onClick = { onEvent(DealsEvent.Back) }) {
                            Text("← Volver")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (offer != null) {
            OfferDetail(
                offer = offer,
                modifier = Modifier.padding(padding)
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = {
                        onEvent(DealsEvent.QueryChanged(it))
                    },
                    label = { Text("¿Qué producto buscas?") },
                    placeholder = { Text("Ej. Nintendo Switch OLED") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            onEvent(DealsEvent.SubmitSearch)
                        }
                    )
                )

                Button(
                    onClick = { onEvent(DealsEvent.SubmitSearch) },
                    enabled = !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(if (state.isLoading) "Buscando…" else "Buscar ofertas")
                }

                if (state.isPreviewData) {
                    Text(
                        "Los resultados de demostración son ficticios.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                when {
                    state.isLoading -> {
                        Box(
                            Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    state.errorMessage != null -> {
                        Text(
                            state.errorMessage,
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(
                            onClick = { onEvent(DealsEvent.SubmitSearch) }
                        ) {
                            Text("Reintentar")
                        }
                    }

                    !state.hasSearched -> {
                        Text("Encuentra y compara productos en la web.")
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                onEvent(
                                    DealsEvent.UseSuggestion("Nintendo Switch OLED")
                                )
                            }
                        ) {
                            Text("Probar: Nintendo Switch OLED")
                        }
                    }

                    state.offers.isEmpty() -> {
                        Text("No se encontraron resultados.")
                    }

                    else -> {
                        Text(
                            "Resultados (${state.offers.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            items(state.offers, key = { it.id }) { item ->
                                OfferCard(item) {
                                    onEvent(DealsEvent.OpenOffer(item))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfferCard(
    offer: ProductOffer,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                offer.store,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                offer.productName,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                offer.price,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(offer.summary, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Toca para ver detalles y fuente",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun OfferDetail(
    offer: ProductOffer,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(offer.productName, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text("Tienda o fuente: ${offer.store}")
        Text("Precio: ${offer.price}")
        Text("Envío: ${offer.shipping}")
        Text("Entrega: ${offer.delivery}")
        Text("Valoración: ${offer.rating}")
        Spacer(Modifier.height(16.dp))
        Text("Descripción", fontWeight = FontWeight.Bold)
        Text(offer.summary)

        if (offer.tags.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Etiquetas: ${offer.tags.joinToString()}")
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                val url = offer.sourceUrl
                if (url.startsWith("https://") || url.startsWith("http://")) {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        )
                    }
                }
            },
            enabled = offer.sourceUrl.startsWith("https://") ||
                offer.sourceUrl.startsWith("http://")
        ) {
            Text("Abrir fuente original")
        }
    }
}