package com.till;

import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.*;

public class SpeedCam implements TrafficLightListener, Runnable 
{
    private final ShiftRegisterManager manager;
    private final DigitalOutput trigger;
    private final DigitalInput echo;
    private volatile boolean isMonitoring = false;
    private double baselineDistance = 0;
    private final int targetLightId = 0;
    private final int flashLedId = 16;

    public SpeedCam(Context pi4j, ShiftRegisterManager manager, 
                                    int trigPin, 
                                    int echoPin) 
    {
        this.manager = manager;

        this.trigger = pi4j.create(DigitalOutput.newConfigBuilder(pi4j)
                .address(trigPin).shutdown(DigitalState.LOW).initial(DigitalState.LOW).build());

        this.echo = pi4j.create(DigitalInput.newConfigBuilder(pi4j)
                .address(echoPin)
                .pull(PullResistance.OFF) // "OFF" to disable internal resistors
                .build());
    }

    public void calibrate() 
    {
        System.out.println("[RedLightCam] Calibrating road distance...");
        double total = 0;
        for (int i = 0; i < 5; i++) {
            total += measureDistance();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) 
            {
            }
        }
        this.baselineDistance = total / 5;
        System.out.println("[RedLightCam] Baseline set to: " + String.format("%.2f", baselineDistance) + " cm");
    }

    private double measureDistance() 
    {
        try {
            trigger.low();
            Thread.sleep(0, 5000); // Ensures it's low
            trigger.high();
            Thread.sleep(0, 10000); // 10 microsecond pulse
            trigger.low();

            // 1. Wait for Echo to go High (Start of pulse)
            long timeout = System.nanoTime() + 50_000_000; // 50ms timeout
            while (echo.isLow()) 
                {
                if (System.nanoTime() > timeout)
                    return -1;
            }
            long pulseStart = System.nanoTime();

            // 2. Wait for Echo to go Low (End of pulse)
            while (echo.isHigh()) {
                if (System.nanoTime() > (pulseStart + 50_000_000))
                    return -1;
            }
            long pulseEnd = System.nanoTime();

            long durationNs = pulseEnd - pulseStart;
            // Distance = (Time in seconds * Speed of Sound 34300 cm/s) / 2
            return (durationNs / 1_000_000_000.0) * 34300 / 2.0;
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public void run() 
    {
        boolean violationArmed = true;
        int consecutiveDetections = 0;
        long lastViolationTime = 0;
        final long COOLDOWN_MS = 2000; // Wait 2 seconds before catching another car

        while (!Thread.currentThread().isInterrupted()) 
            {
            if (isMonitoring) {
                double currentDist = measureDistance();
                long currentTime = System.currentTimeMillis();

                // Ignore impossible readings (like -1.0 or 0)
                if (currentDist > 1) 
                    {

                    // 1. DETECTION LOGIC
                    if (violationArmed && currentDist < (baselineDistance * 0.80)) {
                        consecutiveDetections++;

                        // Only trigger if we see the car for 3 consecutive polls (~150ms)
                        if (consecutiveDetections >= 3 && (currentTime - lastViolationTime > COOLDOWN_MS)) {
                            triggerFlash();
                            violationArmed = false;
                            consecutiveDetections = 0;
                            lastViolationTime = currentTime;
                            System.out.println("[RedLightCam] Violation confirmed and logged.");
                        }
                    } else if (currentDist > (baselineDistance * 0.90)) {
                        // 2. RESET LOGIC
                        consecutiveDetections = 0;
                        if (!violationArmed) {
                            violationArmed = true;
                            System.out.println("[RedLightCam] Road clear. Re-armed.");
                        }
                    }
                }
            } else {
                violationArmed = true;
                consecutiveDetections = 0;
            }

            try {
                Thread.sleep(50); // High-speed poll
            } catch (InterruptedException e) 
            {
                break;
            }
        }
    }

    private void triggerFlash() 
    {
        System.out.println("!!! RED LIGHT VIOLATION DETECTED !!!");

        // 1. Physical Flash
        manager.setLed(flashLedId, true);
        manager.push();

        // 2. Capture Evidence
        captureImage();

        try {
            Thread.sleep(100); // Duration of the flash
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        manager.setLed(flashLedId, false);
        manager.push();
    }

    private void captureImage() 
    {
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        String fileName = "violation_" + timestamp + ".jpg";

        // use -t 1 to make the capture as fast as possible (no preview delay)
        ProcessBuilder pb = new ProcessBuilder(
                "rpicam-still",
                "-n", // No preview
                "-t", "1", // 1ms delay (instant capture)
                "--tuning-file", "/usr/share/libcamera/ipa/rpi/vc4/ov5647_noir.json",
                "-o", fileName);

        try {
            System.out.println("[Camera] Taking photo: " + fileName);
            pb.start();
            // finish the loop to continue without waiting for the disk write.
        } catch (java.io.IOException e) {
            System.err.println("[Camera] Failed to capture image: " + e.getMessage());
        }
    }

    @Override
    public void onStateChanged(int lightId, boolean isRed) 
    {
        if (lightId == targetLightId) {
            this.isMonitoring = isRed;
        }
    }
}