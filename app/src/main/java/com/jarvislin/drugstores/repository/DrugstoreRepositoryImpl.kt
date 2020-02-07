package com.jarvislin.drugstores.repository

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.jarvislin.domain.entity.Drugstore
import com.jarvislin.domain.entity.OpenData
import com.jarvislin.domain.repository.DrugstoreRepository
import com.jarvislin.drugstores.MarkerCacheManager.Companion.MAX_MARKER_AMOUNT
import com.jarvislin.drugstores.base.App
import com.jarvislin.drugstores.data.db.DrugstoreDao
import com.jarvislin.drugstores.extension.toJson
import com.jarvislin.drugstores.extension.toList
import io.reactivex.Completable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.nio.charset.Charset


class DrugstoreRepositoryImpl(private val drugstoreDao: DrugstoreDao) : DrugstoreRepository {
    private val client: OkHttpClient by lazy { OkHttpClient() }
    override fun fetchOpenData(): Single<List<OpenData>> {
        return Single.create<List<OpenData>> { emitter ->
            val request: Request = Request.Builder()
                .url("https://data.nhi.gov.tw/Datasets/Download.ashx?rid=A21030000I-D50001-001&l=https://data.nhi.gov.tw/resource/mask/maskdata.csv")
                .build()

            client.newCall(request).execute().use { response ->
                response.body()?.string()?.let {
                    emitter.onSuccess(
                        csvReader().readAllWithHeader(it).map {
                            OpenData(
                                id = it["醫事機構代碼"] ?: error("wrong key"),
                                adultMaskAmount = it["成人口罩總剩餘數"]?.toInt() ?: error("wrong key"),
                                childMaskAmount = it["兒童口罩剩餘數"]?.toInt() ?: error("wrong key"),
                                updateAt = it["來源資料時間"] ?: error("wrong key")
                            )
                        }
                    )
                }
            }
        }.subscribeOn(Schedulers.io())
    }

    override fun saveOpenData(data: List<OpenData>): Completable {
        Timber.e(data.toJson())
        return Completable.complete()
    }

    override fun deleteOpenData(): Single<Int> {
        return drugstoreDao.deleteOpenData()
            .subscribeOn(Schedulers.io())
    }

    override fun insertDrugstores(stores: List<Drugstore>): Completable {
        return drugstoreDao.insertDrugstores(stores)
            .subscribeOn(Schedulers.io())
    }

    override fun initDrugstores(): Single<List<Drugstore>> {
        return Single.create<List<Drugstore>> { emitter ->
            try {
                App.instance().assets.open("info.json").use { stream ->
                    val size: Int = stream.available()
                    val buffer = ByteArray(size)
                    stream.read(buffer)
                    emitter.onSuccess(String(buffer, Charset.forName("UTF-8")).toList())
                }
            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }.subscribeOn(Schedulers.io())
    }

    override fun fetchNearStores(latitude: Double, longitude: Double): Single<List<Drugstore>> {
        return drugstoreDao.selectNearStores(latitude, longitude, MAX_MARKER_AMOUNT)
            .subscribeOn(Schedulers.io())
    }

}