package com.apps.tinyprice.data.repository

import com.apps.tinyprice.BuildConfig
import com.apps.tinyprice.data.model.ProductOffer
import com.apps.tinyprice.data.remote.ApiErrorResponse
import com.apps.tinyprice.data.remote.SearchProductsRequest
import com.apps.tinyprice.data.remote.TinyPriceApi
import com.apps.tinyprice.data.remote.TinyPriceApiFactory
import com.apps.tinyprice.data.remote.toProductOffer
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RemoteDealsRepository(
    private val api: TinyPriceApi = TinyPriceApiFactory.create(),
    private val gson: Gson = Gson()
) : DealsRepository {

    override val isPreviewData = false

    override suspend fun search(query: String): List<ProductOffer> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.searchProducts(SearchProductsRequest(query))

                if (!response.isSuccessful) {
                    val message = runCatching {
                        response.errorBody()?.charStream()?.use {
                            gson.fromJson(it, ApiErrorResponse::class.java)?.error
                        }
                    }.getOrNull()

                    throw RemoteSearchException(
                        message ?: "Error del servidor: ${response.code()}"
                    )
                }

                response.body()?.offers.orEmpty().map {
                    it.toProductOffer()
                }
            } catch (e: RemoteSearchException) {
                throw e
            } catch (_: Exception) {
                throw RemoteSearchException(
                    "No se pudo conectar con Supabase. Comprueba Internet e inténtalo de nuevo."
                )
            }
        }
}

class RemoteSearchException(message: String) : Exception(message)

object DealsRepositoryProvider {
    fun create(): DealsRepository =
        if (BuildConfig.USE_REMOTE_DEALS) {
            RemoteDealsRepository()
        } else {
            PreviewDealsRepository
        }
}