package environment;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by michael on 11/10/14.
 */
public class MethodType {
    private String              m_label;
    private LinkedList <String> m_parameters;
    private String              m_defintion;

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

    // ACCESSORS
    public String getLabel()
    {
        return m_label;
    }

}
