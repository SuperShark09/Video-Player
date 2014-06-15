/*****************************************************************************
 * PlayerControlWheel.java
 *****************************************************************************
 * Copyright © 2012-2013 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/

package org.videolan.vlc.widget;

import com.mine.videoplayer.R;
import org.videolan.vlc.Util;
import org.videolan.vlc.VLCApplication;
import org.videolan.vlc.interfaces.IPlayerControl;
import org.videolan.vlc.interfaces.OnPlayerControlListener;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class PlayerControlWheel extends LinearLayout implements IPlayerControl {

    private final SeekBar mWheel;
    private OnPlayerControlListener listener = null;

    private boolean mSeekable = true;
    private static final int WHEEL_DEAD_ZONE = 7;
    private static final int WHEEL_RANGE = 60;
    private int mMiddle = 0;
    private long mPosition = 0;
    private long mSeekTo = -1;

    public PlayerControlWheel(Context context) {
        super(context);

        LayoutInflater.from(context).inflate(R.layout.player_contol_wheel, this, true);

        mWheel = (SeekBar) findViewById(R.id.player_overlay_wheelbar);
        mWheel.setMax((WHEEL_DEAD_ZONE + WHEEL_RANGE) * 2);
        mMiddle = WHEEL_DEAD_ZONE + WHEEL_RANGE;
        mWheel.setProgress(mMiddle);
        mWheel.setOnSeekBarChangeListener(mWheelListener);
    }

    private final OnSeekBarChangeListener mWheelListener = new OnSeekBarChangeListener() {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mPosition = listener != null ? listener.onWheelStart() : 0;
            mSeekTo = -1;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (listener != null) {
                // if in dead zone, pause/unpause
                if (mSeekTo < 0)
                    listener.onPlayPause();
                else if(mSeekable)
                    listener.onSeekTo(mSeekTo);
                listener.onShowInfo(null);
            }
            seekBar.setProgress(mMiddle);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!fromUser)
                return;
            int delta = progress - mMiddle;
            if (Math.abs(delta) >= WHEEL_DEAD_ZONE) {
                delta -= Math.signum(delta) * WHEEL_DEAD_ZONE;
                mSeekTo = Math.max(0, mPosition + delta * 1000);
            }
            else
                delta = 0;
            if(mSeekTo >= 0 && listener != null)
                if(mSeekable)
                    listener.onShowInfo(String.format("%s%ds (%s)", delta >= 0 ? "+" : "", delta, Util.millisToString(mSeekTo)));
                else
                    listener.onShowInfo(VLCApplication.getAppContext().getString(R.string.unseekable_stream));

            if(!mSeekable) {
                // if the stream is unseekable do not show the user a seeking bar
                seekBar.setProgress(mMiddle);
            }
        }
    };

    @Override
    public void setState(boolean isPlaying) {
        if (isPlaying) {
            mWheel.setThumb(getResources().getDrawable(R.drawable.wheel_pause));
        } else {
            mWheel.setThumb(getResources().getDrawable(R.drawable.wheel_play));
        }
        mWheel.setThumbOffset(0);

        //force a refresh
        mWheel.layout(mWheel.getLeft() - 1, mWheel.getTop(), mWheel.getRight(), mWheel.getBottom());
        mWheel.layout(mWheel.getLeft() + 1, mWheel.getTop(), mWheel.getRight(), mWheel.getBottom());
    }

    @Override
    public void setOnPlayerControlListener(OnPlayerControlListener listener) {
        this.listener = listener;
    }

    @Override
    public void setSeekable(boolean isSeekable) {
        mSeekable = isSeekable;
    }
}
