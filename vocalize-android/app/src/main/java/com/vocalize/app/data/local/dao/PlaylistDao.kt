package com.vocalize.app.data.local.dao

import androidx.room.*
import com.vocalize.app.data.local.entity.PlaylistEntity
import com.vocalize.app.data.local.entity.PlaylistMemoCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMemoToPlaylist(crossRef: PlaylistMemoCrossRef)

    @Delete
    suspend fun removeMemoFromPlaylist(crossRef: PlaylistMemoCrossRef)

    @Query("DELETE FROM playlist_memo_cross_ref WHERE playlistId = :playlistId AND memoId = :memoId")
    suspend fun removeMemoFromPlaylistById(playlistId: String, memoId: String)

    @Query("SELECT COUNT(*) FROM playlist_memo_cross_ref WHERE playlistId = :playlistId")
    fun getMemoCountForPlaylist(playlistId: String): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_memo_cross_ref WHERE playlistId = :playlistId AND memoId = :memoId)")
    suspend fun isMemoInPlaylist(playlistId: String, memoId: String): Boolean

    @Query("UPDATE playlist_memo_cross_ref SET position = :position WHERE playlistId = :playlistId AND memoId = :memoId")
    suspend fun updateMemoPosition(playlistId: String, memoId: String, position: Int)
}
