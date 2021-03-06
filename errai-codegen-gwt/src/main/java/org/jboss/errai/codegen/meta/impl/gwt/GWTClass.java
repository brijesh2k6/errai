/*
 * Copyright 2011 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.codegen.meta.impl.gwt;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jboss.errai.codegen.DefModifiers;
import org.jboss.errai.codegen.Parameter;
import org.jboss.errai.codegen.builder.impl.Scope;
import org.jboss.errai.codegen.meta.*;
import org.jboss.errai.codegen.meta.impl.AbstractMetaClass;
import org.jboss.errai.codegen.util.GWTPrivateMemberAccessor;
import org.jboss.errai.codegen.util.GenUtil;
import org.jboss.errai.codegen.util.PrivateAccessUtil;

import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JEnumType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JGenericType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JTypeParameter;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;

/**
 * @author Mike Brock <cbrock@redhat.com>
 * @author Christian Sadilek <csadilek@redhat.com>
 */
public class GWTClass extends AbstractMetaClass<JType> {
  protected final Annotation[] annotations;
  protected TypeOracle oracle;

  static {
    GenUtil.addClassAlias(GWTClass.class);
    PrivateAccessUtil.registerPrivateMemberAccessor("jsni", new GWTPrivateMemberAccessor());
  }

  protected GWTClass(final TypeOracle oracle, final JType classType, final boolean erased) {
    super(classType);
    this.oracle = oracle;

    final JClassType classOrInterface = classType.isClassOrInterface();
    if (classOrInterface != null) {
      annotations = AnnotationParser.parseAnnotations(classOrInterface.getAnnotations());
    }
    else {
      annotations = new Annotation[0];
    }

    if (classType.getQualifiedSourceName().contains(" ")
            || classType.getQualifiedSourceName().contains("?")) {
      throw new IllegalArgumentException("Cannot represent \"" + classType + "\" as a class. Try a different meta type such as GWTWildcardType or GWTTypeVaraible.");
    }

    final JParameterizedType parameterizedType = classType.isParameterized();
    if (!erased) {
      if (parameterizedType != null) {
        super.parameterizedType = new GWTParameterizedType(oracle, parameterizedType);
      }
    }
  }

  public static MetaClass newInstance(final TypeOracle oracle, final JType type) {
    return newUncachedInstance(oracle, type);
  }

  public static MetaClass newInstance(final TypeOracle oracle, final String type) {
    try {
      return newUncachedInstance(oracle, oracle.getType(type));
    }
    catch (NotFoundException e) {
      return null;
    }
  }

  public static MetaClass newUncachedInstance(final TypeOracle oracle, final JType type) {

    return new GWTClass(oracle, type, false);
  }

  public static MetaClass newUncachedInstance(final TypeOracle oracle, final JType type, final boolean erased) {
    return new GWTClass(oracle, type, erased);
  }

  public static MetaClass[] fromClassArray(final TypeOracle oracle, final JClassType[] classes) {
    final MetaClass[] newClasses = new MetaClass[classes.length];
    for (int i = 0; i < classes.length; i++) {
      newClasses[i] = newInstance(oracle, classes[i]);
    }
    return newClasses;
  }

  public static Class<?>[] jParmToClass(final JParameter[] parms) throws ClassNotFoundException {
    final Class<?>[] classes = new Class<?>[parms.length];
    for (int i = 0; i < parms.length; i++) {
      classes[i] = getPrimitiveOrClass(parms[i]);
    }
    return classes;
  }

  public static Class<?> getPrimitiveOrClass(final JParameter parm) throws ClassNotFoundException {
    final JType type = parm.getType();
    final String name =
        type.isArray() != null ? type.getJNISignature().replace("/", ".") : type.getQualifiedSourceName();

    if (parm.getType().isPrimitive() != null) {
      final char sig = parm.getType().isPrimitive().getJNISignature().charAt(0);

      switch (sig) {
      case 'Z':
        return boolean.class;
      case 'B':
        return byte.class;
      case 'C':
        return char.class;
      case 'D':
        return double.class;
      case 'F':
        return float.class;
      case 'I':
        return int.class;
      case 'J':
        return long.class;
      case 'S':
        return short.class;
      case 'V':
        return void.class;
      default:
        return null;
      }
    }
    else {
      return Class.forName(name, false, Thread.currentThread().getContextClassLoader());
    }
  }

  @Override
  public String getName() {
    return getEnclosedMetaObject().getSimpleSourceName();
  }

  @Override
  public String getFullyQualifiedName() {
    if (isArray()) {
      if (getOuterComponentType().isPrimitive()) {
        return getInternalName();
      }
      else {
        return getInternalName().replaceAll("/", "\\.");
      }
    }
    else {
      return getEnclosedMetaObject().getQualifiedBinaryName();
    }
  }

  @Override
  public String getCanonicalName() {
    return getEnclosedMetaObject().getQualifiedSourceName();
  }

  @Override
  public String getInternalName() {
    return getEnclosedMetaObject().getJNISignature();
  }

  @Override
  public String getPackageName() {
    return getEnclosedMetaObject().isClassOrInterface().getPackage().getName();
  }

