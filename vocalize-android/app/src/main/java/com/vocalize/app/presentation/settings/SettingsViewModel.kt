package com.vocalize.app.presentation.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import com.vocalize.app.dataStore
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.vocalize.app.data.local.entity.CategoryEntity
import com.vocalize.app.data.local.entity.MemoCategoryCrossRef
import com.vocalize.app.data.local.entity.MemoTagCrossRef
import com.vocalize.app.data.local.entity.PlaylistEntity
import com.vocalize.app.data.local.entity.PlaylistMemoCrossRef
import com.vocalize.app.data.local.entity.ReminderEntity
import com.vocalize.app.data.local.entity.TagEntity
import com.vocalize.app.data.repository.MemoRepository
import com.vocalize.app.util.AudioFileManager
import com.vocalize.app.util.BackupManager
import com.vocalize.app.util.Constants
import com.vocalize.app.util.DailyDigestWorker
import com.vocalize.app.util.FileCompressor
import com.vocalize.app.util.ReminderAlarmScheduler
import com.vocalize.app.util.VoskTranscriber
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val isDarkMode: Boolean = true,
    val voskEnabled: Boolean = true,
    val defaultSnoozeMinutes: Int = 10,
    val lastBackupTime: Long = 0L,
    val storageUsedMb: Float = 0f,
    val totalMemos: Int = 0,
    val isBackingUp: Boolean = false,
    val backupStatusMessage: String = "",
    val isSignedIn: Boolean = false,
    val signedInEmail: String = "",
    val isDownloadingModel: Boolean = false,
    val voskModelExists: Boolean = false,
    val snoozeOptions: List<Int> = listOf(5, 10, 15, 30, 60),
    val dailyDigestEnabled: Boolean = false,
    val dailyDigestHour: Int = 8,
    val accentColor: String = "#E53935",
    val snackbarMessage: String? = null
)

data class VocBackupMetadata(
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)

