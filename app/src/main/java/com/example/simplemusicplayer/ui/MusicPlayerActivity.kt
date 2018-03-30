package com.example.simplemusicplayer.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.DateFormat
import android.widget.*
import com.example.simplemusicplayer.MusicService
import com.example.simplemusicplayer.R


class MusicPlayerActivity : AppCompatActivity() {

    lateinit var mMediaBrowser: MediaBrowserCompat

    // UI関連
    lateinit var albumImageView: ImageView
    lateinit var titleTextView: TextView
    lateinit var artistTextView: TextView
    lateinit var seekBar: SeekBar
    lateinit var positionTextView: TextView
    lateinit var durationTextView: TextView
    lateinit var playPauseButton: ImageButton
    lateinit var skipToPreviousButton: ImageButton
    lateinit var skipToNextButton: ImageButton

    // MusicListActivityから受け取る値
    lateinit var mediaId: String
    var position: Int = 0
    val bundle = Bundle()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)

        // UIのセットアップ
        albumImageView = findViewById(R.id.album_image)
        titleTextView = findViewById(R.id.title)
        artistTextView = findViewById(R.id.artist)
        playPauseButton = findViewById(R.id.play_pause_button)
        durationTextView = findViewById(R.id.duration)
        positionTextView = findViewById(R.id.position)
        seekBar = findViewById(R.id.seekBar)
        skipToPreviousButton = findViewById(R.id.previous_button)
        skipToNextButton = findViewById(R.id.next_button)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null) {
                    mediaController.transportControls.seekTo(seekBar.progress.toLong())
                }
            }
        })
        skipToPreviousButton.setOnClickListener {
            mediaController.transportControls.skipToPrevious()
        }
        skipToNextButton.setOnClickListener {
            mediaController.transportControls.skipToNext()
        }

        // サービスを開始
        startService(Intent(this, MusicService::class.java))

        // MediaBrowserの作成
        mMediaBrowser = MediaBrowserCompat(this, ComponentName(this, MusicService::class.java), mConnectionCallbacks, null)

        // O以上の時、通知チャンネルの作成
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannelIfNeeded()
        }

        mediaId = intent.getStringExtra("id")
        position = intent.getIntExtra("position", 0)
        bundle.putInt("position", position)
    }

    override fun onStart() {
        super.onStart()
        // MusicServiceに接続、mConnectionCallbacksが呼び出される
        mMediaBrowser.connect()
    }

    override fun onStop() {
        super.onStop()
        // MediaController.Callbackの登録を解除
        if (MediaControllerCompat.getMediaController(this) != null) {
            MediaControllerCompat.getMediaController(this).unregisterCallback(mControllerCallback)
        }
        // 接続を切断
        mMediaBrowser.disconnect()
    }

    // 接続された時に呼び出されるコールバック
    var mConnectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()

            // MediaSessionのトークンを取得
            val token: MediaSessionCompat.Token = mMediaBrowser.sessionToken

            // MediaControllerを作成
            val mediaController = MediaControllerCompat(this@MusicPlayerActivity, token)

            // MediaControllerをセット
            MediaControllerCompat.setMediaController(this@MusicPlayerActivity, mediaController)

            // サービスから送られてくるプレイヤーの状態や曲の情報が変更された時のコールバックを設定
            mediaController.registerCallback(mControllerCallback)


            // play/pauseのクリックイベントをセット
            playPauseButton.setOnClickListener {
                val state = mediaController.playbackState.state
                if (state == PlaybackStateCompat.STATE_PLAYING) {
                    mediaController.transportControls.pause()
                } else {
                    mediaController.transportControls.play()
                }
            }

            // サービスから再生可能な曲のリストを取得
            mMediaBrowser.subscribe(mMediaBrowser.root, subscriptionCallback)

            // すでに再生中だった時の処理(UIの更新)
//            val state = mediaController.playbackState.state
//            if (state == PlaybackStateCompat.STATE_PLAYING) {
//                playPauseButton.setImageResource(R.drawable.pause_button)
//            } else if (state == PlaybackStateCompat.STATE_PAUSED) {
//                playPauseButton.setImageResource(R.drawable.play_button)
//            }

            // とりあえず今回は曲を再設定するように
            mediaController.transportControls.playFromMediaId(mediaId, bundle)

        }
    }

    // Subscribeした時に呼び出されるコールバック
    var subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
            super.onChildrenLoaded(parentId, children)
            val mediaController = MediaControllerCompat.getMediaController(this@MusicPlayerActivity)
            if (mediaController.playbackState.state == PlaybackStateCompat.STATE_NONE) {
                // 再生中でなければ始めの曲の再生をリクエスト
                mediaController.transportControls.playFromMediaId(mediaId, bundle)
            }
        }
    }


    // Serviceから送られてくる曲の情報やプレイヤーの状態が変更された時に呼ばれるコールバック
    var mControllerCallback = object : MediaControllerCompat.Callback() {
        // 再生中の曲の情報が変更された時に呼ばれる
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            if (metadata != null) {
                titleTextView.text = metadata.description.title
                artistTextView.text = metadata.getText(MediaMetadataCompat.METADATA_KEY_ARTIST)
                albumImageView.setImageBitmap(metadata.description.iconBitmap)
                durationTextView.text = DateFormat.format("m:ss", metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION))
                seekBar.max = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toInt()
            }
        }

        // プレイヤーの状態が変更された時に呼ばれる
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            if (state != null) {
                // play/pauseボタンの表示の変更
                if (state.state == PlaybackStateCompat.STATE_PLAYING) {
                    playPauseButton.setImageResource(R.drawable.pause_button)
                } else if (state.state == PlaybackStateCompat.STATE_PAUSED) {
                    playPauseButton.setImageResource(R.drawable.play_button)
                }
                // 再生時間とseekBer位置の変更
                positionTextView.text = DateFormat.format("m:ss", state.position)
                seekBar.progress = state.position.toInt()
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannelIfNeeded() {
        val CHANNEL_ID = "music"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val name = "music data"

        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance)

        channel.enableLights(true)

        channel.lightColor = Color.BLUE
        // 振動させるか(だがしかしうまく動かない模様)
        channel.enableVibration(false)
        // バイブレーションの応急処置
        channel.vibrationPattern = longArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0)

        notificationManager.createNotificationChannel(channel)
    }

}
