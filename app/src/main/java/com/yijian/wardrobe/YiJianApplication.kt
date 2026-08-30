package com.yijian.wardrobe

import android.app.Application
import androidx.work.Configuration
import com.yijian.wardrobe.data.AiSettings
import com.yijian.wardrobe.data.BackupManager
import com.yijian.wardrobe.data.LocalFiles
import com.yijian.wardrobe.data.SecureSecrets
import com.yijian.wardrobe.data.WardrobeDatabase
import com.yijian.wardrobe.network.BailianClient
import com.yijian.wardrobe.network.DeepSeekClient

class YiJianApplication : Application(), Configuration.Provider {
    val database by lazy { WardrobeDatabase(this) }
    val files by lazy { LocalFiles(this) }
    val secrets by lazy { SecureSecrets(this) }
    val aiSettings by lazy { AiSettings(this) }
    val deepSeek by lazy { DeepSeekClient(this, aiSettings, secrets, files) }
    val bailian by lazy { BailianClient(aiSettings, secrets) }
    val backups by lazy { BackupManager(this, database) }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setMinimumLoggingLevel(android.util.Log.ERROR).build()
}
