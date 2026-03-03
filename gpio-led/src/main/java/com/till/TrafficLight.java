package com.till;

import java.util.ArrayList;
import java.util.List;

class TrafficLight 
{
    private final ShiftRegisterManager manager;
    private final int startIndex;
    private final int lightId; // 0, 1, 2, or 3
    private boolean isRed = false;
    private final List<TrafficLightListener> listeners = new ArrayList<>();

    public TrafficLight(ShiftRegisterManager manager, int lightNumber) 
    {
        this.manager = manager;
        // light0 -> 0,1,2 | light1 -> 3,4,5 | light2 -> 6,7,8 | light3 -> 9,10,11
        this.startIndex = lightNumber * 3;
        this.lightId = lightNumber;
    }

    public void addListener(TrafficLightListener listener) {
        listeners.add(listener);
    }

    public void setRed() {
        this.isRed = true;
        setStates(true, false, false);
        notifyListeners();
    }

    public void setYellow() {
        this.isRed = false;
        setStates(false, true, false);
        notifyListeners();
    }

    public void setGreen() {
        this.isRed = false;
        setStates(false, false, true);
        notifyListeners();
    }

    public void setAllOff() 
    { 
        setStates(false, false, false); 
        this.isRed = false;
    }

    private void notifyListeners() 
    {
        for (TrafficLightListener l : listeners) {
            l.onStateChanged(this.lightId, this.isRed);
        }
    }

    private void setStates(boolean red, boolean yellow, boolean green) 
    {
        manager.setLed(startIndex, red);
        manager.setLed(startIndex + 1, yellow);
        manager.setLed(startIndex + 2, green);
    }
}