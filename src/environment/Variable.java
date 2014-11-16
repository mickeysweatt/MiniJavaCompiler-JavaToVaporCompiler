package environment;

/**
 * Author: Mickey Sweatt
 */
public class Variable {

    public enum SCOPE {
        INSTANCE_VAR,
        PARAMETER,
        LOCAL_VAR
    }

    private String m_name;
    private String m_location;
    private String m_type;
    private SCOPE m_scope;


    // CREATORS
    public Variable(String name, String type) {
        m_name = name;
        m_type = type;
        m_location = name;
    }

    public Variable(String name, String type, SCOPE scope) {
        m_name = name;
        m_type = type;
        m_scope = scope;
        m_location = name;
    }

//    public Variable(String name, String type, String location) {
//        m_name = name;
//        m_location = location;
//    }

    public Variable(String name, String type, String location, SCOPE scope) {
        m_name = name;
        m_location = location;
        m_scope = scope;
        m_type = type;
    }

    // ACCESSORS
    public String getName() {
        return m_name;
    }

    public String getLocation() {
        return m_location;
    }

    public SCOPE getScope() {
        return m_scope;
    }

    public String getType() {
        return m_type;
    }
}

