package com.example.simplemusicplayer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Toast
import com.example.simplemusicplayer.R
import com.example.simplemusicplayer.model.MusicDataStore

class MusicListActivity : AppCompatActivity() {

    val MY_PERMISSIONS_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_list)

        // パーミッションチェック
        if (checkPermission()) {
            musicListLoader()
        }
    }

    private fun checkPermission(): Boolean {
        // M以上の時
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 許可されているかチェック
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // 権限がなければ権限を求めるダイアログを表示
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), MY_PERMISSIONS_REQUEST)
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        // 権限を求める
        if (requestCode == MY_PERMISSIONS_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 許可された時
                reload()
            } else {
                // 許可されなかった時
                Toast.makeText(this, "許可しないと利用できません。", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // activityの再起動
    private fun reload() {
        finish()
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    // listに曲をセットする
    private fun musicListLoader() {
        val tracks = MusicDataStore.getItems(this)
        val trackList = findViewById<RecyclerView>(R.id.song_list)
        val adapter = ListTrackAdapter(tracks.map { it.value })
        val layoutManager = LinearLayoutManager(this)
        trackList.setHasFixedSize(true)
        trackList.layoutManager = layoutManager
        trackList.adapter = adapter
    }

}
