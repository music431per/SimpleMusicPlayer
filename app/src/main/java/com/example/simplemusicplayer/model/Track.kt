package com.example.simplemusicplayer.model

import android.content.ContentUris
import android.database.Cursor
import android.provider.MediaStore

class Track(cursor: Cursor) {

    val id = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID))
    val path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
    val title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
    val album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM))
    val artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))
    val albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
    val artistId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID))
    val duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
    val trackNo = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.TRACK))
    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toLong())

    companion object {

        val COLUMNS = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.TRACK)

    }

}
