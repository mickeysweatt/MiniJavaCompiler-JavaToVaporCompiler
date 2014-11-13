package environment;

import code_generation.CodeTemporaryPair;
import code_generation.VaporGlobals;

/**
 * Created by michael on 11/9/14.
 */
public class Variable {
    private String m_name;
    private String m_location;

    // CREATORS
    public Variable(String name)
    {
        m_name = name;
    }

    public Variable(String name, String location)
    {
        m_name     = name;
        m_location = location;
    }

    // ACCESSORS
    public String getName()
    {
        return m_name;
    }

    public String getLocation()
    {
        return m_location;
    }
}

