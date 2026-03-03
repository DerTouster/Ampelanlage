package com.till;

import java.util.concurrent.atomic.AtomicBoolean;

public class TrafficLoop implements Runnable 
{
    private TrafficLight light1, light2, light3, light4;
    private ShiftRegisterManager manager;
    private AtomicBoolean crossWalkRequest;
    private PedestrianLight pedLight;

    public TrafficLoop
        (
            TrafficLight l1, 
            TrafficLight l2, TrafficLight l3, 
            TrafficLight l4, 
            ShiftRegisterManager m, 
            AtomicBoolean crossWalkRequest,
            PedestrianLight pl
        )
        {
        this.light1 = l1;
        this.light2 = l2;
        this.light3 = l3;
        this.light4 = l4;
        this.manager = m;
        this.crossWalkRequest = crossWalkRequest;
        this.pedLight = pl;
    }

    @Override
    public void run() 
    {
        try {
            while (!Thread.currentThread().isInterrupted()) 
                {
                
                // 1. Check for Crosswalk Request before starting a new cycle
                checkCrosswalk();

                // 2. PHASE 1: Street A (1 & 3) Green, Street B (2 & 4) Red
                light1.setGreen();
                light3.setGreen();
                light2.setRed();
                light4.setRed();
                manager.push();
                Thread.sleep(5000);

                // 3. TRANSITION: Street A Yellow
                light1.setYellow();
                light3.setYellow();
                manager.push();
                Thread.sleep(2000);

                // 4. PHASE 2: All Red (Safety Buffer)
                light1.setRed();
                light3.setRed();
                manager.push();
                Thread.sleep(1000);

                // 5. PHASE 3: Street B (2 & 4) Green, Street A (1 & 3) Red
                light2.setGreen();
                light4.setGreen();
                manager.push();
                Thread.sleep(5000);

                // 6. TRANSITION: Street B Yellow
                light2.setYellow();
                light4.setYellow();
                manager.push();
                Thread.sleep(2000);

                // 7. PHASE 4: All Red (Safety Buffer)
                light2.setRed();
                light4.setRed();
                manager.push();
                Thread.sleep(1000);
            }
        } 
        catch (InterruptedException e) 
        {
            System.out.println("Traffic Loop interrupted.");
            Thread.currentThread().interrupt();
        } catch (Exception e) 
        {
            System.out.println("Traffic Loop crashed: " + e.getMessage());
        }
    }

    private void checkCrosswalk() throws InterruptedException 
    {
    // Default state: Pedestrians can't walk during car cycles
        pedLight.setDontWalk(); 

        if (crossWalkRequest.get()) 
            {
            System.out.println(">>> CROSSWALK SEQUENCE STARTING <<<");
        
            // 1. All cars to Red
            light1.setRed(); light2.setRed(); light3.setRed(); light4.setRed();
            manager.push();
            Thread.sleep(2000); // Safety gap: wait for cars to actually stop

            // 2. Pedestrians WALK
            pedLight.setWalk();
            manager.push();
            System.out.println(">>> PEDESTRIANS: WALK <<<");
            Thread.sleep(6000); 

            // 3. Pedestrians DON'T WALK
            pedLight.setDontWalk();
            manager.push();
            System.out.println(">>> PEDESTRIANS: STOP <<<");
        
            // Safety gap before cars get Green
            Thread.sleep(2000); 

            crossWalkRequest.set(false);
        }
    }
}