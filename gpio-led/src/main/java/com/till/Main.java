package com.till;

public class Main 
{
    static void main(String[] args) throws Exception
    {
        Logic logic = new Logic();

        System.out.println("Initializing . . .");

        logic.initialize();

        System.out.println("Traffic System Online.");

        logic.startSystem();
    }
}