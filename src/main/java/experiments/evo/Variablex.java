package experiments.evo;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Objects;

public class Variablex {
    String packageName;
    String className;

    String variableName;

    String qualifiedName;

    public Variablex(String packageName, String className, String variableName, String qualifiedName) {
        this.packageName = packageName;
        this.className = className;
        this.variableName = variableName;
        this.qualifiedName = qualifiedName;
    }

    public Variablex(String qualifiedName, String variableName) {
        this.qualifiedName = qualifiedName;
        this.variableName = variableName;
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

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public void setQualifiedName(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Variablex)) return false;
        Variablex variablex = (Variablex) o;
        return Objects.equals(variableName, variablex.variableName) && Objects.equals(qualifiedName, variablex.qualifiedName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableName, qualifiedName);
    }
}
