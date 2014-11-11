package code_generation;

import java.util.Map;

/**
 * Created by admin on 11/10/14.
 */
public class CodeTemporaryPair {
    private int    m_nextTemporary;
    private String m_code;

    public CodeTemporaryPair(String code, int nextTemporary)
    {
        m_code = code;
        m_nextTemporary = nextTemporary;
    }

    public String getCode()
    {
        return m_code;
    }

    public int getNextAvailableTemporary()
    {
        return m_nextTemporary;
    }
}
