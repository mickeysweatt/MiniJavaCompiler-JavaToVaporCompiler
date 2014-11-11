package environment;

/**
 * Created by michael on 11/9/14.
 */

import code_generation.CodeGenerationUtil;
import code_generation.CodeTemporaryPair;
import code_generation.VaporGlobals;

import java.util.*;

public class VaporClass {
    // DATA
    private LinkedHashSet<Variable>     m_instaceVars;
    private HashMap<String ,MethodType> m_methods;
    private String                      m_name;

    // CREATORS
    public VaporClass(String name)
    {
        m_name = name;
    }

    // MANIPULATORS
    public void addInstanceVariable(Variable v)
    {
        if (null == m_instaceVars) {
            m_instaceVars = new LinkedHashSet<Variable>();
        }
        m_instaceVars.add(v);
    }

    public void addMethod(MethodType m, String method_name)
    {
        if (null == m_methods) {
            m_methods = new HashMap<String, MethodType>();
        }
        String full_name = m_name + "." + method_name;
        m.setLabel(full_name);
        m_methods.put(method_name, m);
    }

    // ACCESSORS
    public String getName()
    {
        return m_name;
    }

    public Set<Map.Entry<String, MethodType>> getMethods() { return m_methods.entrySet(); }

    public int getSize()
    {
        return VaporGlobals.WORD_SIZE * (1 + m_instaceVars.size());
    }

    public CodeTemporaryPair getConstructor(int objectAddressLocation)
    {
        String temporary = "t." + objectAddressLocation;
        String code = String.format("%s = HeapAllocZ(%d)\n", temporary, getSize());
        // zero out variables
        if (null != m_instaceVars && m_instaceVars.size() > 0) {
            for (int i = 1; i <= m_instaceVars.size(); ++i)
            {
                code += String.format("[%s + %d] = 0\n", temporary, 4*i);
            }
        }
        return new CodeTemporaryPair(code, objectAddressLocation + 1);
    }

}
