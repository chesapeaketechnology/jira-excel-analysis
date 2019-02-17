package com.chesapeake.technology.data;

import net.rcarz.jiraclient.Project;

import java.util.UUID;

/**
 * @author Proprietary information subject to the terms of a Non-Disclosure Agreement
 * @since
 */
public class MockProject extends Project
{
    private String name = UUID.randomUUID().toString();
    private String key = UUID.randomUUID().toString();

    public MockProject()
    {
        super(null, null);
    }

    @Override
    public String getKey()
    {
        return key;
    }

    @Override
    public String getName()
    {
        return name;
    }
}
