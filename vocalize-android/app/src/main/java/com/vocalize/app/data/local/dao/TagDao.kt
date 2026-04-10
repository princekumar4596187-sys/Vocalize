package com.vocalize.app.data.local.dao

import androidx.room.*
import com.vocalize.app.data.local.entity.MemoTagCrossRef
import com.vocalize.app.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getTagById(id: String): TagEntity?

    @Query("SELECT t.* FROM tags t INNER JOIN memo_tag_cross_ref mt ON t.id = mt.tagId WHERE mt.memoId = :memoId ORDER BY t.name ASC")
    fun getTagsForMemo(memoId: String): Flow<List<TagEntity>>

    @Query("SELECT memoId FROM memo_tag_cross_ref WHERE tagId = :tagId")
    fun getMemoIdsByTag(tagId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagToMemo(crossRef: MemoTagCrossRef)

    @Query("SELECT * FROM memo_tag_cross_ref")
    suspend fun getAllMemoTagCrossRefs(): List<MemoTagCrossRef>

    @Query("DELETE FROM memo_tag_cross_ref WHERE memoId = :memoId AND tagId = :tagId")
    suspend fun removeTagFromMemo(memoId: String, tagId: String)

    @Delete
    suspend fun deleteTag(tag: TagEntity)
}
