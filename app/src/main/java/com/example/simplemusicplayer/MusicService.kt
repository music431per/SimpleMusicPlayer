package com.example.simplemusicplayer

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.content.ContextCompat
import android.support.v4.app.NotificationCompat
import android.media.AudioManager
import com.example.simplemusicplayer.model.MusicDataStore


class MusicService : MediaBrowserServiceCompat() {

    val TAG: String = MusicService::class.java.simpleName
    val MY_MEDIA_ROOT_ID: String = "root"

    lateinit var mMediaSession: MediaSessionCompat
    lateinit var mMediaPlayer: MediaPlayer
    lateinit var mStateBuilder: PlaybackStateCompat.Builder
    lateinit var mHandler: Handler
    lateinit var queueItems: List<MediaSessionCompat.QueueItem>
    var index = 0

    override fun onCreate() {
        super.onCreate()

        // MediaSessionの作成
        mMediaSession = MediaSessionCompat(this, TAG)

        // MediaSessionの機能を設定
        mMediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or    // ヘッドフォンとかのメディアボタンを扱えるように
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS  // 再生、停止、スキップ等のコントロールを提供
        )

        // クライアントからの操作に応じるコールバックを設定
        mMediaSession.setCallback(callback)

        // MediaSessionのトークンを設定
        sessionToken = mMediaSession.sessionToken

        // MediaPlayerの初期化
        mMediaPlayer = MediaPlayer()
        mMediaPlayer.setOnCompletionListener {
            // 音楽が終わった時の処理
            if (mStateBuilder.build().state != PlaybackStateCompat.STATE_NONE) {
                callback.onSkipToNext()
            }
        }

        // 再生リスト
        queueItems = MusicDataStore.getMediaItems().mapIndexed { i, mediaItem -> MediaSessionCompat.QueueItem(mediaItem.description, i.toLong()) }
        mMediaSession.setQueue(queueItems)

        // 最初のステータスの設定
        mStateBuilder = PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
                .setState(PlaybackStateCompat.STATE_NONE, mMediaPlayer.currentPosition.toLong(), 1.0F)


        // ステータスを設定
        mMediaSession.setPlaybackState(mStateBuilder.build())

