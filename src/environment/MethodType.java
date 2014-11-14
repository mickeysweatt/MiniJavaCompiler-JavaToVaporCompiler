package environment;

import java.util.*;

/**
 * Author: Mickey Sweatt
 */
public class MethodType {
    private String              m_label;
    private LinkedList <String> m_parameters;
    private String              m_definition;

    // CREATORS
    public MethodType(String label)
    {
        m_label = label;
    }

    public MethodType(LinkedList<String> parameters)
    {
        m_parameters = parameters;
    }

    // MANIPULATORS
    public void setLabel(String label)
    {
        m_label = label;
    }

    public void setDefinition(String definition)
    {
        m_definition = definition;
    }

    // ACCESSORS
    public String getLabel()
    {
        return m_label;
    }

    public LinkedList <String> getParameters()
    {
        if (null ==  m_parameters) {
            m_parameters = new LinkedList<String>();
        }
        return m_parameters;
    }

    public String getDefinition()
    {
        return m_definition;
    }

}
