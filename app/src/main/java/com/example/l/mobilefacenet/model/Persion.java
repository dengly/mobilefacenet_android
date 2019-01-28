package com.example.l.mobilefacenet.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Persion implements Parcelable {
    private String name;
    private float[] faceFeature;
    private byte[] featureData;

    public Persion(String name, float[] faceFeature){
        this.name = name;
        this.faceFeature = faceFeature;
        this.featureData = new byte[0];
    }

    public Persion(String name, byte[] featureData){
        this.name = name;
        this.faceFeature = new float[0];
        this.featureData = featureData;
    }

    protected Persion(Parcel in) {
        name = in.readString();
        faceFeature = in.createFloatArray();
        featureData = in.createByteArray();
    }

    public byte[] getFeatureData() {
        return featureData;
    }

    public String getName() {
        return name;
    }

    public float[] getFaceFeature() {
        return faceFeature;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeFloatArray(faceFeature);
        dest.writeByteArray(featureData);
    }

    public static final Creator<Persion> CREATOR = new Creator<Persion>() {
        @Override
        public Persion createFromParcel(Parcel in) {
            return new Persion(in);
        }

        @Override
        public Persion[] newArray(int size) {
            return new Persion[size];
        }
    };
}
