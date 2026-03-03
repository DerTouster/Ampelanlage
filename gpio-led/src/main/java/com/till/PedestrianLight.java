package com.till;

public class PedestrianLight {
    private final ShiftRegisterManager manager;
    private final int redId = 13;
    private final int greenId = 14;

    public PedestrianLight(ShiftRegisterManager manager) {
        this.manager = manager;
    }

    public void setWalk() {
        manager.setLed(redId, false);
        manager.setLed(greenId, true);
    }

    public void setDontWalk() {
        manager.setLed(redId, true);
        manager.setLed(greenId, false);
    }
    
    public void setAllOff() {
        manager.setLed(redId, false);
        manager.setLed(greenId, false);
    }
}