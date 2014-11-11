package environment;

import java.util.HashMap;


/**
 * Created by michael on 11/9/14.
 */
public class Environment {
    private HashMap<String, VaporClass> m_classes;

    // MANIPULATORS
    void addClass(VaporClass c)
    {
        if (m_classes == null) {
            m_classes = new HashMap<String, VaporClass>();
        }
        m_classes.put(c.getName(), c);
    }

    // ACCESSORS
    public HashMap<String, VaporClass> getClasses()
    {
        return m_classes;
    }
}
