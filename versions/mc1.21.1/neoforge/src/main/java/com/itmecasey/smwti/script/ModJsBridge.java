package com.itmecasey.smwti.script;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Logger;

/**
 * Gives ModJS client scripts a global {@code SMWTI} (the {@link ScriptApi}). ModJS is optional, so everything here goes through
 * reflection: ModJS offers {@code ExtraBindings.register(BindingProvider)} and the provider's {@code install(type, cx, scope)} adds
 * globals to every script scope. Without ModJS nothing is loaded and nothing fails.
 */
public final class ModJsBridge {
    private static final Logger LOG = Logger.getLogger("smwti");

    private ModJsBridge() {
    }

    /** Installs the binding when ModJS is on the classpath; returns whether it did. */
    public static boolean install() {
        try {
            Class<?> provider = Class.forName("com.itmecasey.modjs.script.BindingProvider");
            Class<?> extra = Class.forName("com.itmecasey.modjs.script.ExtraBindings");
            Class<?> context = Class.forName("org.mozilla.javascript.Context");
            Class<?> scriptable = Class.forName("org.mozilla.javascript.Scriptable");
            Class<?> scriptableObject = Class.forName("org.mozilla.javascript.ScriptableObject");
            Method javaToJs = context.getMethod("javaToJS", Object.class, scriptable);
            Method put = scriptableObject.getMethod("putProperty", scriptable, String.class, Object.class);
            ScriptApi api = new ScriptApi();
            InvocationHandler handler = (proxy, method, args) -> {
                if (method.getName().equals("install") && args != null && args.length == 3) {
                    // Scripts run again on a reload, so what the last run registered goes.
                    if ("CLIENT".equals(String.valueOf(args[0]))) {
                        ScriptRules.clear();
                        ScriptProviders.register();
                        put.invoke(null, args[2], "SMWTI", javaToJs.invoke(null, api, args[2]));
                    }
                    return null;
                }
                if (method.getName().equals("toString")) {
                    return "SMWTI binding";
                }
                if (method.getName().equals("hashCode")) {
                    return System.identityHashCode(proxy);
                }
                if (method.getName().equals("equals")) {
                    return proxy == args[0];
                }
                return null;
            };
            Object binding = Proxy.newProxyInstance(provider.getClassLoader(), new Class<?>[] {provider}, handler);
            extra.getMethod("register", provider).invoke(null, binding);
            LOG.info("SMWTI: ModJS client scripts can use the SMWTI global");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOG.warning("SMWTI: could not add the SMWTI global to ModJS: " + e);
            return false;
        }
    }
}
