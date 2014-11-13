package environment;

import java.util.*;

/**
 * Created by michael on 11/10/14.
 */
public class MethodType {
    private String                     m_label;
    private LinkedList <String>        m_parameters;
    private String                     m_defintion;


    // CREATORS
    public MethodType(String label)
    {
        m_label = label;
    }

    public MethodType(String label, LinkedList<String> parameters)
    {
        m_label = label;
        m_parameters = parameters;
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
        m_defintion = definition;
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

}
