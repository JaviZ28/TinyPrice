package com.apps.tinyprice.data.repository

import com.apps.tinyprice.data.model.ProductOffer

interface DealsRepository {
    val isPreviewData: Boolean
    suspend fun search(query: String): List<ProductOffer>
}

object PreviewDealsRepository : DealsRepository {
    override val isPreviewData = true

    override suspend fun search(query: String): List<ProductOffer> {
        val q = query.trim().ifBlank { "Producto de ejemplo" }

        return listOf(
            ProductOffer(
                id = "demo-1",
                store = "Demo de TinyPrice",
                productName = "$q — ejemplo",
                price = "Ver en fuente",
                sourceUrl = "https://example.com",
                summary = "Este es un resultado ficticio para probar la interfaz.",
                tags = listOf("Demostración")
            )
        )
    }
}