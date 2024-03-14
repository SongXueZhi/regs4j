package experiments.evo;

import java.util.Objects;

public class Methodx {
    String packageName;
    String classQualifiedName;
    String methodName;

    String className;

    String methodSignature;

    public Methodx(String packageName, String className, String methodName, String methodSignature) {
        this.packageName = packageName;
        this.className = className;
        this.methodName = methodName;
        this.methodSignature = methodSignature;
    }

    public Methodx(String classQualifiedName,String methodName) {
        this.classQualifiedName = classQualifiedName;
        this.methodName = methodName;
    }
    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }


    public String getClassQualifiedName() {
        return classQualifiedName;
    }

    public void setClassQualifiedName(String classQualifiedName) {
        this.classQualifiedName = classQualifiedName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Methodx)) return false;
        Methodx methodx = (Methodx) o;
        return Objects.equals(getClassQualifiedName(), methodx.getClassQualifiedName()) && Objects.equals(getMethodName(), methodx.getMethodName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClassQualifiedName(), getMethodName());
    }

    @Override
    public String toString() {
        return "Methodx{" +
                "classQualifiedName='" + classQualifiedName + '\'' +
                ", methodName='" + methodName + '\'' +
                '}';
    }
}
