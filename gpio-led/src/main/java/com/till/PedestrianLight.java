package com.till;

public class PedestrianLight implements Runnable 
{
    private final ShiftRegisterManager manager;
    private final int redId = 12;
    private final int greenId = 13;
    private final int blueId = 17;
    private Boolean blinkFast = false;

    public PedestrianLight(ShiftRegisterManager manager) 
    {
        this.manager = manager;
    }

    public void setWalk() 
    {
        manager.setLed(redId, false);
        manager.setLed(greenId, true);
        this.blinkFast = true;
        manager.push(); // Ensures change is sent to hardware
    }

    public void setDontWalk() 
    {
        manager.setLed(redId, true);
        manager.setLed(greenId, false);
        this.blinkFast = false;
        manager.push();
    }

    @Override
    public void run()
    {
        while (!Thread.currentThread().isInterrupted()) 
            {
            try 
            {
                int sleepTime = blinkFast ? 200 : 800;

                manager.setLed(blueId, true);
                manager.push();
                Thread.sleep(100);

                manager.setLed(blueId, false);
                manager.push();
                Thread.sleep(sleepTime);

            } 
            catch (InterruptedException e) 
            {
                Thread.currentThread().interrupt();
                break;
            }   
        }
    }
}