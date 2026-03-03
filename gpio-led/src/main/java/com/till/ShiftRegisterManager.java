package com.till;

import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.context.Context;

class ShiftRegisterManager 
{
    private final DigitalOutput data, clock, latch;
    private int currentPattern = 0;

    public ShiftRegisterManager(Context pi4j, int dataPin, int clockPin, int latchPin) 
    {
        this.data = pi4j.create(DigitalOutput.newConfigBuilder(pi4j).address(dataPin).build());
        this.clock = pi4j.create(DigitalOutput.newConfigBuilder(pi4j).address(clockPin).build());
        this.latch = pi4j.create(DigitalOutput.newConfigBuilder(pi4j).address(latchPin).build());
    }

    /**
     * Diagnostic tool: Lights up each logical LED (0-11) one by one.
     * Helps verify that the mapping logic correctly hits physical wiring.
     */
    public void playDemo() 
    {
        System.out.println("--- Starting LED Diagnostic Demo ---");
        
        // Reset everything to off first
        this.currentPattern = 0;
        push();

        for (int i = 0; i < 12; i++) 
            {
            System.out.println("Testing LED ID: " + i + " (Bit index: " + getBitIndex(i) + ")");
            
            setLed(i, true);
            push();
            
            try 
            {
                Thread.sleep(500); // Half second per LED
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            setLed(i, false);
        }

        // Final clear
        this.currentPattern = 0;
        push();
        System.out.println("--- Diagnostic Complete ---");
    }

    public synchronized void setLed(int id, boolean on) {
    int bitIndex = getBitIndex(id);
    if (on) currentPattern |= (1 << bitIndex);
    else currentPattern &= ~(1 << bitIndex);
}

    /**
     * Helper to keep the mapping logic in one place.
     */
    private int getBitIndex(int id) 
    {
        if (id == 12) return 0; // SpeedCam Flash
        if (id == 13) return 7; // Pedestrian RED
        if (id == 14) return 8; // Pedestrian GREEN
    
        // Logical LEDs 0-11 -> Bits 1-6 and 9-14
        return (id < 6) ? (id + 1) : (id + 3);
    }

    public synchronized void push() 
    {
    latch.low();
    int sinkingPattern = (~currentPattern) & 0xFFFF; 
    for (int i = 15; i >= 0; i--) {
        clock.low();
        data.setState((sinkingPattern & (1 << i)) != 0);
        clock.high();
    }
    latch.high();
    }
}