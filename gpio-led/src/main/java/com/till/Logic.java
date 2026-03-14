package com.till;

import java.util.concurrent.atomic.AtomicBoolean;
import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.*;

public class Logic
{
    // --- Configuration Constants ---
    private static final int PIN_DATA = 17, PIN_CLOCK = 27, PIN_LATCH = 22;
    private static final int PIN_PED_BUTTON = 18, PIN_AUDIO_BUTTON = 16;
    private static final int PIN_STATUS_LED = 24;

    // --- Hardware Components ---
    private Context pi4j;
    private ShiftRegisterManager manager;
    private TrafficLight[] signals;
    private PedestrianLight pedSignal;
    private SpeedCam speedCam;

    // --- Inputs/Outputs ---
    private DigitalInput pedestrianButton;
    private DigitalInput audioFeedbackToggle;
    private DigitalOutput statusOutput;

    // --- State Management ---
    private final AtomicBoolean crossWalkRequest = new AtomicBoolean(false);
    private final AtomicBoolean pedestriansWalk = new AtomicBoolean(false);
    private final AtomicBoolean audioFeedback = new AtomicBoolean(false);

    public void initialize() 
    {
        this.pi4j = Pi4J.newContextBuilder().autoDetect().build();

        // 1. Hardware Setup
        this.manager = new ShiftRegisterManager(pi4j, PIN_DATA, PIN_CLOCK, PIN_LATCH);
        this.pedSignal = new PedestrianLight(manager);
        this.speedCam = new SpeedCam(pi4j, manager, 23, 25);
        
        initializeTrafficLights();
        initializeIO();

        // 2. Calibration & Diagnostics
        speedCam.calibrate();
        signals[0].addListener(speedCam);
        manager.playDemo();
    }

    private void initializeTrafficLights() 
    {
        this.signals = new TrafficLight[] 
        {
            new TrafficLight(manager, 0),
            new TrafficLight(manager, 1),
            new TrafficLight(manager, 2),
            new TrafficLight(manager, 3)
        };
    }

    private void initializeIO() 
    {
        this.pedestrianButton = createDigitalInput(PIN_PED_BUTTON, "PedBtn");
        this.audioFeedbackToggle = createDigitalInput(PIN_AUDIO_BUTTON, "AudioBtn");
        
        this.statusOutput = pi4j.create(DigitalOutput.newConfigBuilder(pi4j)
                .address(PIN_STATUS_LED)
                .shutdown(DigitalState.LOW)
                .initial(DigitalState.LOW)
                .build());
    }

    private DigitalInput createDigitalInput(int address, String id) 
    {
        return pi4j.create(DigitalInput.newConfigBuilder(pi4j)
                .address(address)
                .id(id)
                .pull(PullResistance.PULL_UP)
                .debounce(10000L)
                .build());
    }

    public void startSystem() 
    {
        // Create Runnables
        var trafficLoop = new TrafficLoop(signals[0], signals[1], signals[2], signals[3], 
                                         manager, crossWalkRequest, pedSignal, pedestriansWalk);
        
        var crossWalkLoop = new CrosWalkLoop(statusOutput, crossWalkRequest, audioFeedback, 
                                           pedestrianButton, audioFeedbackToggle, pedestriansWalk);

        // Start Threads
        startThread(trafficLoop, "Traffic_Worker");
        startThread(crossWalkLoop, "CrossWalk_Worker");
        startThread(speedCam, "SpeedCam_Worker");
        startThread(pedSignal, "PedestrianLight_Worker");
    }

    private void startThread(Runnable target, String name) 
    {
        Thread t = new Thread(target, name);
        t.start();
        System.out.println("Started: " + name);
    }
}