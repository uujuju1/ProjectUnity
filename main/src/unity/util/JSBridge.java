package unity.util;

import arc.*;
import rhino.*;

import static mindustry.Vars.*;
import static unity.Unity.*;

/**
 * Utility class for transition between Java and JS scripts, as well as providing a custom top level scope for the sake of
 * cross-mod compatibility. Use the custom scope for programmatically compiling Rhino functions. Note that {@link #unityScope}
 * does not support the {@code require()} function.
 * @author GlennFolker
 */
public final class JSBridge{
    public static Context context;
    public static ImporterTopLevel defaultScope;
    public static ImporterTopLevel unityScope;

    private JSBridge(){}

    // Main thread only!
    public static void init(){
        if(tools) return;

        context = mods.getScripts().context;
        defaultScope = (ImporterTopLevel)mods.getScripts().scope;

        unityScope = new ImporterTopLevel(context);
        context.evaluateString(unityScope, Core.files.internal("scripts/global.js").readString(), "global.js", 1);
    }

    public static void importDefaults(ImporterTopLevel scope){
        if(tools) return;
        for(var pack : packages){
            importPackage(scope, pack);
        }
    }

    public static void importPackage(ImporterTopLevel scope, String packageName){
        if(tools) return;
        var p = new NativeJavaPackage(packageName, mods.mainLoader());
        p.setParentScope(scope);

        scope.importPackage(p);
    }

    public static void importPackage(ImporterTopLevel scope, Package pack){
        if(tools) return;
        importPackage(scope, pack.getName());
    }

    public static void importClass(ImporterTopLevel scope, String canonical){
        if(tools) return;
        importClass(scope, ReflectUtils.findClass(canonical));
    }

    public static void importClass(ImporterTopLevel scope, Class<?> type){
        if(tools) return;

        var nat = new NativeJavaClass(scope, type);
        nat.setParentScope(scope);

        scope.importClass(nat);
    }

    public static Function compileFunc(Scriptable scope, String sourceName, String source){
        if(tools) throw new IllegalStateException();
        return compileFunc(scope, sourceName, source, 1);
    }

    public static Function compileFunc(Scriptable scope, String sourceName, String source, int lineNum){
        if(tools) throw new IllegalStateException();
        return context.compileFunction(scope, source, sourceName, lineNum);
    }
}
