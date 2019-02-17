package com.chesapeake.technology.data;

import net.rcarz.jiraclient.Status;

import java.util.Random;

/**
 * @author Proprietary information subject to the terms of a Non-Disclosure Agreement
 * @since
 */
public class MockStatus extends Status
{
    private static Random random = new Random();
    private static String[] statusTypes = {"TODO", "In Progress", "Done"};

    private String name = statusTypes[random.nextInt(statusTypes.length)];

    public MockStatus()
    {
        super(null, null);
    }

    @Override
    public String toString()
    {
        return name;
    }

    @Override
    public String getDescription()
    {
        return name;
    }

    @Override
    public String getName()
    {
        return name;
    }
}