data class VocBackupPayload(
    val metadata: VocBackupMetadata = VocBackupMetadata(),
    val categories: List<CategoryEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val memos: List<com.vocalize.app.data.local.entity.MemoEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val memoCategoryCrossRefs: List<MemoCategoryCrossRef> = emptyList(),
    val memoTagCrossRefs: List<MemoTagCrossRef> = emptyList(),
    val playlistMemoCrossRefs: List<PlaylistMemoCrossRef> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val memoRepository: MemoRepository,
    private val audioFileManager: AudioFileManager,
    private val fileCompressor: FileCompressor,
    private val backupManager: BackupManager,
    private val alarmScheduler: ReminderAlarmScheduler,
    private val voskTranscriber: VoskTranscriber
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private val gson = Gson()

    init {
        loadPreferences()
        computeStorage()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            context.dataStore.data.collect { prefs ->
                _uiState.update {
                    it.copy(
                        isDarkMode = prefs[booleanPreferencesKey(Constants.PREFS_DARK_MODE)] ?: true,
                        voskEnabled = prefs[booleanPreferencesKey(Constants.PREFS_VOSK_ENABLED)] ?: true,
                        defaultSnoozeMinutes = (prefs[stringPreferencesKey(Constants.PREFS_DEFAULT_SNOOZE)] ?: "10").toIntOrNull() ?: 10,
                        lastBackupTime = prefs[longPreferencesKey(Constants.PREFS_LAST_BACKUP)] ?: 0L,
                        signedInEmail = prefs[stringPreferencesKey(Constants.PREFS_GOOGLE_ACCOUNT)] ?: "",
                        isSignedIn = (prefs[stringPreferencesKey(Constants.PREFS_GOOGLE_ACCOUNT)] ?: "").isNotBlank(),
                        dailyDigestEnabled = prefs[booleanPreferencesKey("daily_digest_enabled")] ?: false,
                        dailyDigestHour = prefs[intPreferencesKey("daily_digest_hour")] ?: 8,
                        accentColor = prefs[stringPreferencesKey("accent_color")] ?: "#E53935"
                    )
                }
            }
        }
    }

    private fun computeStorage() {
        viewModelScope.launch {
            val recordingsDir = File(context.filesDir, Constants.RECORDINGS_DIR)
            val bytes = recordingsDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            val memoCount = memoRepository.getMemoCount()
            val voskModelDir = File(context.filesDir, "${Constants.MODELS_DIR}/${Constants.VOSK_MODEL_DIR}")
            _uiState.update {
                it.copy(
                    storageUsedMb = bytes / 1_048_576f,
                    totalMemos = memoCount,
                    voskModelExists = voskModelDir.exists() && voskModelDir.listFiles()?.isNotEmpty() == true
                )
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[booleanPreferencesKey(Constants.PREFS_DARK_MODE)] = enabled }
        }
    }

    fun setVoskEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[booleanPreferencesKey(Constants.PREFS_VOSK_ENABLED)] = enabled }
        }
    }

    fun setDefaultSnooze(minutes: Int) {
        viewModelScope.launch {
            context.dataStore.edit { it[stringPreferencesKey(Constants.PREFS_DEFAULT_SNOOZE)] = minutes.toString() }
        }
    }

    fun setDailyDigestEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[booleanPreferencesKey("daily_digest_enabled")] = enabled }
            if (enabled) {
                DailyDigestWorker.schedule(context, _uiState.value.dailyDigestHour)
            } else {
                DailyDigestWorker.cancel(context)
            }
        }
    }

    fun setDailyDigestHour(hour: Int) {
        viewModelScope.launch {
            context.dataStore.edit { it[intPreferencesKey("daily_digest_hour")] = hour }
            if (_uiState.value.dailyDigestEnabled) {
                DailyDigestWorker.schedule(context, hour)
            }
        }
    }

    fun setAccentColor(colorHex: String) {
        viewModelScope.launch {
            context.dataStore.edit { it[stringPreferencesKey("accent_color")] = colorHex }
        }
    }

    private suspend fun gatherBackupPayload(): VocBackupPayload = VocBackupPayload(
        categories = memoRepository.getAllCategories().first(),
        tags = memoRepository.getAllTags().first(),
        playlists = memoRepository.getAllPlaylists().first(),
        memos = memoRepository.getAllMemos().first(),
        reminders = memoRepository.getAllReminders(),
        memoCategoryCrossRefs = memoRepository.getAllMemoCategoryCrossRefs(),
        memoTagCrossRefs = memoRepository.getAllMemoTagCrossRefs(),
        playlistMemoCrossRefs = memoRepository.getAllPlaylistMemoCrossRefs()
    )

    private fun createBackupFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        return "vocalize-backup-$timestamp.voc"
    }

    private suspend fun createBackupZip(tempDir: File, payload: VocBackupPayload): File = withContext(Dispatchers.IO) {
        tempDir.deleteRecursively()
        tempDir.mkdirs()

        val metadataFile = File(tempDir, "backup.json")
        metadataFile.writeText(gson.toJson(payload))

        val audioDir = File(tempDir, "audio").apply { mkdirs() }
        val recordingsDir = File(context.filesDir, Constants.RECORDINGS_DIR)
        recordingsDir.listFiles()?.filter { it.isFile }?.forEach { file ->
            file.copyTo(File(audioDir, file.name), overwrite = true)
        }

        val archiveFile = File(context.cacheDir, "vocalize_backup_${System.currentTimeMillis()}.zip")
        if (archiveFile.exists()) archiveFile.delete()
        if (!fileCompressor.zipDirectory(tempDir, archiveFile)) {
            throw IllegalStateException("Failed to create backup archive")
        }
        archiveFile
    }

    private suspend fun writeArchiveToFolder(folderUri: Uri, archiveFile: File, context: Context): Boolean = withContext(Dispatchers.IO) {
        val folderDocument = DocumentFile.fromTreeUri(context, folderUri)
            ?: return@withContext false
        val fileName = createBackupFileName()
        val backupDocument = folderDocument.createFile("application/octet-stream", fileName)
            ?: return@withContext false

        context.contentResolver.openOutputStream(backupDocument.uri)?.use { outputStream ->
            archiveFile.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: return@withContext false

        true
    }

    fun performExportBackup(folderUri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true, backupStatusMessage = "Preparing .voc backup...") }
            val tempDir = File(context.cacheDir, "voc_backup_temp_${System.currentTimeMillis()}")
            val zipFile = try {
                val payload = gatherBackupPayload()
                createBackupZip(tempDir, payload)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            val success = if (zipFile != null) {
                try {
                    writeArchiveToFolder(folderUri, zipFile, context)
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            } else {
                false
            }

            tempDir.deleteRecursively()
            zipFile?.delete()

            val now = System.currentTimeMillis()
            if (success) {
                context.dataStore.edit { it[longPreferencesKey(Constants.PREFS_LAST_BACKUP)] = now }
                _uiState.update { it.copy(isBackingUp = false, backupStatusMessage = "Backup created successfully.", lastBackupTime = now) }
                showSnackbar("Backup saved as .voc file")
            } else {
                _uiState.update { it.copy(isBackingUp = false, backupStatusMessage = "Failed to create backup.") }
                showSnackbar("Backup export failed")
            }
        }
    }

    private suspend fun importBackupFromZip(zipFile: File): Boolean = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "voc_import_temp_${System.currentTimeMillis()}")
        tempDir.deleteRecursively()
        tempDir.mkdirs()

        val success = fileCompressor.unzip(zipFile, tempDir)
        if (!success) return@withContext false

        val metadataFile = File(tempDir, "backup.json")
        if (!metadataFile.exists()) return@withContext false

        val payload = gson.fromJson(metadataFile.readText(), VocBackupPayload::class.java)
        val audioArchiveDir = File(tempDir, "audio")
        val audioMap = audioArchiveDir.listFiles()?.associateBy { it.name } ?: emptyMap()

        val restoredAudioPaths = mutableMapOf<String, String>()
        audioMap.forEach { (name, file) ->
            file.inputStream().use { stream ->
                audioFileManager.importAudioFile(stream, name)?.let { restoredPath ->
                    restoredAudioPaths[name] = restoredPath
                }
            }
        }

        payload.categories.forEach { memoRepository.insertCategory(it) }
        payload.tags.forEach { memoRepository.insertTag(it) }
        payload.playlists.forEach { memoRepository.insertPlaylist(it) }

        payload.memos.forEach { memo ->
            val audioName = File(memo.filePath).name
            val updatedMemo = if (restoredAudioPaths.containsKey(audioName)) {
                memo.copy(filePath = restoredAudioPaths[audioName] ?: memo.filePath)
            } else memo
            memoRepository.insertMemo(updatedMemo)
        }

        payload.reminders.forEach { memoRepository.insertReminder(it) }
        payload.memoCategoryCrossRefs.forEach { memoRepository.addMemoCategoryCrossRef(it) }
        payload.memoTagCrossRefs.forEach { memoRepository.addTagToMemo(it) }
        payload.playlistMemoCrossRefs.forEach { memoRepository.addMemoToPlaylist(it) }

        payload.memos.filter { it.hasReminder && it.reminderTime != null && it.reminderTime > System.currentTimeMillis() }
            .forEach { memo ->
                alarmScheduler.scheduleReminder(memo)
            }

        tempDir.deleteRecursively()
        true
    }

    fun performImportBackup(backupUri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true, backupStatusMessage = "Importing .voc backup...") }
            val tempZip = File(context.cacheDir, "vocalize_import_${System.currentTimeMillis()}.zip")
            val success = try {
                context.contentResolver.openInputStream(backupUri)?.use { input ->
                    File(context.cacheDir, tempZip.name).outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: return@launch run {
                    _uiState.update { it.copy(isBackingUp = false, backupStatusMessage = "Unable to read backup file.") }
                    showSnackbar("Backup import failed")
                    return@launch
                }
                importBackupFromZip(tempZip)
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }

            tempZip.delete()
            if (success) {
                computeStorage()
                _uiState.update { it.copy(isBackingUp = false, backupStatusMessage = "Backup imported successfully.") }
                showSnackbar("Backup restored from .voc file")
            } else {
                _uiState.update { it.copy(isBackingUp = false, backupStatusMessage = "Backup import failed.") }
                showSnackbar("Backup import failed")
            }
        }
    }

    fun performBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true, backupStatusMessage = "Starting backup...") }
            val dbFile = context.getDatabasePath(Constants.DB_NAME)
            val recordingsDir = File(context.filesDir, Constants.RECORDINGS_DIR)
            val success = backupManager.backup(dbFile, recordingsDir) { msg ->
                _uiState.update { it.copy(backupStatusMessage = msg) }
            }
            val now = System.currentTimeMillis()
            if (success) {
                context.dataStore.edit { it[longPreferencesKey(Constants.PREFS_LAST_BACKUP)] = now }
                _uiState.update { it.copy(isBackingUp = false, backupStatusMessage = "Backup successful!", lastBackupTime = now) }
            } else {
                _uiState.update { it.copy(isBackingUp = false, backupStatusMessage = "Backup failed. Sign in to Google first.") }
            }
        }
    }

    fun performRestore() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true, backupStatusMessage = "Restoring from Drive...") }
            val recordingsDir = File(context.filesDir, Constants.RECORDINGS_DIR)
            val success = backupManager.restore(recordingsDir) { msg ->
                _uiState.update { it.copy(backupStatusMessage = msg) }
            }
            _uiState.update {
                it.copy(
                    isBackingUp = false,
                    backupStatusMessage = if (success) "Restore complete! Restart app." else "Restore failed."
                )
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            context.cacheDir.deleteRecursively()
            computeStorage()
            showSnackbar("Cache cleared")
        }
    }

    fun deleteVoskModel() {
        viewModelScope.launch {
            val modelDir = File(context.filesDir, Constants.MODELS_DIR)
            modelDir.deleteRecursively()
            _uiState.update { it.copy(voskModelExists = false) }
            showSnackbar("Voice model deleted")
        }
    }

    fun downloadVoskModel() {
        if (_uiState.value.isDownloadingModel) return
        _uiState.update { it.copy(isDownloadingModel = true) }
        voskTranscriber.downloadModel(
            onProgress = { /* progress not used */ },
            onComplete = { success ->
                _uiState.update {
                    it.copy(
                        isDownloadingModel = false,
                        voskModelExists = success
                    )
                }
                showSnackbar(if (success) "Voice model downloaded" else "Download failed")
                if (success) computeStorage()
            }
        )
    }

    fun deleteAllData() {
        viewModelScope.launch {
            val all = memoRepository.getAllMemos().first()
            all.forEach { memo ->
                if (memo.hasReminder) alarmScheduler.cancelReminder(memo.id)
                audioFileManager.deleteAudioFile(memo.filePath)
            }
            memoRepository.deleteAllMemos()
            context.dataStore.edit { it.clear() }
            computeStorage()
            showSnackbar("All data deleted")
        }
    }

    fun signOut() {
        viewModelScope.launch {
            context.dataStore.edit { it[stringPreferencesKey(Constants.PREFS_GOOGLE_ACCOUNT)] = "" }
        }
    }

    fun showSnackbar(msg: String) = _uiState.update { it.copy(snackbarMessage = msg) }
    fun clearSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }
}
