package com.yijian.wardrobe.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yijian.wardrobe.YiJianApplication
import com.yijian.wardrobe.data.AiCapability
import com.yijian.wardrobe.data.AiProvider
import com.yijian.wardrobe.data.JobStatus
import com.yijian.wardrobe.data.LocalFiles
import com.yijian.wardrobe.data.SecureSecrets
import com.yijian.wardrobe.data.TryOnJob
import com.yijian.wardrobe.network.SafeApiException
import com.yijian.wardrobe.network.safeError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class TryOnWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext as YiJianApplication
        val id = inputData.getString(KEY_JOB_ID) ?: return@withContext Result.failure()
        var job = app.database.tryOnJob(id) ?: return@withContext Result.failure()
        val currentFingerprint = app.secrets.fingerprint(SecureSecrets.BAILIAN_KEY)
        if (currentFingerprint == null || currentFingerprint != job.keyFingerprint) {
            fail(app, job, "KEY_CHANGED", "百炼 Key 已更换，旧任务不会使用新 Key 查询")
            return@withContext Result.failure()
        }
        try {
            var taskId = job.providerTaskId
            if (taskId.isNullOrBlank()) {
                job = update(app, job, JobStatus.UPLOADING)
                val model = job.model
                val personUrl = app.bailian.uploadTemporary(app.files.absolute(job.personPath), model)
                val topUrl = job.topPath?.let { app.bailian.uploadTemporary(app.files.absolute(it), model) }
                val bottomUrl = job.bottomPath?.let { app.bailian.uploadTemporary(app.files.absolute(it), model) }
                val coarseUrl = job.coarsePath?.let { app.bailian.uploadTemporary(app.files.absolute(it), model) }
                val shoeUrls = job.shoePaths.map { app.bailian.uploadTemporary(app.files.absolute(it), model) }
                taskId = app.bailian.submitTryOn(model, personUrl, topUrl, bottomUrl, coarseUrl, shoeUrls)
                job = job.copy(providerTaskId = taskId, status = JobStatus.SUBMITTED, updatedAt = System.currentTimeMillis())
                app.database.saveTryOnJob(job)
                if (taskId.startsWith("sync:")) {
                    val resultUrl = taskId.removePrefix("sync:")
                    job = update(app, job, JobStatus.DOWNLOADING)
                    val target = File(applicationContext.filesDir, "wardrobe_media/tryon_results/${job.id}.png")
                    app.bailian.download(resultUrl, target)
                    job = job.copy(status = JobStatus.SUCCEEDED, resultPath = LocalFiles(applicationContext).relative(target), updatedAt = System.currentTimeMillis())
                    app.database.saveTryOnJob(job)
                    app.database.recordAiRun(AiProvider.BAILIAN, job.model, "衣物分割", "SUCCEEDED")
                    return@withContext Result.success(Data.Builder().putString(KEY_JOB_ID, id).build())
                }
            }
            repeat(40) {
                val remote = app.bailian.queryTask(taskId!!)
                when (remote.status.uppercase()) {
                    "SUCCEEDED" -> {
                        val resultUrl = remote.resultUrl ?: throw SafeApiException(null, "MISSING_RESULT", "任务成功但没有结果地址")
                        job = update(app, job, JobStatus.DOWNLOADING)
                        val target = File(applicationContext.filesDir, "wardrobe_media/tryon_results/${job.id}.jpg")
                        app.bailian.download(resultUrl, target)
                        job = job.copy(status = JobStatus.SUCCEEDED, resultPath = LocalFiles(applicationContext).relative(target), updatedAt = System.currentTimeMillis())
                        app.database.saveTryOnJob(job)
                        app.database.recordAiRun(AiProvider.BAILIAN, job.model, "虚拟试穿", "SUCCEEDED")
                        return@withContext Result.success(Data.Builder().putString(KEY_JOB_ID, id).build())
                    }
                    "FAILED", "CANCELED", "UNKNOWN" -> throw SafeApiException(null, remote.code ?: "TASK_${remote.status}", remote.message ?: "百炼任务${remote.status}")
                    "RUNNING", "PRE-PROCESSING", "POST-PROCESSING" -> job = update(app, job, if (job.model == "aitryon-refiner") JobStatus.REFINING else if (job.model == "shoemodel-v1") JobStatus.REPLACING_SHOES else JobStatus.RUNNING)
                    else -> job = update(app, job, JobStatus.PENDING)
                }
                delay(3_000)
            }
            return@withContext Result.retry()
        } catch (error: Throwable) {
            val code = (error as? SafeApiException)?.code ?: "LOCAL_ERROR"
            if (job.model == "shoemodel-v1" && code == "AllocationQuota.FreeTierOnly") app.aiSettings.lockShoe(job.keyFingerprint)
            fail(app, job, code, safeError(error))
            app.database.recordAiRun(AiProvider.BAILIAN, job.model, "虚拟试穿", "FAILED", code)
            if (error is java.io.IOException && runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    private fun update(app: YiJianApplication, job: TryOnJob, status: JobStatus): TryOnJob = job.copy(status = status, updatedAt = System.currentTimeMillis()).also(app.database::saveTryOnJob)
    private fun fail(app: YiJianApplication, job: TryOnJob, code: String, message: String) = app.database.saveTryOnJob(job.copy(status = JobStatus.FAILED, errorCode = code, errorMessage = message, updatedAt = System.currentTimeMillis()))

    companion object {
        const val KEY_JOB_ID = "job_id"
        fun enqueue(context: Context, jobId: String) {
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val request = OneTimeWorkRequestBuilder<TryOnWorker>()
                .setInputData(Data.Builder().putString(KEY_JOB_ID, jobId).build())
                .setConstraints(constraints)
                .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 20, TimeUnit.SECONDS)
                .addTag("tryon").addTag("tryon-$jobId").build()
            WorkManager.getInstance(context).enqueueUniqueWork("tryon-$jobId", ExistingWorkPolicy.KEEP, request)
        }
    }
}
