package xyz.sevive.arcaeaoffline.database.repositories

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import xyz.sevive.arcaeaoffline.core.database.entities.Chart
import xyz.sevive.arcaeaoffline.core.database.entities.PlayResult
import xyz.sevive.arcaeaoffline.core.database.repositories.ChartInfoRepository
import xyz.sevive.arcaeaoffline.core.database.repositories.PlayResultRepository
import xyz.sevive.arcaeaoffline.database.daos.OcrQueueTaskDao
import xyz.sevive.arcaeaoffline.database.entities.OcrQueueTask
import xyz.sevive.arcaeaoffline.database.entities.OcrQueueTaskStatus
import xyz.sevive.arcaeaoffline.helpers.ArcaeaPlayResultValidator

interface OcrQueueTaskRepository {
    fun findAll(): Flow<List<OcrQueueTask>>
    fun findById(id: Long): Flow<OcrQueueTask>
    fun findByStatus(statuses: List<OcrQueueTaskStatus>): Flow<List<OcrQueueTask>>
    fun findByStatus(vararg status: OcrQueueTaskStatus) = findByStatus(status.asList())
    fun findDoneWithWarning(): Flow<List<OcrQueueTask>>

    fun count(): Flow<Int>
    fun countByStatus(statuses: List<OcrQueueTaskStatus>): Flow<Int>
    fun countByStatus(vararg status: OcrQueueTaskStatus) = countByStatus(status.asList())
    fun countDoneWithWarning(): Flow<Int>

    suspend fun insert(item: OcrQueueTask): Long
    suspend fun insertBatch(items: List<OcrQueueTask>): List<Long>
    suspend fun insertBatch(uris: List<Uri>, context: Context? = null): List<Long>

    suspend fun update(item: OcrQueueTask): Int
    suspend fun updateChart(id: Long, chart: Chart): Int?
    suspend fun updatePlayResult(
        id: Long, playResult: PlayResult, chartInfoRepository: ChartInfoRepository?
    ): Int?

    suspend fun delete(item: OcrQueueTask): Int
    suspend fun delete(id: Long): Int
    suspend fun deleteBatch(items: List<OcrQueueTask>): Int
    suspend fun deleteAll()

    suspend fun save(item: OcrQueueTask, playResultRepository: PlayResultRepository)
    suspend fun save(itemId: Long, playResultRepository: PlayResultRepository) {
        val item = findById(itemId).firstOrNull() ?: return
        save(item, playResultRepository)
    }

    /**
     * Save all tasks that
     * - Is [OcrQueueTaskStatus.DONE]
     * - Has no validator warnings
     */
    suspend fun saveAll(playResultRepository: PlayResultRepository)
}

class OcrQueueTaskRepositoryImpl(private val dao: OcrQueueTaskDao) : OcrQueueTaskRepository {
    override fun findAll(): Flow<List<OcrQueueTask>> = dao.findAll()
    override fun findById(id: Long): Flow<OcrQueueTask> = dao.findById(id)
    override fun findByStatus(statuses: List<OcrQueueTaskStatus>): Flow<List<OcrQueueTask>> =
        dao.findByStatus(statuses)

    override fun findDoneWithWarning(): Flow<List<OcrQueueTask>> = dao.findDoneWithWarning()

    override fun count(): Flow<Int> = dao.count()
    override fun countByStatus(statuses: List<OcrQueueTaskStatus>): Flow<Int> =
        dao.countByStatus(statuses)

    override fun countDoneWithWarning(): Flow<Int> = dao.countDoneWithWarning()

    override suspend fun insert(item: OcrQueueTask): Long = dao.insert(item)
    override suspend fun insertBatch(items: List<OcrQueueTask>): List<Long> = dao.insertBatch(items)
    override suspend fun insertBatch(uris: List<Uri>, context: Context?): List<Long> {
        return insertBatch(uris.map { OcrQueueTask(it, context) })
    }

    override suspend fun update(item: OcrQueueTask): Int = dao.update(item)

    override suspend fun updateChart(id: Long, chart: Chart): Int? {
        var item = findById(id).firstOrNull() ?: return null

        if (item.status == OcrQueueTaskStatus.ERROR) {
            item = item.copy(status = OcrQueueTaskStatus.DONE, exception = null)
        }

        val newPlayResult =
            item.playResult?.copy(songId = chart.songId, ratingClass = chart.ratingClass)
                ?: PlayResult(songId = chart.songId, ratingClass = chart.ratingClass, score = 0)
        val warnings = ArcaeaPlayResultValidator.validate(newPlayResult, chart)

        return update(
            item.copy(playResult = newPlayResult, warnings = warnings)
        )
    }

    override suspend fun updatePlayResult(
        id: Long, playResult: PlayResult, chartInfoRepository: ChartInfoRepository?
    ): Int? {
        var item = findById(id).firstOrNull()?.copy(playResult = playResult) ?: return null

        chartInfoRepository?.let {
            chartInfoRepository.find(playResult).firstOrNull()?.let { chartInfo ->
                item = item.copy(
                    warnings = ArcaeaPlayResultValidator.validate(playResult, chartInfo)
                )
            }
        }

        return update(item)
    }

    override suspend fun delete(item: OcrQueueTask): Int = dao.delete(item)
    override suspend fun delete(id: Long): Int = dao.delete(id)
    override suspend fun deleteBatch(items: List<OcrQueueTask>): Int = dao.deleteBatch(items)
    override suspend fun deleteAll() = dao.deleteAll()

    override suspend fun save(item: OcrQueueTask, playResultRepository: PlayResultRepository) {
        if (!item.canSaveSilently) return

        playResultRepository.upsert(item.playResult!!)
        delete(item)
    }

    override suspend fun saveAll(playResultRepository: PlayResultRepository) {
        val tasks = findAll().firstOrNull() ?: return
        val tasksToSave = tasks.filter { it.canSaveSilently }

        playResultRepository.upsertBatch(*tasksToSave.map { it.playResult!! }.toTypedArray())
        deleteBatch(tasksToSave)
    }
}
