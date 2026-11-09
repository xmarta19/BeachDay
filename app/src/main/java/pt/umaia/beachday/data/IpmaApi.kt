package pt.umaia.beachday.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

data class RespostaLocais(val data: List<LocalIpma>)

data class LocalIpma(
    val globalIdLocal: Int,
    val local: String,
    val idAreaAviso: String,
    val latitude: String,
    val longitude: String
)

data class RespostaTempo(val data: List<PrevisaoTempo>)

data class PrevisaoTempo(
    val forecastDate: String,
    val tMax: String,
    val precipitaProb: String,
    val classWindSpeed: Int
)

data class RespostaMar(
    val forecastDate: String,
    val data: List<PrevisaoMar>
)

data class PrevisaoMar(
    val globalIdLocal: Int,
    val totalSeaMax: String,
    val sstMax: String
)

data class AvisoIpma(
    val text: String?,
    val awarenessTypeName: String?,
    val idAreaAviso: String?,
    val startTime: String?,
    val endTime: String?,
    val awarenessLevelID: String?
)

interface IpmaApi {
    @GET("distrits-islands.json")
    suspend fun obterLocaisTempo(): RespostaLocais

    @GET("sea-locations.json")
    suspend fun obterLocaisMar(): List<LocalIpma>

    @GET("forecast/meteorology/cities/daily/{id}.json")
    suspend fun obterPrevisaoTempo(@Path("id") id: Int): RespostaTempo

    @GET("forecast/oceanography/daily/hp-daily-sea-forecast-day{dia}.json")
    suspend fun obterPrevisaoMar(@Path("dia") dia: Int): RespostaMar

    @GET("forecast/warnings/warnings_www.json")
    suspend fun obterAvisos(): List<AvisoIpma>

    companion object {
        fun criar(): IpmaApi =
            Retrofit.Builder()
                .baseUrl("https://api.ipma.pt/open-data/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(IpmaApi::class.java)
    }
}