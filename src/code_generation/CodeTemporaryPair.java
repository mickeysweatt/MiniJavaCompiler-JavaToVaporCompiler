package code_generation;

import java.util.Map;

/**
 * Created by admin on 11/10/14.
 */
public class CodeTemporaryPair {
    private int    m_nextTemporary;
    private String m_code;
    private String m_resultLocation;

    public CodeTemporaryPair(String code, int nextTemporary)
    {
        m_code = code;
        m_nextTemporary = nextTemporary;
    }

    public CodeTemporaryPair(String code, int nextTemporary, String loc)
    {
        m_code = code;
        m_nextTemporary = nextTemporary;
        m_resultLocation = loc;
    }

    public void setResultLocation(String loc)
    {
        m_resultLocation = loc;
    }

    public String getCode()
    {
        return m_code;
    }

    public String getResultLocation()
    {
        return m_resultLocation;
    }

    public int getNextAvailableTemporary()
    {
        return m_nextTemporary;
    }
}
