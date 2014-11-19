package environment;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Author: Mickey Sweatt
 */
public class Environment {
    private HashMap<String, VaporClass> m_classes;
    private VaporClass                m_mainClass;
    private HashMap<String, Variable> m_vars;

    // CREATORS
    public Environment()
    {
        m_classes = new HashMap<String, VaporClass>();
        m_vars    = new HashMap<String, Variable>();
    }

    public Environment(Environment other)
    {
        m_classes = null == other.m_classes ? null : (HashMap<String, VaporClass>) other.m_classes.clone();
        m_vars    = null == other.m_vars    ? null : (HashMap<String, Variable>) other.m_vars.clone();
    }

    // MANIPULATORS
    public void addClass(VaporClass c)
    {
        if (m_classes == null) {
            m_classes = new HashMap<String, VaporClass>();
        }
        m_classes.put(c.getName(), c);
    }

    public void addVarsInScope(Variable v)
    {
        if (null == m_vars) {
            m_vars = new HashMap<String, Variable>();
        }
        m_vars.put(v.getName(), v);
    }

    public void setMainClass(VaporClass main)
    {
        m_mainClass = main;
    }

    // ACCESSORS
    public Set<Map.Entry <String, VaporClass> > getClasses() {
        return m_classes.entrySet();
    }

    public VaporClass getClass(String class_name)
    {
        return m_classes.get(class_name);
    }

    public Variable getVariable(String var_name)
    {
        if (null == m_vars) {
            return null;
        }
        return m_vars.get(var_name);
    }

    public VaporClass getMainClass()
    {
        return m_mainClass;
    }

}
