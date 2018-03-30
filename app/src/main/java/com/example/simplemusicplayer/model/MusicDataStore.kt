package com.example.simplemusicplayer.model

import android.content.Context
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import java.util.*

/**
 * 書き方がわからない！！！！！！！！
 */

object MusicDataStore {

    val musicList: TreeMap<String, Track> = TreeMap()

    fun getMediaItems(): MutableList<MediaBrowserCompat.MediaItem> {
        val result = musicList.map {
            MediaBrowserCompat.MediaItem(
                    createMediaMetadataCompat(it.value).description,
                    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
        }
        return result.toMutableList()
    }

    fun getMetadata(id: String?): MediaMetadataCompat? {
        val track = musicList[id]
        if (track != null) {
            return createMediaMetadataCompat(track)
        }
        return null
    }

    fun getPath(id: String?): String? {
        val track = musicList[id]
        if (track != null) {
            return track.path
        }
        return null
    }

    fun createMediaMetadataCompat(track: Track): MediaMetadataCompat {
        val metaData = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, track.id)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.album)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.duration)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, track.path)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, track.uri.toString())
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, track.uri.toString())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
                .build()
        return metaData
    }


    fun getItems(context: Context): TreeMap<String, Track> {
        val resolver = context.contentResolver
        val cursor = resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                Track.COLUMNS,
                null,
                null,
                null
        )
        cursor.moveToFirst()
        val tracks = arrayListOf<Track>()
        while (cursor.moveToNext()) {
            if (cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)) < 3000) {
                continue
            }
            tracks.add(Track(cursor))
        }
        cursor.close()
        for (track in tracks) {
            musicList.put(track.id, track)
        }
        return musicList
    }

}
