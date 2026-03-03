package com.till;

import java.util.concurrent.atomic.AtomicBoolean;

import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;

public class CrosWalkLoop implements Runnable 
{
    private DigitalOutput statusOutput;
    private AtomicBoolean buttonPressed;
    private DigitalInput button;

    public CrosWalkLoop(DigitalOutput statusOutput, 
        AtomicBoolean ab, 
        DigitalInput button) 
        {
        this.statusOutput = statusOutput;
        this.buttonPressed = ab;
        this.button = button;
    }

    public void run() {
        button.addListener(event -> 
            {
            if (event.state() == DigitalState.LOW) // Button pulls to GND
                { 
                buttonPressed.set(true);
                System.out.println("Pedestrian Request Received");
            }
        });

        while (!Thread.currentThread().isInterrupted()) 
            {
            try {
                // Check current state for this cycle
                boolean request = buttonPressed.get();
                int sleepTime = request ? 100 : 500;

                // High (On)
                statusOutput.high();
                Thread.sleep(sleepTime);

                // Low (Off)
                statusOutput.low();
                Thread.sleep(sleepTime);

            } catch (InterruptedException e) 
            {
                Thread.currentThread().interrupt();
            }
        }
    }
}
