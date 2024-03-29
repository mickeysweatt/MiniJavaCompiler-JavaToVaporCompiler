package environment;

/**
 * Author: Michael Sweatt
 */

import code_generation.CodeGenerationUtil;
import code_generation.CodeTemporaryPair;
import code_generation.VaporGlobals;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class VaporClass {
    // DATA
    private LinkedHashSet<Variable>           m_instanceVars = new LinkedHashSet<Variable>();
    private LinkedHashMap<String, MethodType> m_methods      = new LinkedHashMap<String, MethodType>();
    private LinkedHashSet<VaporClass>         m_supers       = new LinkedHashSet<VaporClass>();
    private String m_name = "";

    // CREATORS
    public VaporClass(String name) {
        m_name = name;
    }

    // MANIPULATORS
    public void addInstanceVariable(Variable v) {
        m_instanceVars.add(v);
    }

    public void addInstanceVariable(String varName, String varType) {
        String offset = String.format("%d", (1 + m_instanceVars.size()) * VaporGlobals.WORD_SIZE);
        addInstanceVariable(new Variable(varName, varType, offset, Variable.SCOPE.INSTANCE_VAR));
    }

    public void addMethod(MethodType m, String method_name) {
        String full_name = m_name + "." + method_name;
        m.setLabel(full_name);
        m_methods.put(method_name, m);
    }

    public void addSuper(VaporClass v) {
        m_supers.add(v);
    }

    public void setInstanceVars(LinkedHashSet<Variable> vars) {
        m_instanceVars = vars;
    }

    // ACCESSORS
    public String getName() {
        return m_name;
    }

    public Set<Map.Entry<String, MethodType>> getMethods() {
        return m_methods.entrySet();
    }

    public void setMethods(LinkedHashMap<String, MethodType> methods) {
        m_methods = methods;
    }

    public int getSize() {
        return VaporGlobals.WORD_SIZE * (1 + m_instanceVars.size());
    }

    public CodeTemporaryPair getConstructor(int objectAddressLocation) {
        String temporary = "t." + objectAddressLocation;
        String code = String.format("%s = HeapAllocZ(%d)\n", temporary, getSize());
        // zero out variables
        code += String.format("[%s] = :%s\n", temporary, CodeGenerationUtil.vtableLabel(this));
        return new CodeTemporaryPair(code, objectAddressLocation + 1, temporary);
    }

    public VaporClass getSuper() {

        if (m_supers.size() < 1) {
            return null;
        }
        return m_supers.iterator().next();
    }

    public LinkedHashSet<VaporClass> getHierarchy() {
        return m_supers;
    }

    public void setHierarchy(LinkedHashSet<VaporClass> supers) {
        m_supers = supers;
    }


//    public Variable getVariable(String varName) {
//        Variable rval = null;
//        if (null != m_instanceVars) {
//            for (Variable v : m_instanceVars) {
//                if (v.getName().equals(varName)) {
//                    rval = v;
//                    break;
//                }
//            }
//        }
//        return rval;
//    }

    public LinkedHashSet<Variable> getVariables() {
        if (null == m_instanceVars) {
            m_instanceVars = new LinkedHashSet<Variable>();
        }
        return m_instanceVars;
    }

    public MethodType getMethod(String method_name) {
        return m_methods.get(method_name);
    }

    public int getMethodOffset(String methodName) {
        int offset = 0;
        for (Map.Entry<String, MethodType> curr : m_methods.entrySet()) {
            if (methodName.equals(curr.getKey())) {
                break;
            } else {
                offset += 4;
            }
        }
        return offset;
    }

}