  private static MetaMethod[] fromMethodArray(final TypeOracle oracle, final JMethod[] methods) {
    final List<MetaMethod> methodList = new ArrayList<MetaMethod>();

    for (final JMethod m : methods) {
      methodList.add(new GWTMethod(oracle, m));
    }

    return methodList.toArray(new MetaMethod[methodList.size()]);
  }

  private List<MetaMethod> getSpecialTypeMethods() {
    final List<MetaMethod> meths = new ArrayList<MetaMethod>();
    final JEnumType type = getEnclosedMetaObject().isEnum();

    if (type != null) {
      meths.add(new GWTSpecialMethod(this, DefModifiers.none(), Scope.Public, String.class, "name"));
      meths.add(new GWTSpecialMethod(this, DefModifiers.none(), Scope.Public, Enum.class, "valueOf", Parameter.of(
          String.class, "p").getMetaParameter()));
      meths.add(new GWTSpecialMethod(this, DefModifiers.none(), Scope.Public, Enum[].class, "values"));
    }

    return meths;
  }

  // TODO report this to be fixed in GWT: getClass() in java.lang.Object is reported as non-final method.
  private static final List<MetaMethod> overrideMethods =
      Arrays.asList(MetaClassFactory.get(Object.class).getMethods());

  @Override
  public MetaMethod[] getMethods() {
    final Set<MetaMethod> meths = new LinkedHashSet<MetaMethod>();
    meths.addAll(getSpecialTypeMethods());

    JClassType type = getEnclosedMetaObject().isClassOrInterface();
    if (type == null) {
      return null;
    }

    final Set<String> processedMethods = new HashSet<String>();
    do {
      for (final JMethod jMethod : type.getMethods()) {
        GWTMethod gwtMethod = new GWTMethod(oracle, jMethod);
        String readableMethodDecl = GenUtil.getMethodString(gwtMethod);
        if (!jMethod.isPrivate() && !processedMethods.contains(readableMethodDecl)) {
            meths.add(gwtMethod);
            processedMethods.add(readableMethodDecl);
        }
      }

      for (final JClassType interfaceType : type.getImplementedInterfaces()) {
        for (MetaMethod ifaceMethod : Arrays.asList(GWTClass.newInstance(oracle, interfaceType).getMethods())) {
          String readableMethodDecl = GenUtil.getMethodString(ifaceMethod);
          if (!processedMethods.contains(readableMethodDecl)) {
            meths.add(ifaceMethod);
            processedMethods.add(readableMethodDecl);
          }
        }
      }
    }
    while ((type = type.getSuperclass()) != null && !type.getQualifiedSourceName().equals("java.lang.Object"));
    meths.addAll(overrideMethods);

    return meths.toArray(new MetaMethod[meths.size()]);
  }

  @Override
  public MetaMethod[] getDeclaredMethods() {
    final JClassType type = getEnclosedMetaObject().isClassOrInterface();
    if (type == null) {
      return null;
    }

    return fromMethodArray(oracle, type.getMethods());
  }

  private static MetaField[] fromFieldArray(final TypeOracle oracle, final JField[] methods) {
    final List<MetaField> methodList = new ArrayList<MetaField>();

    for (final JField f : methods) {
      methodList.add(new GWTField(oracle, f));
    }

    return methodList.toArray(new MetaField[methodList.size()]);
  }

  @Override
  public MetaClass getErased() {
    if (getParameterizedType() == null) {
      return this;
    }
    else {
      return new GWTClass(oracle, getEnclosedMetaObject().getErasedType(), true);
    }
  }

  @Override
  public MetaField[] getFields() {
    final JClassType type = getEnclosedMetaObject().isClassOrInterface();
    if (type == null) {
      return null;
    }
    return fromFieldArray(oracle, type.getFields());
  }

  @Override
  public MetaField[] getDeclaredFields() {
    return getFields();
  }

  @Override
  public MetaField getField(final String name) {
    JClassType type = getEnclosedMetaObject().isClassOrInterface();
    if (type == null) {
      if ("length".equals(name) && getEnclosedMetaObject().isArray() != null) {
        return new MetaField.ArrayLengthMetaField(this);
      }
      return null;
    }

    JField field = type.findField(name);
    while ((field == null || (field != null && !field.isPublic())) &&
        (type = type.getSuperclass()) != null && !type.getQualifiedSourceName().equals("java.lang.Object")) {
      field = type.findField(name);

      for (final JClassType interfaceType : type.getImplementedInterfaces()) {
        field = interfaceType.findField(name);
      }
    }

    if (field == null) {
      throw new RuntimeException("no such field: " + name + " in class: " + this);
    }

    return new GWTField(oracle, field);
  }

  @Override
  public MetaField getDeclaredField(final String name) {
    JClassType type = getEnclosedMetaObject().isClassOrInterface();
    if (type == null) {
      if ("length".equals(name) && getEnclosedMetaObject().isArray() != null) {
        return new MetaField.ArrayLengthMetaField(this);
      }
      return null;
    }

    JField field = type.findField(name);

    if (field == null) {
      return null;
    }

    return new GWTField(oracle, field);
  }

