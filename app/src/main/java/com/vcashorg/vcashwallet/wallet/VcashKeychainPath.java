package com.vcashorg.vcashwallet.wallet;

import java.nio.ByteBuffer;

import static java.nio.ByteOrder.BIG_ENDIAN;

public class VcashKeychainPath {
    public int mDepth;
    public int[] mPath;

    public VcashKeychainPath(int depth, byte[] pathData){
        mDepth = depth;

        if (pathData.length == 16){
            mPath = new int[4];
            ByteBuffer buf = ByteBuffer.wrap(pathData).order(BIG_ENDIAN);
            mPath[0] = buf.getInt();
            mPath[1] = buf.getInt();
            mPath[2] = buf.getInt();
            mPath[3] = buf.getInt();
        }
    }

    public VcashKeychainPath(int depth, int d1, int d2, int d3, int d4){
        mDepth = depth;
        mPath = new int[4];
        mPath[0] = d1;
        mPath[1] = d2;
        mPath[2] = d3;
        mPath[3] = d4;
    }

    public byte[] pathData(){
        ByteBuffer buf = ByteBuffer.allocate(16).order(BIG_ENDIAN);
        buf.putInt(mPath[0]);
        buf.putInt(mPath[1]);
        buf.putInt(mPath[2]);
        buf.putInt(mPath[3]);
        return buf.array();
    }

    public VcashKeychainPath nextPath(){
        VcashKeychainPath path = new VcashKeychainPath(this.mDepth, this.mPath[0], this.mPath[1], this.mPath[2], this.mPath[3]);
        path.mPath[path.mDepth-1] += 1;
        return path;
    }
}
