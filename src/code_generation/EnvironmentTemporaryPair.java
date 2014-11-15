package code_generation;

import environment.Environment;

/**
 * Author: Mickey Sweatt
 */
public class EnvironmentTemporaryPair {
    private int m_nextTemporary;
    private Environment m_environment;

    // CREATORS
    public EnvironmentTemporaryPair(Environment env, int k) {
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
