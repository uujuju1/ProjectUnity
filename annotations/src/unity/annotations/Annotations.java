package unity.annotations;

import arc.func.*;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Attribute.Array;
import com.sun.tools.javac.code.Attribute.Enum;
import com.sun.tools.javac.code.Attribute.Error;
import com.sun.tools.javac.code.Attribute.Visitor;
import com.sun.tools.javac.code.Attribute.*;
import com.sun.tools.javac.code.Scope.*;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.*;
import mindustry.world.*;
import sun.reflect.annotation.*;

import javax.lang.model.element.*;
import javax.lang.model.type.*;
import java.lang.Class;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.Map.Entry;

/** @author GlennFolker */
public class Annotations{
    //region definitions

    /** Indicates that this content belongs to a specific faction */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.SOURCE)
    public @interface FactionDef{
        /** @return The faction */
        String value();
    }

    /** Indicates that this content's entity type inherits interfaces */
    @Target({ElementType.FIELD, ElementType.TYPE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface EntityDef{
        /** @return The interfaces that will be inherited by the generated entity class */
        Class<?>[] value();

        /** @return Whether the class can serialize itself */
        boolean serialize() default true;

        /** @return Whether the class can write/read to/from save files */
        boolean genio() default true;

        /** @return Whether the class is poolable */
        boolean pooled() default false;
    }

    /** Indicates that this content's entity will be the one that is pointed, or if it's the type it will get mapped to the entity mapping */
    @Target({ElementType.FIELD, ElementType.TYPE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface EntityPoint{
        /** @return The entity type */
        Class<?> value() default Void.class;
    }

    /** Whether this class is the base class for faction enum. Only one type may use this */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    public @interface FactionBase{}

    /** Works somewhat like {@code Object.assign(...)} for Block and Building */
    @Target({ElementType.TYPE, ElementType.FIELD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Merge{
        /** @return The base class */
        Class<?> base() default Block.class;

        /** @return The merged classes */
        Class<?>[] value();
    }

    /** Notifies that this class is a component class; an interface will be generated out of this */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    public @interface MergeComponent{}

    /** The generated interface from {@link MergeComponent} */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    public @interface MergeInterface{}

    /** Indicates that this class is an entity component */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    public @interface EntityComponent{
        /** @return Whether the component should be interpreted into interfaces */
        boolean write() default true;

        /** @return Whether the component should generate a base class for itself */
        boolean base() default false;
    }

    /** All entity components will inherit from this */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    public @interface EntityBaseComponent{}

    /** Whether this interface wraps an entity component */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    public @interface EntityInterface{}

    /** Prevents this component from getting added into an entity group, specified by the group's element type */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    public @interface ExcludeGroups{
        /** @return The excluded group's element type */
        Class<?>[] value();
    }

    //end region
    //region utilities

    /** Indicates that a field will be interpolated when synced. */
    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SyncField{
        /** If true, the field will be linearly interpolated. If false, it will be interpolated as an angle. */
        boolean value();

        /** If true, the field is clamped to 0-1. */
        boolean clamped() default false;
    }

    /** Indicates that a field will not be read from the server when syncing the local player state. */
    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SyncLocal{}

    /** Indicates that the field annotated with this came from another component class */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.SOURCE)
    public @interface Import{}

    /** Whether the field returned by this getter is meant to be read-only */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.SOURCE)
    public @interface ReadOnly{}

    /** Whether this method replaces the actual method in the base class */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    public @interface Replace{}

    /** Whether this method is implemented in annotation-processing time */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    public @interface InternalImpl{}

    /** Used for method appender sorting */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    public @interface MethodPriority{
        /** @return The priority */
        int value();
    }

    /** Inserts this parameter-less method into another void method */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    public @interface Insert{
        /**
         * @return The target method described in {@link String} with the format {@code <methodName>(<paramType>...)}.
         * For example, when targetting {@code void call(String arg, int prior)}, the target descriptor must be
         * {@code call(java.lang.String, int)}
         */
        String value();

        /** @return The component-specific method implementation to target */
        Class<?> block() default Void.class;

        /** @return Whether the call to this method is after the default or not */
        boolean after() default true;
    }

    /** Wraps a component-specific method implementation with this boolean parameterless method */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    public @interface Wrap{
        /**
         * @return The target method described in {@link String} with the format {@code <methodName>(<paramType>...)}.
         * For example, when targetting {@code void call(String arg, int prior)}, the target descriptor must be
         * {@code call(java.lang.String, int)}
         */
        String value();

        /** @return The component-specific method implementation to target */
        Class<?> block() default Void.class;
    }

    /** Appends this {@code add()}/{@code remove()} method before the {@code if([!]added)} check */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    public @interface BypassGroupCheck{}

    /** Will not replace {@code return;} to {@code break [block];}, hence breaking the entire method statement */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    public @interface BreakAll{}

    /** Loads texture regions but does not assign them to their acquirers */
    @Retention(RetentionPolicy.SOURCE)
    public @interface LoadRegs{
        /** @return The regions' name */
        String[] value();

        /** @return Whether it should outline the region, as a separate texture */
        boolean outline() default false;

        /** @return The outline color, only valid if {@link #outline()} is true */
        String outlineColor() default "464649";

        /** @return The outline radius, only valid if {@link #outline()} is true */
        int outlineRadius() default 4;
    }

    //end region

    //anuke's implementation of annotation proxy maker, to replace the broken one from oracle
    //thanks, anuke
    //damn you, oracle
    @SuppressWarnings({"unchecked"})
    public static class AnnotationProxyMaker{
        private final Compound anno;
        private final Class<? extends Annotation> annoType;

        private AnnotationProxyMaker(Compound anno, Class<? extends Annotation> annoType){
            this.anno = anno;
            this.annoType = annoType;
        }

        public static <A extends Annotation> A generateAnnotation(Compound anno, Class<A> annoType){
            AnnotationProxyMaker var2 = new AnnotationProxyMaker(anno, annoType);
            return annoType.cast(var2.generateAnnotation());
        }

        private Annotation generateAnnotation(){
            return AnnotationParser.annotationForMap(annoType, getAllReflectedValues());
        }

        private Map<String, Object> getAllReflectedValues(){
            Map<String, Object> res = new LinkedHashMap<>();

            for(Entry<MethodSymbol, Attribute> entry : getAllValues().entrySet()){
                MethodSymbol meth = entry.getKey();
                Object value = generateValue(meth, entry.getValue());
                if(value != null){
                    res.put(meth.name.toString(), value);
                }
            }

            return res;
        }

        private Map<MethodSymbol, Attribute> getAllValues(){
            Map<MethodSymbol, Attribute> map = new LinkedHashMap<>();
            ClassSymbol cl = (ClassSymbol)anno.type.tsym;

            try{
                Class<?> entryClass = Class.forName("com.sun.tools.javac.code.Scope$Entry");
                Field siblingField = entryClass.getField("sibling");
                Field symField = entryClass.getField("sym");

                WriteableScope members = cl.members();
                Field field = members.getClass().getField("elems");
                Object elems = field.get(members);

                for(Object currEntry = elems; currEntry != null; currEntry = siblingField.get(currEntry)){
                    handleSymbol((Symbol)symField.get(currEntry), map);
                }
            }catch(Throwable e){
                try{
                    Class<?> lookupClass = Class.forName("com.sun.tools.javac.code.Scope$LookupKind");
                    Field nonRecField = lookupClass.getField("NON_RECURSIVE");
                    Object nonRec = nonRecField.get(null);

                    WriteableScope scope = cl.members();
                    Method getSyms = scope.getClass().getMethod("getSymbols", lookupClass);
                    Iterable<Symbol> it = (Iterable<Symbol>)getSyms.invoke(scope, nonRec);
                    for(Symbol symbol : it){
                        handleSymbol(symbol, map);
                    }
                }catch(Throwable death){
                    throw new RuntimeException(death);
                }
            }

            for(Pair<MethodSymbol, Attribute> var7 : this.anno.values){
                map.put(var7.fst, var7.snd);
            }

            return map;
        }

        private <T extends Symbol> void handleSymbol(Symbol sym, Map<T, Attribute> map){
            if(sym.getKind() == ElementKind.METHOD){
                MethodSymbol var4 = (MethodSymbol)sym;
                Attribute var5 = var4.getDefaultValue();
                if(var5 != null){
                    map.put((T)var4, var5);
                }
            }
        }

        private Object generateValue(MethodSymbol var1, Attribute var2){
            AnnotationProxyMaker.ValueVisitor var3 = new AnnotationProxyMaker.ValueVisitor(var1);
            return var3.getValue(var2);
        }

        private class ValueVisitor implements Visitor{
            private final MethodSymbol meth;
            private Class<?> returnClass;
            private Object value;

            ValueVisitor(MethodSymbol var2){
                this.meth = var2;
            }

            Object getValue(Attribute var1){
                Method var2;
                try{
                    var2 = AnnotationProxyMaker.this.annoType.getMethod(meth.name.toString());
                }catch(NoSuchMethodException var4){
                    return null;
                }

                this.returnClass = var2.getReturnType();
                var1.accept(this);
                if(!(this.value instanceof ExceptionProxy) && !AnnotationType.invocationHandlerReturnType(this.returnClass).isInstance(this.value)){
                    this.typeMismatch(var2, var1);
                }

                return this.value;
            }

            @Override
            public void visitConstant(Constant var1){
                this.value = var1.getValue();
            }

            @Override
            public void visitClass(com.sun.tools.javac.code.Attribute.Class var1){
                this.value = mirrorProxy(var1.classType);
            }

            @Override
            public void visitArray(Array var1){
                Name var2 = ((ArrayType)var1.type).elemtype.tsym.getQualifiedName();
                int var6;
                if(var2.equals(var2.table.names.java_lang_Class)){
                    ListBuffer var14 = new ListBuffer();
                    Attribute[] var15 = var1.values;
                    int var16 = var15.length;

                    for(var6 = 0; var6 < var16; ++var6){
                        Attribute var7 = var15[var6];
                        Type var8 = var7 instanceof UnresolvedClass ? ((UnresolvedClass)var7).classType : ((com.sun.tools.javac.code.Attribute.Class)var7).classType;
                        var14.append(var8);
                    }

                    this.value = mirrorProxy(var14.toList());
                }else{
                    int var3 = var1.values.length;
                    Class var4 = this.returnClass;
                    this.returnClass = this.returnClass.getComponentType();

                    try{
                        Object var5 = java.lang.reflect.Array.newInstance(this.returnClass, var3);

                        for(var6 = 0; var6 < var3; ++var6){
                            var1.values[var6].accept(this);
                            if(this.value == null || this.value instanceof ExceptionProxy){
                                return;
                            }

                            try{
                                java.lang.reflect.Array.set(var5, var6, this.value);
                            }catch(IllegalArgumentException var12){
                                this.value = null;
                                return;
                            }
                        }

                        this.value = var5;
                    }finally{
                        this.returnClass = var4;
                    }
                }
            }

            @Override
            public void visitEnum(Enum var1){
                if(this.returnClass.isEnum()){
                    String var2 = var1.value.toString();

                    try{
                        this.value = java.lang.Enum.valueOf((Class)this.returnClass, var2);
                    }catch(IllegalArgumentException var4){
                        this.value = proxify(() -> new EnumConstantNotPresentException((Class)this.returnClass, var2));
                    }
                }else{
                    this.value = null;
                }
            }

            @Override
            public void visitCompound(Compound var1){
                try{
                    Class var2 = this.returnClass.asSubclass(Annotation.class);
                    this.value = AnnotationProxyMaker.generateAnnotation(var1, var2);
                }catch(ClassCastException var3){
                    this.value = null;
                }

            }

            @Override
            public void visitError(Error var1){
                if(var1 instanceof UnresolvedClass){
                    this.value = mirrorProxy(((UnresolvedClass)var1).classType);
                }else{
                    this.value = null;
                }
            }

            private void typeMismatch(Method var1, final Attribute var2){
                this.value = proxify(() -> new AnnotationTypeMismatchException(var1, var2.type.toString()));
            }
        }

        private static Object mirrorProxy(Type t){
            return proxify(() -> new MirroredTypeException(t));
        }

        private static Object mirrorProxy(List<Type> t){
            return proxify(() -> new MirroredTypesException(t));
        }

        private static <T extends Throwable> Object proxify(Prov<T> prov){
            try{
                return new ExceptionProxy(){
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected RuntimeException generateException(){
                        return (RuntimeException)prov.get();
                    }
                };
            }catch(Throwable t){
                throw new RuntimeException(t);
            }
        }
    }
}
