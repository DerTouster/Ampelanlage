package com.till;

import java.util.concurrent.atomic.AtomicBoolean;

import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;

public class CrosWalkLoop implements Runnable 
{
    private DigitalOutput statusOutput;
    private AtomicBoolean pedestrianRequest;
    private AtomicBoolean audioFeedback;
    private DigitalInput pedestrianButton;
    private DigitalInput audioButton;
    private AtomicBoolean pedestriansWalk;

    public CrosWalkLoop(
            DigitalOutput statusOutput,
            AtomicBoolean cr,
            AtomicBoolean af,
            DigitalInput pedestrianButton,
            DigitalInput audioButton,
            AtomicBoolean pedWalk) 
    {
        this.statusOutput = statusOutput;
        this.pedestrianRequest = cr;
        this.audioFeedback = af;
        this.pedestrianButton = pedestrianButton;
        this.audioButton = audioButton;
        this.pedestriansWalk = pedWalk;

        initialize();
    }

    private void initialize() 
    {
        // Listeners
        pedestrianButton.addListener(event -> 
            {
            if (event.state() == DigitalState.LOW) 
                {
                pedestrianRequest.set(true);
                System.out.println("Pedestrian Request Received");
            }
        });

        audioButton.addListener(event -> 
            {
            if (event.state() == DigitalState.LOW) 
                {
                audioFeedback.set(!audioFeedback.get());
                System.out.println("Audio Feedback: " + (audioFeedback.get() ? "ON" : "OFF"));
            }
        });
    }

    @Override
    public void run() 
    {
        try {
            while (!Thread.currentThread().isInterrupted()) 
                {
                // 1. THE GATEKEEPER: Check if Audio Feedback is enabled
                // If false, loop here and do nothing else.
                while (!audioFeedback.get()) 
                    {
                    // Ensures the LED is actually OFF while waiting
                    statusOutput.low();

                    Thread.sleep(150); // Check every 150ms to save CPU

                    // Allow the thread to exit if interrupted during wait
                    if (Thread.currentThread().isInterrupted())
                        return;
                }

                // 2. THE BLINK LOGIC: Only reached if audioFeedback is TRUE
                // Determine speed based on pedestrianRequest
                int sleepTime = pedestriansWalk.get() ? 200 : 800;

                statusOutput.high();
                Thread.sleep(100);

                statusOutput.low();
                Thread.sleep(sleepTime);
            }
        } catch (InterruptedException e) 
        {
            Thread.currentThread().interrupt();
        } finally {
            // Safety: Ensures LED is off if the thread stops
            statusOutput.low();
        }
    }
}