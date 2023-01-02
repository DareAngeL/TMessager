package com.dareangel.tmessager.ui.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.dareangel.tmessager.R

/**
 * Plays a sound
 */
class SoundPlayer(
    mContext: Context
) {

    private val mSoundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        )
        .build()

    private val mNotifSound = mSoundPool.load(mContext, R.raw.notify, 1)
    private val mSentSound = mSoundPool.load(mContext, R.raw.sent, 1)

    fun playNotificationSound() {
        mSoundPool.play(mNotifSound, 1f, 1f, 1, 0, 1f)
    }

    fun playSentSound() {
        mSoundPool.play(mSentSound, 1f, 1f, 1, 0, 1f)
    }
}