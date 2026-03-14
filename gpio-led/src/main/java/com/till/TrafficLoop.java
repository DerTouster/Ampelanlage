package com.till;

import java.util.concurrent.atomic.AtomicBoolean;

public class TrafficLoop implements Runnable 
{
    // Grouping signals for easier bulk-management
    private final TrafficLight[] streetA;
    private final TrafficLight[] streetB;
    private final ShiftRegisterManager manager;
    private final AtomicBoolean crossWalkRequest;
    private final PedestrianLight pedLight;
    private final AtomicBoolean pedestrianWalk;

    // --- Timings (milliseconds) ---
    private static final int GREEN_TIME = 15_000;
    private static final int YELLOW_TIME = 2_000;
    private static final int ALL_RED_BUFFER = 2_000;
    private static final int PED_WALK_TIME = 6_000;

    public TrafficLoop(TrafficLight l1, 
            TrafficLight l2, 
            TrafficLight l3, 
            TrafficLight l4, 
            ShiftRegisterManager m, 
            AtomicBoolean crossWalkRequest, 
            PedestrianLight pl, 
            AtomicBoolean pw) 
        {
        this.streetA = new TrafficLight[]{l1, l3};
        this.streetB = new TrafficLight[]{l2, l4};
        this.manager = m;
        this.crossWalkRequest = crossWalkRequest;
        this.pedLight = pl;
        this.pedestrianWalk = pw;
    }

    @Override
    public void run() 
    {
        try {
            while (!Thread.currentThread().isInterrupted()) 
                {
                // Initial State: All Red
                setAllRed();

                // Sequence
                handlePhase(streetA, streetB); // Street A goes Green
                handlePhase(streetB, streetA); // Street B goes Green
            }
        } catch (InterruptedException e) 
        {
            Thread.currentThread().interrupt();
        }
    }

    private void handlePhase(TrafficLight[] activeStreet, TrafficLight[] idleStreet) throws InterruptedException 
    {
        // 1. Checking if pedestrians want to cross before switching
        checkCrosswalk();

        // 2. Set Active Green, Idle Red
        for (TrafficLight light : activeStreet) light.setGreen();
        for (TrafficLight light : idleStreet) light.setRed();
        manager.push();
        Thread.sleep(GREEN_TIME);

        // 3. Yellow Transition
        for (TrafficLight light : activeStreet) light.setYellow();
        manager.push();
        Thread.sleep(YELLOW_TIME);

        // 4. Back to All Red Safety Buffer
        setAllRed();
        Thread.sleep(ALL_RED_BUFFER);
    }

    private void checkCrosswalk() throws InterruptedException 
    {
        pedLight.setDontWalk();
        if (!crossWalkRequest.get()) return;

        System.out.println(">>> CROSSWALK SEQUENCE STARTING <<<");
        setAllRed();
        Thread.sleep(ALL_RED_BUFFER);

        pedLight.setWalk();
        pedestrianWalk.set(true);
        manager.push();
        
        Thread.sleep(PED_WALK_TIME);

        pedLight.setDontWalk();
        pedestrianWalk.set(false);
        manager.push();
        
        Thread.sleep(ALL_RED_BUFFER);
        crossWalkRequest.set(false);
    }

    private void setAllRed() 
    {
        for (TrafficLight light : streetA) light.setRed();
        for (TrafficLight light : streetB) light.setRed();
        pedLight.setDontWalk();
        manager.push();
    }
}