        // 500msごとに再生情報を更新(再生時間が変わった時にもUI側にcallbackを返すようにするため)
        mHandler = Handler()
        mHandler.postDelayed(object : Runnable {
            override fun run() {
                // 再生中の時のみアップデート
                if (mStateBuilder.build().state == PlaybackStateCompat.STATE_PLAYING) {
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                }
                // 再度実行
                mHandler.postDelayed(this, 500)
            }
        }, 500)

    }

    override fun onDestroy() {
        super.onDestroy()
        mMediaPlayer.release()
    }


    // クライアントからの操作に応じるコールバック
    private val callback = object : MediaSessionCompat.Callback() {

        // 曲のIDから再生
        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            super.onPlayFromMediaId(mediaId, extras)
            if (extras != null){
                index = extras.getInt("position")
            }

            val path = MusicDataStore.getPath(mediaId)
            mMediaPlayer.reset()
            mMediaPlayer.setDataSource(path)
            mMediaPlayer.prepare()
            mMediaSession.setMetadata(MusicDataStore.getMetadata(mediaId))
            onPlay()
        }

        // 再生
        override fun onPlay() {
            super.onPlay()
            val audioManager: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val res = audioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
            if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mMediaSession.isActive = true
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                mMediaPlayer.start()
            }
        }

        // 一時停止
        override fun onPause() {
            super.onPause()
            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
            mMediaPlayer.pause()
            val audioManager: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.abandonAudioFocus(afChangeListener)
        }

        // 停止
        override fun onStop() {
            super.onStop()
            mMediaSession.isActive = false
            updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
            mMediaPlayer.stop()
            val audioManager: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.abandonAudioFocus(afChangeListener)
        }

        // シーク
        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
            mMediaPlayer.seekTo(pos.toInt())
        }

        // 次の曲
        override fun onSkipToNext() {
            super.onSkipToNext()
            index++
            if (index >= MusicDataStore.getMediaItems().size) {
                index = 0
            }
            onPlayFromMediaId(queueItems[index].description.mediaId, null)
        }

        // 前の曲
        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            index--
            if (index < 0) {
                index = MusicDataStore.getMediaItems().size - 1
            }
            onPlayFromMediaId(queueItems[index].description.mediaId, null)
        }
    }

    // サービスへのアクセスを制御
    // クライアントと接続する時に呼び出される
    // パッケージ名などから接続するかどうかを決定する
    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        // 接続を許可
        return BrowserRoot(MY_MEDIA_ROOT_ID, null)
    }

    // クライアント側でsubscribeが呼ばれると呼び出される
    // 音楽ライブラリの情報を返す
    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        if (MY_MEDIA_ROOT_ID == parentId) {
            // 曲のリストをクライアントに送信
            result.sendResult(MusicDataStore.getMediaItems())
        } else {
            result.sendResult(mutableListOf())
        }
    }

    // ステータスの更新
    private fun updatePlaybackState(@PlaybackStateCompat.State state: Int) {
        mStateBuilder.setState(state, mMediaPlayer.currentPosition.toLong(), 1.0F)
        mMediaSession.setPlaybackState(mStateBuilder.build())
        createNotification()
    }

    private fun createNotification() {
        val CHANNEL_ID = "music"

        val controller = mMediaSession.controller
        val mediaMetadata = controller.metadata
        val description = mediaMetadata.description

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)

        // 再生中の曲の情報をセット
        builder.setContentTitle(description.title)
                .setContentText(description.subtitle)
                .setSubText(description.description)
                .setLargeIcon(description.iconBitmap)

                // 通知をクリックするとプレイヤーを起動できるように
                .setContentIntent(controller.sessionActivity)

                // 通知をスワイプしたときサービスを停止できるように
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_STOP))

                // ロック画面でも操作ボタンを表示させる
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

                // アイコンとアクセントの色の設定
                .setSmallIcon(R.drawable.music_notification)
                .setColor(ContextCompat.getColor(this, R.color.primary_material_dark))

                // MediaStyleを利用
                .setStyle(android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mMediaSession.sessionToken)
                        .setShowActionsInCompactView(0)

                        // キャンセルボタンを追加
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                                PlaybackStateCompat.ACTION_STOP)))

        // 前の曲へのスキップボタン
        builder.addAction(NotificationCompat.Action(
                R.drawable.previous_button, "previous",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)))

        if (controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
            // 一時停止ボタン
            builder.addAction(NotificationCompat.Action(
                    R.drawable.pause_button, "pause",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                            PlaybackStateCompat.ACTION_PLAY_PAUSE)))
        } else if (controller.playbackState.state == PlaybackStateCompat.STATE_PAUSED) {
            // 再生ボタン
            builder.addAction(NotificationCompat.Action(
                    R.drawable.play_button, "play",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                            PlaybackStateCompat.ACTION_PLAY)))
        }

        // 次の曲へのスキップボタン
        builder.addAction(NotificationCompat.Action(
                R.drawable.next_button, "next",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT)))

        // 通知を表示
        startForeground(1, builder.build())

        if (controller.playbackState.state != PlaybackStateCompat.STATE_PLAYING)
            stopForeground(false)
    }

    val afChangeListener = AudioManager.OnAudioFocusChangeListener {
        when (it) {
        // フォーカスを失うか一時的にフォーカスを失った時
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ->
                // 止める
                mMediaSession.controller.transportControls.pause()
        // フォーカスをまた得た時
            AudioManager.AUDIOFOCUS_GAIN ->
                mMediaSession.controller.transportControls.play()

        }
    }


}
