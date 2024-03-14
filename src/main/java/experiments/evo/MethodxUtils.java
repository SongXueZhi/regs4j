package experiments.evo;

public class MethodxUtils {

    public  static Methodx get(String classQualifiedName,String methodName) {
        return new Methodx(classQualifiedName,methodName);
    }
}
