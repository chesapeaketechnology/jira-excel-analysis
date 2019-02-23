package com.chesapeake.technology.data;

import net.rcarz.jiraclient.User;

import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * @author Proprietary information subject to the terms of a Non-Disclosure Agreement
 * @since
 */
public class MockUser extends User
{
    private Random random = new Random();

    private String name = UUID.randomUUID().toString();
    private String displayName = UUID.randomUUID().toString();
    private String email = UUID.randomUUID().toString();
    private String identifier = UUID.randomUUID().toString();
    private String url = UUID.randomUUID().toString();

    private boolean active = random.nextBoolean();

    public MockUser()
    {
        super(null, null);
    }

    @Override
    public String toString()
    {
        return name;
    }

    @Override
    public boolean isActive()
    {
        return active;
    }

    @Override
    public Map<String, String> getAvatarUrls()
    {
        return Collections.EMPTY_MAP;
    }

    @Override
    public String getDisplayName()
    {
        return displayName;
    }

    @Override
    public String getEmail()
    {
        return email;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getId()
    {
        return identifier;
    }

    @Override
    public String getUrl()
    {
        return url;
    }

    @Override
    public String getSelf()
    {
        return "self " + name;
    }
}