  private static MetaConstructor[] fromMethodArray(final TypeOracle oracle, final JConstructor[] constructors) {
    final List<MetaConstructor> constructorList = new ArrayList<MetaConstructor>();

    for (final JConstructor c : constructors) {
      constructorList.add(new GWTConstructor(oracle, c));
    }

    return constructorList.toArray(new MetaConstructor[constructorList.size()]);
  }

  @Override
  public MetaConstructor[] getConstructors() {
    final JClassType type = getEnclosedMetaObject().isClassOrInterface();
    if (type == null) {
      return null;
    }

    return fromMethodArray(oracle, type.getConstructors());
  }

  @Override
  public MetaConstructor[] getDeclaredConstructors() {
    return getConstructors();
  }

  @Override
  public MetaClass[] getDeclaredClasses() {
    final JClassType[] nestedTypes = getEnclosedMetaObject().isClassOrInterface().getNestedTypes();
    final MetaClass[] declaredClasses = new MetaClass[nestedTypes.length];
    int i = 0;
    for (JClassType type : nestedTypes) {
      declaredClasses[i++] = GWTClass.newInstance(oracle, type);
    }
    return declaredClasses;
  }

  @Override
  public MetaClass[] getInterfaces() {
    final JClassType jClassType = getEnclosedMetaObject().isClassOrInterface();
    if (jClassType == null)
      return new MetaClass[0];

    final List<MetaClass> metaClassList = new ArrayList<MetaClass>();
    for (final JClassType type : jClassType.getImplementedInterfaces()) {

      metaClassList.add(new GWTClass(oracle, type, false));
    }

    return metaClassList.toArray(new MetaClass[metaClassList.size()]);
  }

  @Override
  public boolean isArray() {
    return getEnclosedMetaObject().isArray() != null;
  }

  @Override
  public MetaClass getSuperClass() {
    JClassType type = getEnclosedMetaObject().isClassOrInterface();
    if (type == null) {
      return null;
    }

    type = type.getSuperclass();

    if (type == null) {
      return null;
    }

    return newUncachedInstance(oracle, type);
  }

  @Override
  public MetaClass getComponentType() {
    final JArrayType type = getEnclosedMetaObject().isArray();
    if (type == null) {
      return null;
    }
    return newUncachedInstance(oracle, type.getComponentType());
  }

  @Override
  public Annotation[] getAnnotations() {
    return annotations;
  }

  @Override
  public MetaTypeVariable[] getTypeParameters() {
    final List<MetaTypeVariable> typeVariables = new ArrayList<MetaTypeVariable>();
    final JGenericType genericType = getEnclosedMetaObject().isGenericType();

    if (genericType != null) {
      for (final JTypeParameter typeParameter : genericType.getTypeParameters()) {
        typeVariables.add(new GWTTypeVariable(oracle, typeParameter));
      }
    }

    return typeVariables.toArray(new MetaTypeVariable[typeVariables.size()]);
  }

  @Override
  public boolean isVoid() {
    return getEnclosedMetaObject().getSimpleSourceName().equals("void");
  }

  @Override
  public boolean isPrimitive() {
    return getEnclosedMetaObject().isPrimitive() != null;
  }

  @Override
  public boolean isInterface() {
    return getEnclosedMetaObject().isInterface() != null;
  }

  @Override
  public boolean isAbstract() {
    return getEnclosedMetaObject().isClass() != null && getEnclosedMetaObject().isClass().isAbstract();
  }

  @Override
  public boolean isEnum() {
    return getEnclosedMetaObject().isEnum() != null;
  }

  @Override
  public boolean isAnnotation() {
    return getEnclosedMetaObject().isAnnotation() != null;
  }

  @Override
  public boolean isPublic() {
    return getEnclosedMetaObject().isClassOrInterface() != null &&
            getEnclosedMetaObject().isClassOrInterface().isPublic();
  }

  @Override
  public boolean isPrivate() {
    return getEnclosedMetaObject().isClassOrInterface() != null &&
            getEnclosedMetaObject().isClassOrInterface().isPrivate();
  }

  @Override
  public boolean isProtected() {
    return getEnclosedMetaObject().isClassOrInterface() != null &&
            getEnclosedMetaObject().isClassOrInterface().isProtected();
  }

  @Override
  public boolean isFinal() {
    return getEnclosedMetaObject().isClassOrInterface() != null &&
            getEnclosedMetaObject().isClassOrInterface().isFinal();
  }

  @Override
  public boolean isStatic() {
    return getEnclosedMetaObject().isClassOrInterface() != null &&
            getEnclosedMetaObject().isClassOrInterface().isStatic();
  }

  @Override
  public boolean isSynthetic() {
    return false;
  }

  @Override
  public boolean isAnonymousClass() {
    return false;
  }

  @Override
  public MetaClass asArrayOf(final int dimensions) {
    JType type = getEnclosedMetaObject();
    for (int i = 0; i < dimensions; i++) {
      type = oracle.getArrayType(type);
    }

    return new GWTClass(oracle, type, false);
  }
}
