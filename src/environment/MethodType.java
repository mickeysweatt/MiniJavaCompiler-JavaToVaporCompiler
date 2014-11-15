package environment;

import java.util.*;

/**
 * Author: Mickey Sweatt
 */
public class MethodType {
    private String                m_label;
    private LinkedList <Variable> m_parameters;
    private String                m_definition;

    // CREATORS
    public MethodType(String label)
    {
        m_label = label;
    }

    public MethodType(LinkedList<String> parameters)
    {
        if (null == m_parameters) {
            m_parameters = new LinkedList<Variable>();
        }
        for (String parameter_name : parameters) {
            m_parameters.add(new Variable(parameter_name, Variable.TYPE.PARAMETER));
        }
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

    public LinkedList <Variable> getParameters()
    {
        if (null ==  m_parameters) {
            m_parameters = new LinkedList<Variable>();
        }
        return m_parameters;
    }

    public String getDefinition()
    {
        return m_definition;
    }

}
