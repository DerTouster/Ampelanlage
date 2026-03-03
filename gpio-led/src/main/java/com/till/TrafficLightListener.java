package com.till;

public interface TrafficLightListener 
{
    void onStateChanged(int lightId, boolean isRed);
}