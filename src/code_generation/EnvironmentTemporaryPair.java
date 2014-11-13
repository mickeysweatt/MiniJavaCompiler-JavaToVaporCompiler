package code_generation;

import environment.Environment;

/**
 * Created by michael on 11/10/14.
 */
public class EnvironmentTemporaryPair {
    private int m_nextTemporary;
    private Environment m_environment;

    // CREATORS
    public EnvironmentTemporaryPair(int k, Environment env) {
        m_environment = env;
        m_nextTemporary = k;
    }

    public EnvironmentTemporaryPair(Environment env) {
        m_environment = env;
        m_nextTemporary = 0;
    }

    // ACCESSORS
    public int getNextAvailableTemporary()
    {
        return m_nextTemporary;
    }

    public Environment getEnvironment()
    {
        return m_environment;
    }
}
