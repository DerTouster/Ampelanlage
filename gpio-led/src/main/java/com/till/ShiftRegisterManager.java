package com.till;

import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.context.Context;

class ShiftRegisterManager 
{
    private final DigitalOutput data, clock, latch;
    private int currentPattern = 0;

    public ShiftRegisterManager(Context pi4j, 
        int dataPin, 
        int clockPin, 
        int latchPin) 
        {
        this.data = pi4j.create(DigitalOutput
            .newConfigBuilder(pi4j)
            .address(dataPin)
            .build());
        this.clock = pi4j.create(DigitalOutput
            .newConfigBuilder(pi4j)
            .address(clockPin)
            .build());
        this.latch = pi4j.create(DigitalOutput
            .newConfigBuilder(pi4j)
            .address(latchPin)
            .build());
    }

    /**
     * Diagnostic tool: Lights up each pysical LED (0-15) one by one.
     * Helps verify that the physical wiring is correct.
     */
    public void playDemo() 
    {
        System.out.println("--- Starting Physical Pin Diagnostic (Direct Bit Access) ---");

        // 1. Ensure everything is off (sinking high)
        this.currentPattern = 0;
        push();

        // 2. Loop through all 16 physical bits of the two registers
        for (int bit = 0; bit < 16; bit++) 
            {
            // Calculate which register and pin it are on (for the console output)
            int register = (bit < 8) ? 1 : 2;
            int pin = bit % 8;

            System.out.println("Testing Physical Register " + register + ", Pin Q" + pin + " (Bit index: " + bit + ")");

            // MANUALLY set the bit without using getBitIndex mapping
            this.currentPattern = (1 << bit);
            push();
            try {
                Thread.sleep(400); // Slightly faster scan
            } catch (InterruptedException e) 
            {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // 3. Final clear
        this.currentPattern = 0;
        push();
        System.out.println("--- Physical Diagnostic Complete ---");
    }

    public synchronized void setLed(int id, boolean on) 
    {
        int bitIndex = getBitIndex(id);
        if (on)
            currentPattern |= (1 << bitIndex);
        else
            currentPattern &= ~(1 << bitIndex);
    }

    /**
     * Helper to keep the mapping logic in one place.
     */
    private int getBitIndex(int id) 
    {
        return switch (id) 
        {
            // Intersection 1 (Register 1)
            case 0 -> 0; // Red
            case 1 -> 1; // Yellow
            case 2 -> 2; // Green

            // Intersection 2 (Register 1)
            case 3 -> 3; // Red
            case 4 -> 4; // Yellow
            case 5 -> 5; // Green

            // Specials (Register 1)
            case 16 -> 6; // SpeedCam Flash
            case 17 -> 7; // Pedestrian Blue

            // Intersection 3 (Register 2)
            case 6 -> 8; // Red
            case 7 -> 9; // Yellow
            case 8 -> 10; // Green

            // Intersection 4 (Register 2)
            case 9 -> 11; // Red
            case 10 -> 12; // Yellow
            case 11 -> 13; // Green

            // Pedestrian (Register 2)
            case 12 -> 14; // Ped Red
            case 13 -> 15; // Ped Green

            default -> 0; // Safety fallback
        };
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