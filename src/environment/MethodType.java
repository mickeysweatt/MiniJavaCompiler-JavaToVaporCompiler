package environment;

import java.util.LinkedList;

/**
 * Author: Mickey Sweatt
 */
public class MethodType {
    private String               m_label;
    private LinkedList<Variable> m_parameters = new LinkedList<Variable>();
    private String               m_definition;
    private String               m_returnType;

    // CREATORS
    public MethodType(String label, String returnType) {
        m_label      = label;
        m_returnType = returnType;
    }

    public MethodType(String returnType, LinkedList<Variable> parameters) {
        m_returnType = returnType;
        for (Variable parameter : parameters) {
            String parameter_name = parameter.getName();
            String type_name = parameter.getType();
            m_parameters.add(new Variable(parameter_name, type_name, Variable.SCOPE.PARAMETER));
        }
    }

    // ACCESSORS
    public String getLabel() {
        return m_label;
    }

    // MANIPULATORS
    public void setLabel(String label) {
        m_label = label;
    }

    public LinkedList<Variable> getParameters() {
        if (null == m_parameters) {
            m_parameters = new LinkedList<Variable>();
        }
        return m_parameters;
    }

    public String getDefinition() {
        return m_definition;
    }

    public void setDefinition(String definition) {
        m_definition = definition;
    }

    public String getReturnType() { return m_returnType; }

}
