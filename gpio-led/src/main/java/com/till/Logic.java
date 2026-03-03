package com.till;

import java.util.concurrent.atomic.AtomicBoolean;
import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.PullResistance;

public class Logic 
{
    TrafficLight signal0;
    TrafficLight signal1;
    TrafficLight signal2;
    TrafficLight signal3;
    SpeedCam speedCam;
    private AtomicBoolean crossWalkRequest;
    private DigitalInput button;
    private DigitalOutput statusOutput;
    PedestrianLight pedSignal;
    ShiftRegisterManager manager;

    public void initialize()
    {
        Context pi4j = Pi4J.newContextBuilder()
                .add(com.pi4j.plugin.gpiod.provider.gpio.digital.GpioDDigitalOutputProvider.newInstance())
                .add(com.pi4j.plugin.gpiod.provider.gpio.digital.GpioDDigitalInputProvider.newInstance())
                .build();

        // 1. Initialize the Manager with specific GPIO pins
        this.manager = new ShiftRegisterManager(pi4j, 17, 27, 22);

        // 2. Create the 4 Traffic Light objects
        // Light 0 uses LEDs 0,1,2 | Light 1 uses LEDs 3,4,5 ...
        this.signal0 = new TrafficLight(manager, 0);
        this.signal1 = new TrafficLight(manager, 1);
        this.signal2 = new TrafficLight(manager, 2);
        this.signal3 = new TrafficLight(manager, 3);

        var buttonConfig = DigitalInput.newConfigBuilder(pi4j)
                .address(18)
                .pull(PullResistance.PULL_UP)
                .debounce(3000L) // 3ms debounce to prevent "flicker"
                .build();

        this.button = pi4j.create(buttonConfig);

        this.crossWalkRequest = new AtomicBoolean(false);

        var statusConfig = DigitalOutput.newConfigBuilder(pi4j)
            .address(24)
            .id("status")
            .shutdown(DigitalState.LOW)
            .initial(DigitalState.LOW)
            .provider("gpiod-digital-output")
            .build();

        this.statusOutput = pi4j.create(statusConfig);

        this.speedCam = new SpeedCam(pi4j, manager, 23, 25);

        speedCam.calibrate();

        signal0.addListener(speedCam);

        this.pedSignal  = new PedestrianLight(manager);

        manager.playDemo();
    }

    public void startSystem() 
    {
    // 1. Create the Loops
    TrafficLoop trafficLoop = new TrafficLoop(signal0, signal1, signal2, signal3, manager, crossWalkRequest, pedSignal);
    CrosWalkLoop crosWalkLoop = new CrosWalkLoop(statusOutput, crossWalkRequest, button);

    // 2. Wrap them in Threads
    Thread trafficThread = new Thread(trafficLoop);
    Thread crossWalkThread = new Thread(crosWalkLoop);
    Thread speedCamThread = new Thread(speedCam);

    // 3. Asighn them Names
    trafficThread.setName("Trafic_Worker");
    crossWalkThread.setName("CrossWalk_Worker");
    speedCamThread.setName("SpeedCam_Worker");
    
    // 4. Start everything
    crossWalkThread.start();
    trafficThread.start();
    speedCamThread.start(); 
    }
}