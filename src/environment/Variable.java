package environment;

/**
 * Author: Mickey Sweatt
 */
public class Variable {
    public enum TYPE {
        INSTANCE_VAR,
        PARAMETER,
        LOCAL_VAR
    }

    private String m_name;
    private String m_location;
    private TYPE m_type;

    // CREATORS
    public Variable(String name) {
        m_name = name;
    }

    public Variable(String name, TYPE type) {
        m_name = name;
        m_type = type;
        m_location = name;
    }

    public Variable(String name, String location) {
        m_name = name;
        m_location = location;
    }

    public Variable(String name, String location, TYPE type) {
        m_name = name;
        m_location = location;
        m_type = type;
    }

    // ACCESSORS
    public String getName() {
        return m_name;
    }

    public String getLocation() {
        return m_location;
    }

    public TYPE getType() {
        return m_type;
    }
}

