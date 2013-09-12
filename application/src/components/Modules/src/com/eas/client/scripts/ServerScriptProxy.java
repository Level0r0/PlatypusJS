/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eas.client.scripts;

import com.eas.client.AppClient;
import com.eas.script.ScriptUtils;
import java.awt.EventQueue;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.UniqueTag;

/**
 *
 * @author pk, mg refactoring
 */
public class ServerScriptProxy extends ScriptableObject {

    private final AppClient platypusClient;
    private final String moduleName;

    /**
     * Constructs server script runner for ordinary platypus session modules.
     *
     * @param aModuleName
     * @param aClient
     * @param aScope
     * @throws Exception
     */
    public ServerScriptProxy(String aModuleName, AppClient aClient, Scriptable aScope) throws Exception {
        super(aScope, null);
        platypusClient = aClient;
        moduleName = platypusClient.createServerModule(aModuleName);
        defineFunctionProperties(new String[]{
            "dispose"
        }, ServerScriptProxy.class, EMPTY);
    }

    /**
     * Disposes the session module on the server. Typically called from script
     * directly to free some resources or to ensure that module doesn't exist on
     * the server.
     *
     * @throws Exception
     */
    public void dispose() throws Exception {
        if (platypusClient != null) {
            platypusClient.disposeServerModule(moduleName);
        }
    }

    @Override
    public String getClassName() {
        return "ServerModule";
    }

    @Override
    public Object get(String name, Scriptable start) {
        final Object result = super.get(name, start);
        if (result == null || result instanceof Undefined || UniqueTag.NOT_FOUND.equals(result)) {
            return new StubFunction(name);
        } else {
            return result;
        }
    }

    public String getScriptName() {
        return moduleName;
    }

    private class StubFunction extends ScriptableObject implements Function {

        private String methodName;

        public StubFunction(String aMethodName) {
            super();
            methodName = aMethodName;
        }

        @Override
        public String getClassName() {
            return "ServerModuleFunction";
        }

        @Override
        public Object call(final Context cx, Scriptable scope, Scriptable thisObj, final Object[] arguments) {
            try {
                Function onSuccess = null;
                Function onFailure = null;
                int argsLength = arguments != null ? arguments.length : 0;
                if (arguments != null) {
                    if (arguments.length > 1 && arguments[arguments.length - 1] instanceof Function && arguments[arguments.length - 2] instanceof Function) {
                        onSuccess = (Function) arguments[arguments.length - 2];
                        onFailure = (Function) arguments[arguments.length - 1];
                        argsLength -= 2;
                    } else if (arguments.length > 0 && arguments[arguments.length - 1] instanceof Function) {
                        onSuccess = (Function) arguments[arguments.length - 1];
                        argsLength -= 1;
                    }
                }

                if (onSuccess != null) {
                    final Function successCallback = onSuccess;
                    final Function failureCallback = onFailure;
                    final Object[] args = Arrays.copyOf(arguments, argsLength);
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Object result = platypusClient.executeServerModuleMethod(moduleName, methodName, args);
                                Context cx = Context.getCurrentContext();
                                boolean wasContext = cx != null;
                                if (!wasContext) {
                                    cx = ScriptUtils.enterContext();
                                }
                                try {
                                    successCallback.call(cx, StubFunction.this, StubFunction.this, new Object[]{result});
                                } finally {
                                    if (!wasContext) {
                                        Context.exit();
                                    }
                                }
                            } catch (Exception ex) {
                                if (failureCallback != null) {
                                    Context cx = Context.getCurrentContext();
                                    boolean wasContext = cx != null;
                                    if (!wasContext) {
                                        cx = ScriptUtils.enterContext();
                                    }
                                    try {
                                        failureCallback.call(cx, StubFunction.this, StubFunction.this, new Object[]{ex.getMessage()});
                                    } finally {
                                        if (!wasContext) {
                                            Context.exit();
                                        }
                                    }
                                }
                                Logger.getLogger(ServerScriptProxy.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    });
                    return Context.getUndefinedValue();
                } else {
                    return platypusClient.executeServerModuleMethod(moduleName, methodName, arguments);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
            throw new UnsupportedOperationException("Calling server module function as a constructor is not supported.");
        }
    }
}
